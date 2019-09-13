package be.fenego.microservice.vertx.eureka;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.EurekaClient;

@Singleton
public class EurekaRegistration {
	private static final Logger logger = LoggerFactory.getLogger(EurekaRegistration.class);

	private final ApplicationInfoManager applicationInfoManager;
	private final EurekaClient eurekaClient;
	private final DynamicPropertyFactory configInstance;

	@Inject
	public EurekaRegistration(ApplicationInfoManager applicationInfoManager, EurekaClient eurekaClient,
			DynamicPropertyFactory configInstance) {
		this.applicationInfoManager = applicationInfoManager;
		this.eurekaClient = eurekaClient;
		this.configInstance = configInstance;
	}

	@PostConstruct
	public void start() {
		logger.info("Registering service to eureka with STARTING status");
		applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);

		logger.info("Changing eureka service to UP");
		applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);

		waitForRegistrationWithEureka(eurekaClient);

		logger.info("Eureka service started and ready to process requests..");
	}

	@PreDestroy
	public void stop() {
		if (eurekaClient != null) {
			logger.info("Shutting down eureka server.");
			eurekaClient.shutdown();
		}
	}

	private void waitForRegistrationWithEureka(EurekaClient eurekaClient) {
		String vipAddress = configInstance.getStringProperty("eureka.vipAddress", "sampleservice.mydomain.net").get();

		if (vipAddress == "sampleservice.mydomain.net") {
			logger.warn("Using default vip address");
		}

		InstanceInfo nextServerInfo = null;
		while (nextServerInfo == null) {
			try {
				nextServerInfo = eurekaClient.getNextServerFromEureka(vipAddress, false);
			} catch (Throwable e) {
				logger.info("Waiting ... verifying service registration with eureka ...");

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					// ignore
				}
			}
		}
	}
}
