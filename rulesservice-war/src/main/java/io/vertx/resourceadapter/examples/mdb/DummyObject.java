package io.vertx.resourceadapter.examples.mdb;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.smallrye.reactive.messaging.kafka.KafkaConnector;

@Singleton
@Startup
@DependsOn("StartupService")
public class DummyObject extends KafkaConnector {

	@PostConstruct
	public void pr(){
		System.out.println("heeeeeeeeeeeeeeeeeeeelllooo");

	}

	public void print(){

		System.out.println("heeeeeeeeeeeeeeeeeeeelllooo");
	}
	//@Produces
	//@ApplicationScoped
	//@Named("default-kafka-broker")
	//public Map<String, Object> createKafkaRuntimeConfig() {
		//Map<String, Object> properties = new HashMap<>();

		//StreamSupport
			//.stream(config.getPropertyNames().spliterator(), false)
			//.map(String::toLowerCase)
			//.filter(name -> name.startsWith("kafka"))
			//.distinct()
			//.sorted()
			//.forEach(name -> {
				//final String key = name.substring("kafka".length() + 1).toLowerCase().replaceAll("[^a-z0-9.]", ".");
				//final String value = config.getOptionalValue(name, String.class).orElse("");
				//properties.put(key, value);
			//});

		//return properties;
	//}
}
