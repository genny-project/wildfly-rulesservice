/**
 * 
 */
package life.genny.qwanda.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.TimedRuleExecutionFilter;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.conf.TimerJobFactoryOption;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import com.google.common.io.Files;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.eventbus.EventBusInterface;
import life.genny.qwanda.entity.User;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.KeycloakUtils;



import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;

import life.genny.eventbus.EventBusInterface;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import io.vertx.resourceadapter.examples.mdb.WildflyCache;
import javax.inject.Inject;
import life.genny.qwanda.message.QEventMessage;

import life.genny.rules.RulesLoader;


/**
 * @author acrow
 *
 */

@ApplicationScoped

public class RulesService {
	
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	Hazel inDb;

	@Inject
	EventBusBean eventBus;
	

	WildflyCache cacheInterface;

	@PostConstruct
	public void init() {
		log.info("Initialising Rules .... from " + GennySettings.rulesDir);
		cacheInterface = new WildflyCache(inDb);
	//	eventBus = new EventBusBean();
		
		VertxUtils.init(eventBus,cacheInterface);
		// Load in Rules
		RulesLoader.loadRules(GennySettings.rulesDir);

 	    if (!"TRUE".equalsIgnoreCase(System.getenv("DISABLE_INIT_RULES_STARTUP"))) {
 	    	RulesLoader.triggerStartupRules(GennySettings.rulesDir, eventBus);
 	    } else {
 	    	log.warn("DISABLE_INIT_RULES_STARTUP IS TRUE -> No Init Rules triggered.");
 	    }
	}

	public void executeStateful(final String rulesGroup, final EventBusInterface bus,
			final List<Tuple2<String, Object>> globals, final List<Object> facts,
			final Map<String, String> keyValueMap) {
		
		RulesLoader.executeStateful(rulesGroup, bus, globals, facts, keyValueMap);
	}
	
	public Map<String, Object> getDecodedTokenMap(final String token) {
		return RulesLoader.getDecodedTokenMap(token);
	}
	
	public List<Tuple2<String, Object>> getStandardGlobals() {
		return RulesLoader.getStandardGlobals();
	}
	
	public void info()
	
	{
		log.info("Rules info");
	}
}
