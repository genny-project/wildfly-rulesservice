package io.vertx.resourceadapter.examples.mdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import org.apache.commons.lang3.StringUtils;
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
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QDataGPSMessage;
import life.genny.qwanda.message.QDataPaymentsCallbackMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventBtnClickMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.rule.Rule;
import life.genny.qwanda.service.RulesService;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;
import life.genny.models.GennyToken;

import life.genny.eventbus.EventBusInterface;
import life.genny.jbpm.customworkitemhandlers.RuleFlowGroupWorkItemHandler;
import life.genny.rules.RulesLoader;
import javax.transaction.Transactional;
import javax.ejb.Asynchronous;
import org.jboss.ejb3.annotation.ResourceAdapter;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.TaskAsk;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.utils.VertxUtils;

/**
 * Message-Driven Bean implementation class for: EventBusDataListener
 */

@MessageDriven(name = "EventBusDataWithReplyListener", messageListenerInterface = VertxListener.class, activationConfig = {
		@ActivationConfigProperty(propertyName = "address", propertyValue = "dataWithReply"), })
@ResourceAdapter(value = "rulesservice-ear.ear#vertx-jca-adapter-3.5.4.rar")
public class EventBusDataWithReplyListener implements VertxListener {

//@Inject
//EventBusBean eventBus;

	@Inject
	RulesEngineBean rulesEngineBean;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static Map<String, Object> decodedToken = null;
	static Set<String> userRoles = null;
	private static Map<String, User> usersSession = new HashMap<String, User>();

	static String token;

	/**
	 * Default constructor.
	 */
	public EventBusDataWithReplyListener() {
		// log.info("EventBusDataListener started.");
	}

	@Override
//	@Transactional
  @Asynchronous
	public <T> void onMessage(Message<T> message) {
		
		long starttime = System.currentTimeMillis();
		
		GennyToken userToken = null;
		final JsonObject payload = new JsonObject(message.body().toString());
		QDataAnswerMessage dataMsg = null;
		dataMsg = JsonUtils.fromJson(message.body().toString(), QDataAnswerMessage.class);
		dataMsg.setAliasCode("STATELESS");
		String token = payload.getString("token");

		userToken = new GennyToken("userToken", token);
		log.info("");
		log.info("Incoming App API Call for "+userToken.getUserCode());
		
		String serviceTokenStr = VertxUtils.getObject(userToken.getRealm(), "CACHE", "SERVICE_TOKEN", String.class);
		GennyToken serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		beUtils.setServiceToken(serviceToken);
		
		
		
		// Extract the device code
		LocalDateTime now = LocalDateTime.now();
		
		String deviceCode = "UNKNOWN";
		String deviceType = "UNKNOWN";
		String deviceVersion = "UNKNOWN";
		String deviceLastUpdate = "";
		
		
		

		Attribute attributeSync = RulesUtils.getAttribute("PRI_SYNC", userToken);

		// Extract existing codes
		List<String> existingCodesList = new ArrayList<String>();
		Set<String> updatedCodesList = new HashSet<String>();
		List<Answer> normalAnswers = new ArrayList<Answer>();
		Map<String,BaseEntity> existingBEs = new ConcurrentHashMap<String,BaseEntity>();
		
		for (Answer ans : dataMsg.getItems()) {
			if (ans != null) {
				boolean newone = false;
				if ("PRI_DEVICE_CODE".equals(ans.getAttributeCode())) {
					deviceCode = ans.getValue();
				}
				else if ("PRI_DEVICE_TYPE".equals(ans.getAttributeCode())) {
						deviceType = ans.getValue();
					} 
				else if ("PRI_DEVICE_VERSION".equals(ans.getAttributeCode())) {
					deviceVersion = ans.getValue();
				} 
				else {
					// check if no change
					BaseEntity be = existingBEs.get(ans.getTargetCode());
					if (be!=null) {
						String existingAnswer = be.getValueAsString(ans.getAttributeCode());
						if (existingAnswer!=null) {
							if (!ans.getValue().equals(existingAnswer)) {
								normalAnswers.add(ans);
								newone = true;
							} else {
								if (ans.getTargetCode().startsWith("JNL_")) {
										normalAnswers.add(
												new Answer(userToken.getUserCode(), ans.getTargetCode(), "PRI_LAST_UPDATED", now));
										normalAnswers.add(new Answer(userToken.getUserCode(), ans.getTargetCode(),
												"PRI_LAST_CHANGED_BY", userToken.getUserCode()));
										updatedCodesList.add(ans.getTargetCode());
										newone = true;
								}

							}
						}
					} else {
						normalAnswers.add(ans);
						newone = true;
					}
				}
				if (newone) {
					log.info("INCOMING ANSWER: " + ans.getSourceCode() + ":" + ans.getTargetCode() + ":"
							+ ans.getAttributeCode() + ":" + ans.getValue());

				}
			}
		}
		

		BaseEntity device = beUtils.getBaseEntityByCode("DEV_"+deviceCode.toUpperCase());
		List<Answer> deviceAnswers = new ArrayList<Answer>();

		if (device == null) {
			String deviceName = userToken.getString("given_name")+"'s "+deviceType+" Phone";
			beUtils.create("DEV_"+deviceCode.toUpperCase(), deviceName);
			
			deviceAnswers.add(new Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),"LNK_USER",userToken.getUserCode()));
			deviceAnswers.add(new Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),"PRI_DEVICE_CODE",deviceCode));
			deviceAnswers.add(new Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),"PRI_TYPE",deviceType));
			deviceAnswers.add(new Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),"PRI_VERSION",deviceVersion));
		}
		deviceAnswers.add(new Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),"PRI_LAST_UPDATE",now));
		beUtils.saveAnswers(deviceAnswers);
		
		if ((normalAnswers != null)&&(!normalAnswers.isEmpty())) {
			dataMsg.setItems(normalAnswers.toArray(new Answer[0]));
		}

		List<Tuple2<String, Object>> globals = new ArrayList<Tuple2<String, Object>>();

		Map<String, Object> facts = new ConcurrentHashMap<String, Object>();
		facts.put("serviceToken", serviceToken);
		facts.put("userToken", userToken);
		facts.put("data", dataMsg);
		RuleFlowGroupWorkItemHandler ruleFlowGroupHandler = new RuleFlowGroupWorkItemHandler();

		
		log.info("Executing Rules ");
		long startrulestime = System.currentTimeMillis();
		ruleFlowGroupHandler.executeRules(serviceToken, userToken, facts, "DataProcessing",
				"DataWithReply:DataProcessing");
		long midrulestime = System.currentTimeMillis();
		
		
		

		Map<String, Object> results = ruleFlowGroupHandler.executeRules(serviceToken, userToken, facts, "Stateless",
				"DataWithReply:Stateless");
		long endrulestime = System.currentTimeMillis();
		JsonObject ret = new JsonObject();

		if ((results == null) || results.get("payload") == null) {
			ret.put("status", "ERROR");
		} else {
			ret.put("status", "OK");
			Object obj = results.get("payload");
			String retPayload = null;
			if (obj instanceof QBulkMessage) {
				QBulkMessage msg = (QBulkMessage) results.get("payload");

				// How many journals?

				Integer unapproved = 0;
				Integer approved = 0;
				Integer rejected = 0;

				List<QDataBaseEntityMessage> distinctMessages = new ArrayList<QDataBaseEntityMessage>();

				for (QDataBaseEntityMessage mg : msg.getMessages()) {
					if (mg.getAliasCode() != null) {
						distinctMessages.add(mg);
						continue;
					}
					List<BaseEntity> normalBes = new ArrayList<BaseEntity>();
					
					for (BaseEntity be : mg.getItems()) {
						if (be.getCode().startsWith("JNL_")) {
							String status = be.getValue("PRI_STATUS", "UNAPPROVED");
							if ("UNAPPROVED".equals(status)) {
								unapproved++;
							} else if ("APPROVED".equals(status)) {
								approved++;
							} else {
								rejected++;
							}

								try {
									be.setValue(attributeSync, "TRUE"); // tell the device not to send this again
								} catch (BadDataException e) {
									// TODO Auto-generated catch block
									// e.printStackTrace();
								}
								log.info("RETURN " + be.getCode() + ":" + be.getName() + "  - alias :"
										+ mg.getAliasCode());
								normalBes.add(be);
						

						}
					}
					if (!normalBes.isEmpty()) {
						// update the msg
						mg.setItems(normalBes.toArray(new BaseEntity[0]));
						distinctMessages.add(mg);
					}
				}

				msg.setMessages(distinctMessages.toArray(new QDataBaseEntityMessage[0]));

				log.info("unapproved = " + unapproved + " , approved = " + approved + " rejected = " + rejected,
						message);

				retPayload = JsonUtils.toJson(msg);
			} else if (obj instanceof QDataBaseEntityMessage) {
				QDataBaseEntityMessage msg = (QDataBaseEntityMessage) results.get("payload");
				retPayload = JsonUtils.toJson(msg);
			} else if (obj instanceof QCmdMessage) {
				QCmdMessage msg = (QCmdMessage) results.get("payload");
				retPayload = JsonUtils.toJson(msg);
			} else if (obj instanceof String) {
				String msg = (String) results.get("payload");
				ret.put("value", msg);
			}

			// System.out.println("here is the payload::::::::"+retPayload);
			JsonObject valueJson = new JsonObject(retPayload);

			ret.put("value", valueJson);

		}

		message.reply(ret);
		if (userToken != null)

		{
			long endtime = System.currentTimeMillis();
			log.info("Time to process incoming Data for Rules = "+(startrulestime-starttime)+"ms");
			log.info("Time to run  Data Processing Rules      = "+(midrulestime-startrulestime)+"ms");
			log.info("Time to run  Stateless Rules            = "+(endrulestime-midrulestime)+"ms");
			log.info("Time to run  Post Processing            = "+(endtime-startrulestime)+"ms");
			log.info("Time to run everything                  = "+(endtime-starttime)+"ms");
			
			log.info("App api call completed for " + userToken.getUserCode()+"\n");
		}
	}

}
