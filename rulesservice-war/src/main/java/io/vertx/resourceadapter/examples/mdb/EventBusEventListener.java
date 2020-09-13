package io.vertx.resourceadapter.examples.mdb;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.message.*;
import life.genny.qwandautils.JsonUtils;
import life.genny.utils.VertxUtils;
import org.apache.logging.log4j.Logger;
import org.jboss.ejb3.annotation.ResourceAdapter;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;

import javax.ejb.Asynchronous;
import javax.transaction.Transactional;
/**
 * Message-Driven Bean implementation class for: EventBusEventListener
 */

@MessageDriven(name = "EventBusEventListener", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "events"), })
@ResourceAdapter(value="rulesservice-ear.ear#vertx-jca-adapter-3.5.4.rar")
public class EventBusEventListener implements VertxListener {
	

	
	@Inject
	RulesEngineBean rulesEngineBean;


 	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
 	private static QEventMessage lastMessage; // TODO this is ugly and exists to stop duplicate messages

  /**
   * Default constructor.
   */
  public EventBusEventListener() {
   // log.info("EventBusEventListener started.");
  }

  
  
//  @Override
//  public <T> void onMessage(Message<T> message) {
//	  doProcessing(message);
//  }
  
  @Override
//  @Transactional
//  @Asynchronous
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());

	  String logMessage = "********* THIS IS WILDFLY EVENT LISTENER!!!! ******************* ";	
	  
	QEventMessage eventMsg = null;
	if (payload.getString("event_type").equals("EVT_ATTRIBUTE_VALUE_CHANGE")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventAttributeValueChangeMessage.class);
		logMessage += " EVT_ATTRIBUTE_VALUE_CHANGE "+eventMsg.hashCode();
		if (eventMsg!=null) {
			if ((lastMessage != null) && (eventMsg.equals(lastMessage))) {
				return;
			} else {
				lastMessage = eventMsg;
			}
		}
	} else if (payload.getString("event_type").equals("BTN_CLICK")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventBtnClickMessage.class);
	} else if (payload.getString("event_type").equals("EVT_LINK_CHANGE")) {
		eventMsg = JsonUtils.fromJson(payload.toString(), QEventLinkChangeMessage.class);
		logMessage += " EVT_LINK_CHANGE ";
	} else if ((payload.getString("event_type").equals("AUTH_INIT"))){
		JsonObject frontendData = payload.getJsonObject("data");
		log.info("Incoming Frontend data is "+frontendData.toString());
		try {
			String payloadString = payload.toString();
			eventMsg = JsonUtils.fromJson(payloadString, QEventMessage.class);
			eventMsg.getData().setValue(frontendData.toString());
		} catch (NoClassDefFoundError e) {
			log.error("No class def found ["+payload.toString()+"]");
		}
	} else {
		try {
			String payloadString = payload.toString();
			eventMsg = JsonUtils.fromJson(payloadString, QEventMessage.class);
		} catch (NoClassDefFoundError e) {
			log.error("No class def found ["+payload.toString()+"]");
		}
	}
	
	
	log.info(logMessage);
	if (eventMsg == null) {
		log.error("Can't get eventMsg from payload:" + message.body().toString());
		return;
	}

	if ((eventMsg.getData().getCode()!=null)&&(eventMsg.getData().getCode().equals("QUE_SUBMIT"))) {
		String token =  payload.getString("token");
		GennyToken userToken = new GennyToken(token);
			Answer dataAnswer = new Answer(userToken.getUserCode(),
					userToken.getUserCode(), "PRI_SUBMIT", "QUE_SUBMIT");
			dataAnswer.setChangeEvent(false);
			QDataAnswerMessage dataMsg = new QDataAnswerMessage(dataAnswer);
			dataMsg.setToken(token);
//			SessionFacts sessionFactsData = new SessionFacts(facts.getServiceToken(),
//					userToken, dataMsg);
			log.info("QUE_SUBMIT event to 'data' for "
					+ userToken.getUserCode());
			
			rulesEngineBean.processMsg(eventMsg, payload.getString("token"));
			
			VertxUtils.writeMsg("data",JsonUtils.toJson(dataMsg));
			//kieSession.signalEvent("data", sessionFactsData, processId);
	} else {
		if (eventMsg.getData().getCode()!=null) {
			rulesEngineBean.processMsg(eventMsg, payload.getString("token"));
		} 
	}
  }

  


}