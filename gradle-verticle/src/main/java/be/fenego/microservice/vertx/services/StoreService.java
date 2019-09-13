package be.fenego.microservice.vertx.services;

import java.util.List;
import java.util.Map;

import be.fenego.microservice.vertx.pojos.Store;
import be.fenego.microservice.vertx.pojos.StoreHours;
import io.vertx.core.Promise;

public interface StoreService {
	Promise<Map<String, List<StoreHours>>> getStoreHours(Store... store);
}
