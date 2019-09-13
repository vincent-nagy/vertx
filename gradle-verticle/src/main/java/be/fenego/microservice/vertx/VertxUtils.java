package be.fenego.microservice.vertx;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import io.vertx.core.json.JsonArray;

public interface VertxUtils {
	public static final Collector<String, JsonArray, JsonArray> jsonArrayCollector = new Collector<String, JsonArray, JsonArray>() {

		@Override
		public Supplier<JsonArray> supplier() {
			return JsonArray::new;
		}

		@Override
		public BiConsumer<JsonArray, String> accumulator() {
			return (a, s) -> a.add(s);
		}

		@Override
		public BinaryOperator<JsonArray> combiner() {
			return (a1, a2) -> a1.addAll(a2);
		}

		@Override
		public Function<JsonArray, JsonArray> finisher() {
			return a -> a;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return Collections.emptySet();
		}

	};

	public static String buildQueryWithVariableArgs(String query, int size) {
		StringBuilder variableArgs = new StringBuilder(size);
		variableArgs.append("(");
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				variableArgs.append(',');
			}
			variableArgs.append("?");
		}
		variableArgs.append(")");

		return query.replace("{}", variableArgs.toString());
	}
}
