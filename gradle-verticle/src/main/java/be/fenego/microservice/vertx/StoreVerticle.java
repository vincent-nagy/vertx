package be.fenego.microservice.vertx;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fenego.microservice.vertx.pojos.EnrichedStore;
import be.fenego.microservice.vertx.pojos.Store;
import be.fenego.microservice.vertx.pojos.StoreHours;
import be.fenego.microservice.vertx.pojos.StoreWrapper;
import be.fenego.microservice.vertx.services.JDBCStoreService;
import be.fenego.microservice.vertx.services.StoreService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.codec.BodyCodec;

public class StoreVerticle extends BaseVerticle {
	private static final Logger logger = LoggerFactory.getLogger(StoreVerticle.class);

	private final StoreService storeService;
	private final Map<Integer, JsonObject> standardOpeningHours;

	public StoreVerticle() {
		super();

		JsonObject config = new JsonObject();
		config.put("service.type", "jdbc");
		config.put("url", "jdbc:postgresql://localhost/store?characterEncoding=UTF-8&useSSL=false");
		config.put("driver_class", "org.postgresql.Driver");
		config.put("user", "vertx");
		config.put("password", "vertx");
		config.put("max_pool_size", 30);

		storeService = new JDBCStoreService(config);

		standardOpeningHours = new HashMap<>();

	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		super.start(startFuture);

		router.route("/store/").method(HttpMethod.GET).handler(this::handleStoreGet);

		getStandardOpeningHours().future().setHandler(ar -> {
			if (ar.failed()) {
				logger.error("Failed to fetch standard opening hours");
			} else {
				JsonArray result = ar.result();

				for (int i = 0; i < 7; i++) {
					JsonObject day = result.getJsonObject(i);

					standardOpeningHours.put(i, day);
				}
			}
		});

	}

	private void handleStoreGet(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();

		getStores().future().setHandler(ar -> {
			if (ar.failed()) {
				serviceUnavailable(routingContext);
			} else {
				Store[] stores = ar.result();
				Promise<Map<String, List<StoreHours>>> storeHours = storeService.getStoreHours(stores);

				storeHours.future().setHandler(r -> {
					if (r.succeeded()) {
						Map<String, List<StoreHours>> result = r.result();
						List<EnrichedStore> enrichedStores = new ArrayList<>();

						Arrays.stream(stores).forEach(store -> {
							EnrichedStore eStore = new EnrichedStore(store);
							eStore = fillInOpeningHours(eStore, result.get(eStore.getUuid()));
							enrichedStores.add(eStore);
						});

						response.setStatusCode(200).end(Json.encodePrettily(enrichedStores));
					} else {
						response.setStatusCode(500).end();
					}
				});
			}
		});
	}

	private Promise<Store[]> getStores() {
		Promise<Store[]> stores = Promise.promise();

		client.get("http://localhost/INTERSHOP/rest/WFS/inSPIRED-inTRONICS-Site/smb-responsive/stores")
				.as(BodyCodec.jsonObject()).send(ar -> {
					if (ar.succeeded()) {
						JsonObject body = ar.result().body();

						StoreWrapper wrapper = body.mapTo(StoreWrapper.class);
						stores.complete(wrapper.getElements());
					} else {
						stores.fail(ar.cause());
					}
				});
		return stores;
	}

	private EnrichedStore fillInOpeningHours(EnrichedStore enrichedStore, List<StoreHours> storeHours) {
		List<StoreHours> completeStoreHours = new ArrayList<>();
		LocalDate lowerLimit = LocalDate.now().with(DayOfWeek.MONDAY);

		for (int i = 0; i < 14; i++) {
			String standardOpeningTime = standardOpeningHours.get(i % 7).getString("openingTime");
			String standardClosingTime = standardOpeningHours.get(i % 7).getString("closingTime");

			StoreHours standard = new StoreHours(lowerLimit.plus(i, ChronoUnit.DAYS).toString(), standardOpeningTime,
					standardClosingTime);
			completeStoreHours.add(standard);
		}

		if (storeHours != null && !storeHours.isEmpty()) {
			List<String> divergentDates = storeHours.stream().map(x -> x.getDate()).collect(Collectors.toList());

			completeStoreHours.removeIf(x -> divergentDates.contains(x.getDate()));
			completeStoreHours.addAll(storeHours);
		}

		enrichedStore.setOpeningHours(completeStoreHours);

		return enrichedStore;
	}

	private Promise<JsonArray> getStandardOpeningHours() {
		Promise<JsonArray> openingHoursPromise = Promise.promise();
		retriever.getConfig(ar -> {
			if (ar.failed()) {
				openingHoursPromise.fail("Failed to fetch the configuration");
			} else {
				JsonObject config = ar.result();
				openingHoursPromise.complete(config.getJsonArray("standardOpeningHours"));
			}
		});
		return openingHoursPromise;
	}
}
