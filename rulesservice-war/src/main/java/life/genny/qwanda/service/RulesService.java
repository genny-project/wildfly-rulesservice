/**
 * 
 */
package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;


import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import io.vertx.resourceadapter.examples.mdb.WildflyCache;
import life.genny.qwandautils.GennySettings;
import life.genny.rules.RulesLoader;
import life.genny.utils.VertxUtils;

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

	public void init() {
		log.info("Initialising Rules .... from " + GennySettings.rulesDir);
		cacheInterface = new WildflyCache(inDb);
		VertxUtils.init(eventBus, cacheInterface);
		
		// Load in DEFs for every realm
		
		
		
		// Load in Rules
		RulesLoader.init();
		log.info("Loading Rules");
		Boolean noChangeInRules = RulesLoader.loadRules(GennySettings.rulesDir);
		Boolean noskip = true;
		JsonObject skipJson = VertxUtils.readCachedJson("JENNY", "SKIP");
		if (skipJson.containsKey("status")) {
			if ("ok".equalsIgnoreCase(skipJson.getString("status"))) {
				String val = skipJson.getJsonObject("value").toString();
				if ("TRUE".equalsIgnoreCase(val)) {
					noChangeInRules = true;
					noskip = false;
				}
			}
		}
		log.info("SKIP JENNY JSON = "+skipJson.toString());
		
		if (noskip &&((!noChangeInRules) || (!"TRUE".equalsIgnoreCase(System.getenv("DISABLE_INIT_RULES_STARTUP"))))) {
			log.info("Rulesservice triggering rules");
			(new RulesLoader()).triggerStartupRules(GennySettings.rulesDir);
		} else {
			log.warn("DISABLE_INIT_RULES_STARTUP IS TRUE -> No Init Rules triggered. SKIP CACHE = "+(noskip?"FALSE":"TRUE"));
		}
	}


	
	@PreDestroy
	public void shutdown() {
		RulesLoader.shutdown();
	}

	public void info()

	{
		log.info("Rules info");
	}
}
