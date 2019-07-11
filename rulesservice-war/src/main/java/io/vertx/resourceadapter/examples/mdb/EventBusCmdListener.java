package io.vertx.resourceadapter.examples.mdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import org.apache.logging.log4j.Logger;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import io.vertx.rxjava.core.Vertx;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GPS;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataGPSMessage;
import life.genny.qwanda.message.QDataPaymentsCallbackMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventBtnClickMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.rule.Rule;
import life.genny.qwanda.service.RulesService;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.models.GennyToken;
import life.genny.qwanda.message.QCmdMessage;

import life.genny.eventbus.EventBusInterface;
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
