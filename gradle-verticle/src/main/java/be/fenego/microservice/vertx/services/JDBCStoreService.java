package be.fenego.microservice.vertx.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fenego.microservice.vertx.VertxUtils;
import be.fenego.microservice.vertx.pojos.Store;
import be.fenego.microservice.vertx.pojos.StoreHours;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

public class JDBCStoreService implements StoreService {
	private static final String QUERY = "SELECT storeuuid, date, openingtime, closingtime FROM openingtimes WHERE storeuuid IN {} AND date BETWEEN ? AND ?";
	private static final Logger logger = LoggerFactory.getLogger(JDBCStoreService.class);

	private final JDBCClient client;

	public JDBCStoreService(JsonObject config) {
		this.client = JDBCClient.createShared(Vertx.vertx(), config);
	}

	@Override
	public Promise<Map<String, List<StoreHours>>> getStoreHours(Store... store) {
		Promise<Map<String, List<StoreHours>>> result = Promise.promise();

		client.getConnection(conn -> {
			if (conn.succeeded()) {
				final SQLConnection connection = conn.result();

				LocalDate lowerLimit = LocalDate.now().with(DayOfWeek.MONDAY);
				LocalDate upperLimit = LocalDate.now().plusWeeks(1).with(DayOfWeek.SUNDAY);

				JsonArray storeUuids = Arrays.stream(store).map(s -> s.getUuid())
						.collect(VertxUtils.jsonArrayCollector);
				JsonArray params = new JsonArray().addAll(storeUuids).add(lowerLimit.toString())
						.add(upperLimit.toString());

				connection.queryWithParams(VertxUtils.buildQueryWithVariableArgs(QUERY, store.length), params, qr -> {
					if (qr.failed()) {
						logger.error("Query failed to execute: {}", qr.cause());
						result.fail(qr.cause());
					} else {
						List<JsonObject> rows = qr.result().getRows();

						if (rows == null || rows.isEmpty()) {
							result.complete();
						} else {
							Map<String, List<StoreHours>> allOpeningHours = new HashMap<>();

							rows.forEach(row -> {
								String uuid = row.getString("storeuuid");

								StoreHours storeHours = new StoreHours(row.getString("date"),
										row.getString("openingtime").substring(0, 5),
										row.getString("closingtime").substring(0, 5));

								if (!allOpeningHours.containsKey(uuid)) {
									allOpeningHours.put(uuid, new ArrayList<>());
								}
								allOpeningHours.get(uuid).add(storeHours);
							});
							result.complete(allOpeningHours);
						}
						connection.close();
					}
				});
			} else {
				System.out.println("Failed to make a connection");
			}
		});
		return result;
	}
}
