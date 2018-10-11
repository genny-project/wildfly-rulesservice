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


import io.vavr.Tuple2;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import io.vertx.rxjava.core.Vertx;

import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventBtnClickMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.rules.QRules;
import life.genny.rules.RulesLoader;
import life.genny.eventbus.EventBusInterface;


/**
 * Message-Driven Bean implementation class for: VertxMonitor
 */
//@MessageDriven(name = "VertxMonitor", messageListenerInterface = VertxListener.class)
@MessageDriven(name = "VertxMonitor", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "events"), })

public class VertxMonitor implements VertxListener {
	

@Inject
EventBusBean eventBus;

  final static String st = System.getenv("MYIP");
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	static Map<String, Object> decodedToken = null;
	static Set<String> userRoles = null;
	private static Map<String, User> usersSession = new HashMap<String, User>();

	


	static String token;


  /**
   * Default constructor.
   */
  public VertxMonitor() {
    log.info("VertxMonitor started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());
//    logger.info("Get a aaaaaaaaaaaaaaaaaaaamessage from Vert.x: " + payload);

	QEventMessage eventMsg = null;
	String evtMsg = "Event:";
	if (payload.getString("event_type").equals("EVT_ATTRIBUTE_VALUE_CHANGE")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventAttributeValueChangeMessage.class);
	} else if (payload.getString("event_type").equals("BTN_CLICK")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventBtnClickMessage.class);
	} else if (payload.getString("event_type").equals("EVT_LINK_CHANGE")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventLinkChangeMessage.class);
	} else {
		try {
			eventMsg = JsonUtils.fromJson(payload.toString(), QEventMessage.class);
		} catch (NoClassDefFoundError e) {
			log.error("No class def found ["+payload.toString()+"]");
		}
	}
	processMsg("Event:"+payload.getString("event_type"), payload.getString("ruleGroup"),eventMsg, eventBus, payload.getString("token"));
  }

  
	public void processMsg(final String msgType,String ruleGroup,final Object msg, final EventBusInterface eventBus, final String token) {
			
			Map<String,Object> adecodedTokenMap = RulesLoader.getDecodedTokenMap(token);
			// check for token expiry
			
			
			Set<String> auserRoles = KeycloakUtils.getRoleSet(adecodedTokenMap.get("realm_access").toString());
			User userInSession = usersSession.get(adecodedTokenMap.get("preferred_username").toString());

			String preferredUName = adecodedTokenMap.get("preferred_username").toString();
			String fullName = adecodedTokenMap.get("name").toString();
			String realm = adecodedTokenMap.get("realm").toString();
			if ("genny".equalsIgnoreCase(realm)) {
				realm = GennySettings.mainrealm;
				adecodedTokenMap.put("realm", GennySettings.mainrealm);
			}
			String accessRoles = adecodedTokenMap.get("realm_access").toString();

			QRules qRules = new QRules(eventBus, token, adecodedTokenMap);
			qRules.set("realm", realm);


			List<Tuple2<String, Object>> globals = new ArrayList<Tuple2<String, Object>>();
			RulesLoader.getStandardGlobals();

			List<Object> facts = new ArrayList<Object>();
			facts.add(qRules);
			facts.add(msg);
			facts.add(adecodedTokenMap);
			facts.add(auserRoles);
			if(userInSession!=null)
				facts.add(usersSession.get(preferredUName));
			else {
	            User currentUser = new User(preferredUName, fullName, realm, accessRoles);
				usersSession.put(adecodedTokenMap.get("preferred_username").toString(), currentUser);
				facts.add(currentUser);
			}


			Map<String, String> keyvalue = new HashMap<String, String>();
			keyvalue.put("token", token);

			if (!"GPS".equals(msgType)) { System.out.println("FIRE RULES ("+realm+") "+msgType); }

		//	String ruleGroupRealm = realm + (StringUtils.isBlank(ruleGroup)?"":(":"+ruleGroup));
			try {
				RulesLoader.executeStatefull(realm, eventBus, globals, facts, keyvalue);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	

	}

  
}
