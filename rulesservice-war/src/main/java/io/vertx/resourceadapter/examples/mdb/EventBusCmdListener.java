package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.service.RulesService;
import life.genny.rules.RulesLoader;


/**
 * Message-Driven Bean implementation class for: EventBusDataListener
 */

@MessageDriven(name = "EventBusCmdListener", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "cmds"), })

public class EventBusCmdListener implements VertxListener {
	

@Inject
EventBusBean eventBus;

@Inject
RulesService rulesService;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	static Map<String, Object> decodedToken = null;
	static Set<String> userRoles = null;
	private static Map<String, User> usersSession = new HashMap<String, User>();

	


	static String token;


  /**
   * Default constructor.
   */
  public EventBusCmdListener() {
    //log.info("EventBusDataListener started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());
    log.info("Get a data message from Vert.x: " + payload);
	log.info("********* THIS IS WILDFLY CMD LISTENER!!!! *******************");

	QCmdMessage cmdMsg = null;
	
	String token = payload.getString("token");
	
	GennyToken gennyToken = new GennyToken(token);
	
		
	if (gennyToken.hasRole("dev")) {

		if (payload.getString("msg_type").equals("CMD_MSG")) {
			if (payload.getString("cmd_type").equals("CMD_RELOAD_RULES")) {
				if (payload.getString("code").equals("RELOAD_RULES_FROM_FILES")) {
					RulesLoader.loadRules(gennyToken.getRealm(),"/rules");
				}
			}
		}
	}
  }

  


}
