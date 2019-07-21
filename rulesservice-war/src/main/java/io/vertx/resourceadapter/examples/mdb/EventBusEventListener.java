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
import life.genny.models.GennyToken;


/**
 * Message-Driven Bean implementation class for: EventBusEventListener
 */

@MessageDriven(name = "EventBusEventListener", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "events"), })

public class EventBusEventListener implements VertxListener {
	

@Inject
EventBusBean eventBus;

@Inject
RulesService rulesService;

 	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  


  /**
   * Default constructor.
   */
  public EventBusEventListener() {
   // log.info("EventBusEventListener started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());

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
	
	
	log.info("********* THIS IS WILDFLY EVENT LISTENER!!!! *******************");
	
	RulesLoader.processMsg(eventMsg, payload.getString("token"));
  }

  


}