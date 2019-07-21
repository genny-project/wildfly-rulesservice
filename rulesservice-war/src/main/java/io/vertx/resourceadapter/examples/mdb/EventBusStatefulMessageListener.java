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
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import io.vertx.rxjava.core.Vertx;

import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventBtnClickMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.service.RulesService;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;

import life.genny.eventbus.EventBusInterface;
import life.genny.rules.RulesLoader;
import life.genny.security.TokenIntrospection;
import life.genny.models.GennyToken;

/**
 * Message-Driven Bean implementation class for: EventBusMessageListener -
 * listen for Stateful JBPM Messages
 */

@MessageDriven(name = "EventBusStatefulMessageListener", messageListenerInterface = VertxListener.class, activationConfig = {
		@ActivationConfigProperty(propertyName = "address", propertyValue = "statefulmessages"), })

public class EventBusStatefulMessageListener implements VertxListener {


	@Inject
	RulesService rulesService;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	private static final List<String> roles;

	static {
		roles = TokenIntrospection.setRoles("user");
	}

	/**
   * Default constructor.
   */
  public EventBusStatefulMessageListener() {
    //log.info("EventBusStatefulMessageListener started.");
  }

	@Override
	public <T> void onMessage(Message<T> message) {

		log.info("********* THIS IS WILDFLY STATEFUL MESSGE LISTENER!!!! *******************");

		final JsonObject payload = new JsonObject(message.body().toString());

		String token = payload.getString("token"); // TODO, this should be grabbed from header

		if (token != null/* && TokenIntrospection.checkAuthForRoles(userToken,roles, token)*/) { // do not allow empty tokens

//			log.info("Roles from this token are allow and authenticated "
//					+ TokenIntrospection.checkAuthForRoles(roles, token));
			
			QEventMessage eventMsg = null;
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

			
			RulesLoader.processMsg(eventMsg,token);
		}
	}

}