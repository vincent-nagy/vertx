package be.fenego.microservice.vertx;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import be.fenego.microservice.vertx.eureka.EurekaRegistration;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

public abstract class BaseVerticle extends AbstractVerticle {
	protected static ApplicationInfoManager applicationInfoManager;
	protected static EurekaClient eurekaClient;

	protected EurekaRegistration eurekaRegistration;
	protected Router router;
	protected HttpServer server;
	protected WebClient client;
	protected ConfigRetriever retriever;

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		initialize();

		server = vertx.createHttpServer();

		router = Router.router(vertx);

		server.requestHandler(router).listen(applicationInfoManager.getInfo().getPort());

		client = WebClient.create(vertx);

		retriever = ConfigRetriever.create(vertx);
	}

	private void initialize() {
		DynamicPropertyFactory configInstance = DynamicPropertyFactory.getInstance();
		ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(
				new MyDataCenterInstanceConfig());
		EurekaClient eurekaClient = initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());

		eurekaRegistration = new EurekaRegistration(applicationInfoManager, eurekaClient, configInstance);
		eurekaRegistration.start();
	}

	private static synchronized ApplicationInfoManager initializeApplicationInfoManager(
			EurekaInstanceConfig instanceConfig) {
		if (applicationInfoManager == null) {
			InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
			applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
		}

		return applicationInfoManager;
	}

	private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig clientConfig) {
		if (eurekaClient == null) {
			eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
		}

		return eurekaClient;
	}

	@Override
	public void stop() throws Exception {
		super.stop();

		eurekaRegistration.stop();
	}

	protected void serviceUnavailable(RoutingContext context) {
		context.response().setStatusCode(503).end();
	}
}
