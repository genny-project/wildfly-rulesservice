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


import life.genny.eventbus.EventBusInterface;
import life.genny.rules.RulesLoader;


/**
 * Message-Driven Bean implementation class for: EventBusDataListener
 */

@MessageDriven(name = "EventBusDataListener", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "data"), })

public class EventBusDataListener implements VertxListener {
	

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
  public EventBusDataListener() {
    //log.info("EventBusDataListener started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());
    log.info("Get a data message from Vert.x: " + payload);
	log.info("********* THIS IS WILDFLY DATA LISTENER!!!! *******************");

	QDataAnswerMessage dataMsg = null;

	// Is it a Rule?
	if (payload.getString("data_type").equals(Rule.class.getSimpleName())) {
		JsonArray ja = payload.getJsonArray("items");
		String ruleGroup = ja.getJsonObject(0).getString("ruleGroup");
		String ruleText = ja.getJsonObject(0).getString("rule");
		String ruleCode = ja.getJsonObject(0).getString("code");
		// QDataRuleMessage ruleMsg = gson3.fromJson(json, QDataRuleMessage.class);
		log.info("Incoming Rule :" + ruleText);

		String rulesGroup = GennySettings.rulesDir;
		List<Tuple3<String,String, String>> rules = new ArrayList<Tuple3<String,String, String>>();
		rules.add(Tuple.of(ruleGroup,ruleCode, ruleText));

		RulesLoader.addRules(rulesGroup, rules);
	} else if (payload.getString("data_type").equals(Answer.class.getSimpleName())) {
		log.info("DATA Msg :");;
		try {
			dataMsg = JsonUtils.fromJson(payload.toString(), QDataAnswerMessage.class);
			RulesLoader.processMsg(dataMsg,  payload.getString("token"));
		} catch (com.google.gson.JsonSyntaxException e) {
			log.error("BAD Syntax converting to json from " + dataMsg);
			JsonObject json = new JsonObject(payload.toString());
			JsonObject answerData = json.getJsonObject("items");
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(answerData);
			json.put("items", jsonArray);
			dataMsg = JsonUtils.fromJson(json.toString(), QDataAnswerMessage.class);
			RulesLoader.processMsg( dataMsg,  payload.getString("token"));
		}
	}
	else if (payload.getString("data_type").equals(GPS.class.getSimpleName())) {

		QDataGPSMessage dataGPSMsg = null;
		try {
			dataGPSMsg = JsonUtils.fromJson(payload.toString(), QDataGPSMessage.class);
			RulesLoader.processMsg(dataGPSMsg, payload.getString("token"));
		}
		catch (com.google.gson.JsonSyntaxException e) {

			log.error("BAD Syntax converting to json from " + dataGPSMsg);
			JsonObject json = new JsonObject(payload.toString());
			JsonObject answerData = json.getJsonObject("items");
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(answerData);
			json.put("items", jsonArray);
			dataGPSMsg = JsonUtils.fromJson(json.toString(), QDataGPSMessage.class);
			RulesLoader.processMsg( dataGPSMsg,  payload.getString("token"));
		}
	} else if(payload.getString("data_type").equals(QDataPaymentsCallbackMessage.class.getSimpleName())) {
		QDataPaymentsCallbackMessage dataCallbackMsg = null;
		try {
			dataCallbackMsg = JsonUtils.fromJson(payload.toString(), QDataPaymentsCallbackMessage.class);
			RulesLoader.processMsg(dataCallbackMsg,  payload.getString("token"));
		}
		catch (com.google.gson.JsonSyntaxException e) {

			log.error("BAD Syntax converting to json from " + dataCallbackMsg);
			JsonObject json = new JsonObject(payload.toString());
			dataCallbackMsg = JsonUtils.fromJson(json.toString(), QDataPaymentsCallbackMessage.class);
			RulesLoader.processMsg( dataCallbackMsg, payload.getString("token"));
		}
	}
  }

  


}
