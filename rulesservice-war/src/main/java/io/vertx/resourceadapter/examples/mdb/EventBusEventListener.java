package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;
//import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
//import io.vertx.resourceadapter.inflow.VertxListener;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventBtnClickMessage;
import life.genny.qwanda.message.QEventDropdownMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.utils.VertxUtils;
/**
 * Message-Driven Bean implementation class for: EventBusEventListener
 */

@Startup
@DependsOn("StartupService")
@Singleton
public class EventBusEventListener {
	

	@Inject DummyObject dummy;
	
	@Inject
	RulesEngineBean rulesEngineBean;


 	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
 	private static QEventMessage lastMessage; // TODO this is ugly and exists to stop duplicate messages

  /**
   * Default constructor.
   */
  public EventBusEventListener() {
				
  }

	@PostConstruct
	public void dummy(){
	}
  
  
	@Incoming("events")
	@Merge
  public CompletionStage<Void> onMessage(Message<String> message) {
		//final JsonObject payload = new JsonObject(message.body().toString());
		final JsonObject payload = new JsonObject(message.getPayload());
		System.out.println("MSG PAYLOAD = " + payload.toString());

		long startTime = System.nanoTime();
        log.info("********* THIS IS WILDFLY DATA LISTENER!!!! *********" + startTime);
        String logMessage = "";
        
		QEventMessage eventMsg = null;
		if (payload.getString("event_type").equals("EVT_ATTRIBUTE_VALUE_CHANGE")) {
			eventMsg = JsonUtils.fromJson(payload.toString(), QEventAttributeValueChangeMessage.class);
			logMessage += " EVT_ATTRIBUTE_VALUE_CHANGE "+eventMsg.hashCode();
			if (eventMsg!=null) {
				if ((lastMessage != null) && (eventMsg.equals(lastMessage))) {
					return message.ack();
				} else {
					lastMessage = eventMsg;
				}
			}
		} else if (payload.getString("event_type").equals("DD")) {
			eventMsg = JsonUtils.fromJson(payload.toString(), QEventDropdownMessage.class);
			System.out.println("DD MSG = " + JsonUtils.toJson(eventMsg));
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
			//log.error("Can't get eventMsg from payload:" + message.body().toString());
			log.error("Can't get eventMsg from payload:" + message.getPayload());
			return message.ack();
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
			if ((eventMsg.getData().getCode()!=null)||(eventMsg.getAttributeCode()!= null)) {
				rulesEngineBean.processMsg(eventMsg, payload.getString("token"));
			} 
		}
		long endTime = System.nanoTime();
        log.info("********* Time taken from startTime *********" + startTime + " -> "+ (endTime - startTime)/1000000 + "ms");
		return message.ack();
  }

  


}
