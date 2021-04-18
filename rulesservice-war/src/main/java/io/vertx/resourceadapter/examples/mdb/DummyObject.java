package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.logging.log4j.Logger;

@Singleton
@Startup
@DependsOn("StartupService")
public class DummyObject {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@PostConstruct
	public void dummy(){
		log.info("Dummy Object initialised");
	}

}
