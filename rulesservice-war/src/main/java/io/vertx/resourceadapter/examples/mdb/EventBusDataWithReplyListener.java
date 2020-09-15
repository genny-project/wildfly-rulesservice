package io.vertx.resourceadapter.examples.mdb;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.Asynchronous;
import javax.ejb.MessageDriven;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.jboss.ejb3.annotation.ResourceAdapter;


import io.vavr.Tuple2;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;
import life.genny.jbpm.customworkitemhandlers.RuleFlowGroupWorkItemHandler;
import life.genny.models.GennyToken;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Answers;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.MessageData;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;
import org.jsoup.Connection;

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

	private BaseEntity getUser(BaseEntityUtils beUtils, GennyToken userToken) {
		BaseEntity user = null;
		String userCode = userToken.getUserCode();
		String userUUID = userToken.getUserUUID();

		if (userCode.contains("_AT_")) {
			String email = beUtils.getEmailFromOldCode(userCode);
			user = beUtils.getPersonFromEmail(email);
		}

		if (user == null) {
			user = beUtils.getBaseEntityByCode(userCode);
		}

		if (user== null) {
			user = beUtils.getBaseEntityByCode(userUUID);
		}
		return user;
	}

	@Override
//	@Transactional
	@Asynchronous
	public <T> void onMessage(Message<T> message) {
		log.info("********* THIS IS WILDFLY DATA WITH REPLY LISTENER!!!! *******************");

		long starttime = System.currentTimeMillis();
		Boolean sendEventMessage = false;

		GennyToken userToken = null;
		final JsonObject payload = new JsonObject(message.body().toString());
		QDataAnswerMessage dataMsg = null;
		dataMsg = JsonUtils.fromJson(message.body().toString(), QDataAnswerMessage.class);
		dataMsg.setAliasCode("STATELESS");
		String token = payload.getString("token");

		userToken = new GennyToken("userToken", token);
		log.info("");
		log.info("Incoming App API Call for " + userToken.getUserCode());

		String serviceTokenStr = VertxUtils.getObject(userToken.getRealm(), "CACHE", "SERVICE_TOKEN", String.class);
		GennyToken serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		beUtils.setServiceToken(serviceToken);

		// Extract the device code
		LocalDateTime now = LocalDateTime.now();

		String deviceCode = "UNKNOWN";
		String deviceType = "UNKNOWN";
		String deviceVersion = "UNKNOWN";

		Attribute attributeSync = RulesUtils.getAttribute("PRI_SYNC", userToken);

		BaseEntity user = getUser(beUtils, userToken);

		if (user == null) {
			log.error(String.format("Can not find user BaseEntity neither by UUID:%s nor by userCode:%s, " +
					"will not go further!!", userToken.getUserUUID(), userToken.getUserCode()));
			return;
		}

		Optional<Boolean> optUserCode = user.getValue("PRI_IS_INTERN");
		if (optUserCode.isPresent()) {
			if (optUserCode.get()) {
				sendEventMessage = true;
			}
		}

		// Extract existing codes
		Set<String> updatedCodesList = new HashSet<String>();
		List<Answer> normalAnswers = new ArrayList<Answer>();
		Map<String, BaseEntity> existingBEs = new ConcurrentHashMap<String, BaseEntity>();
		Boolean loadAll = false;
		for (Answer ans : dataMsg.getItems()) {
			if (ans != null) {
				boolean newone = false;
				if ("PRI_DEVICE_CODE".equals(ans.getAttributeCode())) {
					deviceCode = ans.getValue();
				} else if ("PRI_DEVICE_TYPE".equals(ans.getAttributeCode())) {
					deviceType = ans.getValue();
				} else if ("PRI_DEVICE_VERSION".equals(ans.getAttributeCode())) {
					deviceVersion = ans.getValue();
				} else if ("PRI_EXISTING_CODES".equals(ans.getAttributeCode())) {
					if ("EMPTY".equals(ans.getValue())) {
						loadAll = true;
					}
				} else {
					if (ans.getTargetCode().startsWith("JNL_")) {
						BaseEntity existingJnl = beUtils.getBaseEntityByCode(ans.getTargetCode());
						if (existingJnl == null) {
							updatedCodesList.add(ans.getTargetCode());	
						}
					}
					// check if no change
//					BaseEntity be = existingBEs.get(ans.getTargetCode());
//					if (be != null) {
//						String existingAnswer = be.getValueAsString(ans.getAttributeCode());
//						if (existingAnswer != null) {
//							if (!ans.getValue().equals(existingAnswer)) {
//								normalAnswers.add(ans);
//								newone = true;
//							} else {
//								if (ans.getTargetCode().startsWith("JNL_")) {
//									normalAnswers.add(new Answer(userToken.getUserCode(), ans.getTargetCode(),
//											"PRI_LAST_UPDATED", now));
//									normalAnswers.add(new Answer(userToken.getUserCode(), ans.getTargetCode(),
//											"PRI_LAST_CHANGED_BY", userToken.getUserCode()));
//									updatedCodesList.add(ans.getTargetCode());
//									newone = true;
//								}
//
//							}
//						}
//					} else {
						normalAnswers.add(ans);
						if (ans.getTargetCode().startsWith("JNL_")) {
							//updatedCodesList.add(ans.getTargetCode());
						}
						newone = true;
//					}
				}
				if (newone) {
					log.info("INCOMING ANSWER: " + ans.getSourceCode() + ":" + ans.getTargetCode() + ":"
							+ ans.getAttributeCode() + ":" + ans.getValue());

				}
			}
		}

		String uniqueDeviceCode = "DEV_" + deviceCode.toUpperCase() + userToken.getString("sub").hashCode();

		BaseEntity device = beUtils.getBaseEntityByCode(uniqueDeviceCode);
		List<Answer> deviceAnswers = new ArrayList<Answer>();

		if ((device == null)) {
			String deviceName = userToken.getString("given_name") + "'s " + deviceType + " Phone";
			beUtils.create(uniqueDeviceCode, deviceName);

			deviceAnswers.add(new Answer(uniqueDeviceCode, uniqueDeviceCode, "LNK_USER", "[\""+userToken.getUserCode()+"\"]"));
			deviceAnswers.add(new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_DEVICE_CODE", deviceCode));
			deviceAnswers.add(new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_TYPE", deviceType));
			deviceAnswers.add(new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_VERSION", deviceVersion));
			log.info("New device detected -> created " + deviceType + ":" + deviceVersion + ":" + deviceCode
					+ "  associated " + userToken.getUserCode() + "->uniqueCode:" + uniqueDeviceCode);

		}
		if ((device == null) || loadAll) {
			LocalDateTime veryearly = LocalDateTime.of(1970, 01, 01, 0, 0, 0);
			deviceAnswers.add(new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_LAST_UPDATED", veryearly));
			beUtils.saveAnswers(deviceAnswers);

		}

		log.info("Device identified  " + deviceType + ":" + deviceVersion + ":" + deviceCode + "  associated "
				+ userToken.getUserCode() + "->uniqueCode:" + uniqueDeviceCode);

		device = beUtils.getBaseEntityByCode(uniqueDeviceCode);

		if ((normalAnswers != null) && (!normalAnswers.isEmpty())) {
			/*
			 * normalAnswers.add(new
			 * Answer("DEV_"+deviceCode.toUpperCase(),"DEV_"+deviceCode.toUpperCase(),
			 * "PRI_DEVICE_CODE",deviceCode));
			 */
			dataMsg.setItems(normalAnswers.toArray(new Answer[0]));
			
		} else {
			// Only supply device code
			Answer[] defaultAnswerArray = new Answer[1];
			defaultAnswerArray[0] = new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_DEVICE_CODE", deviceCode);
			dataMsg.setItems(defaultAnswerArray);
		}


		Map<String, Object> facts = new ConcurrentHashMap<String, Object>();
		facts.put("serviceToken", serviceToken);
		facts.put("userToken", userToken);
		facts.put("data", dataMsg);
		facts.put("device", new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_DEVICE_CODE", deviceCode));
		RuleFlowGroupWorkItemHandler ruleFlowGroupHandler = new RuleFlowGroupWorkItemHandler();

		log.info("Executing Dataprocessing Rules ");
		long startrulestime = System.currentTimeMillis();
		Map<String,Object> results = null;
		if (dataMsg.getItems().length > 1) { // not just the device used for sync
			results = ruleFlowGroupHandler.executeRules(serviceToken, userToken, facts, "DataProcessing",
					"DataWithReply:DataProcessing");
		}
		
		// save all answers
		if ((results != null) && results.get("answersToSave") != null) {
			log.info("Saving all the answers for "+userToken.getUserCode());
			Answers answers = (Answers)results.get("answersToSave");
			beUtils.saveAnswers(answers.getAnswers());
		} 
		long midrulestime = System.currentTimeMillis();
		log.info("Fetch Stateless Data ");
		// Now fetch any synced data
		results = ruleFlowGroupHandler.executeRules(serviceToken, userToken, facts, "Stateless",
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

				// How many journals?nginx

				Integer unapproved = 0;
				Integer approved = 0;
				Integer rejected = 0;
				Integer interns = 0;

				Set<QDataBaseEntityMessage> distinctMessages = new HashSet<QDataBaseEntityMessage>();
			
				// Check if device requires all sent BEs to have sync attribute set to false to oenable reeseend
				Boolean resend = false;
				try {
					resend = device.getValue("PRI_RESEND",false);
				} catch (Exception e1) {

				}
				
				
				for (QDataBaseEntityMessage mg : msg.getMessages()) {
					Set<BaseEntity> beSet = new HashSet<BaseEntity>();
					for (BaseEntity be : mg.getItems()) {
						if (be.getCode().startsWith("PER_")) {
							Optional<Boolean> optInternCode = be.getValue("PRI_IS_INTERN");
							if (optInternCode.isPresent()) {
								if (optInternCode.get()) {
									interns++;
								}
							}
			
						} else if (be.getCode().startsWith("JNL_")) {
							String status = be.getValue("PRI_STATUS", "UNAPPROVED");
							if ("UNAPPROVED".equals(status)) {
								unapproved++;
							} else if ("APPROVED".equals(status)) {
								approved++;
							} else {
								rejected++;
							}
							
						}
						try {
							if (resend) {
								be.setValue(attributeSync, "FALSE"); // tell the device to send this again
							} else {
								be.setValue(attributeSync, "TRUE"); // tell the device not to send this again
							}
							be.setLinks(null);
							be.setQuestions(null);
														
							beSet.add(be);
						} catch (BadDataException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
						}
						log.info("RETURN " + be.getCode() + ":" + be.getName() + "  - alias :" + mg.getAliasCode());

					}
					mg.setItems(beSet.toArray(new BaseEntity[0]));
					if ((mg.getItems() != null) && (mg.getItems().length > 0)) {

						distinctMessages.add(mg);
					}
				}

				msg.setMessages(distinctMessages.toArray(new QDataBaseEntityMessage[0]));

				log.info("interns = " + interns + ", unapproved = " + unapproved + " , approved = " + approved
						+ " rejected = " + rejected, message);

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
		if (userToken != null) {
			// now update the latest sync time
			beUtils.saveAnswer(new Answer(userToken.getUserCode(), uniqueDeviceCode, "PRI_LAST_UPDATED", now));
			beUtils.saveAnswer(new Answer(userToken.getUserCode(), uniqueDeviceCode, "PRI_RESEND", false));
			long endtime = System.currentTimeMillis();
			log.info("Time to process incoming Data for Rules = " + (startrulestime - starttime) + "ms");
			log.info("Time to run  Data Processing Rules      = " + (midrulestime - startrulestime) + "ms");
			log.info("Time to run  Stateless Rules            = " + (endrulestime - midrulestime) + "ms");
			log.info("Time to run  Post Processing            = " + (endtime - startrulestime) + "ms");
			log.info("Time to run everything                  = " + (endtime - starttime) + "ms");

			log.info("App api call completed for " + userToken.getUserCode() + "\n");
			if (sendEventMessage) {
				String updatedCodes = String.join(",", updatedCodesList);
				facts = new ConcurrentHashMap<String, Object>();
				facts.put("serviceToken", serviceToken);
				facts.put("userToken", userToken);
				QEventMessage msg = new QEventMessage("UPDATE", "JOURNAL_ADD");
				MessageData data = new MessageData("JOURNAL_ADD");
				data.setParentCode(userToken.getUserCode());
				data.setTargetCode(updatedCodes);
				msg.setData(data);
				msg.setToken(serviceToken.getToken());

//				facts.put("data", msg);
//				facts.put("device", new Answer(uniqueDeviceCode, uniqueDeviceCode, "PRI_DEVICE_CODE", deviceCode));
//				ruleFlowGroupHandler = new RuleFlowGroupWorkItemHandler();
//				ruleFlowGroupHandler.executeRules(serviceToken, userToken, facts, "JournalProcessing",
//						"DataWithReply:DataProcessing");
				// VertxUtils.sendEvent("JOURNAL_ADD", userToken.getUserCode(),
				// updatedCodes,userToken);
				sendTheDamnedSlackMessage(userToken, serviceToken, msg);
			}
		}
	}

	public void sendTheDamnedSlackMessage(GennyToken userToken, GennyToken serviceToken, QEventMessage message) {
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		beUtils.setServiceToken(serviceToken);

		String userWhoUpdatedJournals = message.getData().getParentCode();
		String listOfChangedJournals = message.getData().getTargetCode();
		String webhookURL = null;
		String studentName = "Unknown";
		String hostCompany = "Unknown";
		String educationProvider = "Unknown";
		LocalDateTime updateTime = null;

		List<String> changedJournals = new ArrayList<String>(); /*
																 * Stream.of(listOfChangedJournals.split(",",
																 * -1)).collect(Collectors.toList());
																 */
		String[] journalArray = listOfChangedJournals.split(",", -1);
		for (String journalCode : journalArray) {
			changedJournals.add(journalCode);
		}

		List<BaseEntity> journals = new ArrayList<BaseEntity>();
		for (String journalCode : changedJournals) {
			BaseEntity journal = beUtils.getBaseEntityByCode(journalCode);
			journals.add(journal);

		}

		/* studentName = user.getValue("PRI_LASTNAME",true); */
		/* updateTime = journal.getValue("PRI_INTERN_LAST_UPDATE",true); */

		Set<BaseEntity> internSet = new HashSet<BaseEntity>();
		Map<String, BaseEntity> supervisorMap = new HashMap<String, BaseEntity>();

		for (BaseEntity journal : journals) {
			if (journal != null) {
				String journalName = journal.getName();
				String status = journal.getValue("PRI_STATUS", "NO STATUS");

				Optional<String> optHostCompanySupervisorCode = journal.getValue("LNK_INTERN_SUPERVISOR");
//				if (optHostCompanySupervisorCode.isPresent()) {
//					String supervisorCode = optHostCompanySupervisorCode.get();
//					supervisorCode = supervisorCode.substring(2);
//					supervisorCode = supervisorCode.substring(0, (supervisorCode.length() - 2));
//					BaseEntity supervisor = beUtils.getBaseEntityByCode(supervisorCode);
//				}

				Optional<String> optInternCode = journal.getValue("LNK_INTERN");
				if (optInternCode.isPresent()) {
					String internCode = optInternCode.get();
					internCode = internCode.substring(2);
					internCode = internCode.substring(0, (internCode.length() - 2));
					BaseEntity intern = beUtils.getBaseEntityByCode(internCode);
					studentName = intern.getName();
					Optional<String> optEduCode = intern.getValue("LNK_EDU_PROVIDER");
					if (optEduCode.isPresent()) {
						String eduCode = optEduCode.get();
						eduCode = eduCode.substring(2);
						eduCode = eduCode.substring(0, (eduCode.length() - 2));
						BaseEntity edu = beUtils.getBaseEntityByCode(eduCode);
						educationProvider = edu.getName();
					}
					hostCompany = intern.getValue("PRI_ASSOC_HOST_COMPANY", "NOT SET");
				}

				BaseEntity agent = beUtils.getBaseEntityByCode("CPY_OUTCOME_LIFE");
				webhookURL = agent.getValueAsString("PRI_SLACK");

				/* Sending Slack Notification */

				updateTime = LocalDateTime.now();

				JsonObject msgpayload = new JsonObject("{\n" + "   \"blocks\":[\n" + "      {\n"
						+ "         \"type\":\"section\",\n" + "         \"text\":{\n"
						+ "            \"type\":\"mrkdwn\",\n" + "            \"text\":\"New Journal (" + status
						+ ") -> " + journalName + " :memo:\"\n" + "         }\n" + "      },\n" + "      {\n"
						+ "         \"type\":\"divider\"\n" + "      },\n" + "      {\n"
						+ "         \"type\":\"section\",\n" + "         \"fields\":[\n" + "            {\n"
						+ "               \"type\":\"mrkdwn\",\n" + "               \"text\":\"*Student:*\\n"
						+ studentName + "\"\n" + "            },\n" + "            {\n"
						+ "               \"type\":\"mrkdwn\",\n" + "               \"text\":\"*Time:*\\n" + updateTime
						+ "\"\n" + "            },\n" + "            {\n" + "               \"type\":\"mrkdwn\",\n"
						+ "               \"text\":\"*Host Company:*\\n" + hostCompany + "\"\n" + "            },\n"
						+ "            {\n" + "               \"type\":\"mrkdwn\",\n"
						+ "               \"text\":\"*Education Provider:*\\n" + educationProvider + "\"\n"
						+ "            }\n" + "         ]\n" + "      },\n" + "      {\n"
						+ "         \"type\":\"divider\"\n" + "      },\n" + "      {\n"
						+ "         \"type\":\"context\",\n" + "         \"elements\":[\n" + "            {\n"
						+ "               \"type\":\"mrkdwn\",\n"
						+ "               \"text\":\"*Last updated:* 9:15 AM May 22, 2020\"\n" + "            }\n"
						+ "         ]\n" + "      }\n" + "   ]\n" + "}");

				System.out.println("Send Slack message for "+journalName);
				

	
						try {
							QwandaUtils.apiPostEntity(webhookURL, msgpayload.toString(), serviceToken.getToken());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			}
		}


	}
}
