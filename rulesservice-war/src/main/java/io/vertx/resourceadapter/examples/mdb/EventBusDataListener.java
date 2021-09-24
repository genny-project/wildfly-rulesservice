package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;
import io.vavr.Tuple;
import io.vavr.Tuple3;
//import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
//import io.vertx.resourceadapter.inflow.VertxListener;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GPS;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataGPSMessage;
import life.genny.qwanda.message.QDataPaymentsCallbackMessage;
import life.genny.qwanda.rule.Rule;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.rules.RulesLoader;
import life.genny.rules.RulesLoaderFactory;
import life.genny.utils.BaseEntityUtils;

@DependsOn("StartupService")
@Startup
@Singleton
public class EventBusDataListener {

	@Inject DummyObject dummy;

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
    public EventBusDataListener() {
    }

	@PostConstruct
	public void dummy(){
	}

    private RulesLoader getRulesLoader(String token) {
        String sessionState = (String) KeycloakUtils.getJsonMap(token).get("session_state");
        return RulesLoaderFactory.getRulesLoader(sessionState);
    }

    @Incoming("data")
    @Merge
    public CompletionStage<Void>  onMessage(Message<String> message) {
        final JsonObject payload = new JsonObject(message.getPayload());
        String token = payload.getString("token");
        payload.remove("token");
        log.debug("Get a data message from Vert.x: " + payload);
        payload.put("token", token);
        long startTime = System.nanoTime();
        log.info("********* KAFKA DATA LISTENER!!!! *********" + startTime +" data came in "+payload);
        
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
            List<Tuple3<String, String, String>> rules = new ArrayList<Tuple3<String, String, String>>();
            rules.add(Tuple.of(ruleGroup, ruleCode, ruleText));

            RulesLoader.addRules(rulesGroup, rules);
        } else if (payload.getString("data_type").equals(Answer.class.getSimpleName())) {
            // log.info("DATA Msg :");;
            try {
                BaseEntityUtils beUtils = new BaseEntityUtils(new GennyToken(token));
                dataMsg = JsonUtils.fromJson(payload.toString(), QDataAnswerMessage.class);
                List<Answer> answers = new ArrayList<Answer>();
                for (Answer answer : dataMsg.getItems()) {
                    if ("PRI_SEARCH_TEXT".equals(answer.getAttributeCode())) {
                        answers.add(answer);
                        continue;
                    }
                    BaseEntity target = beUtils.getBaseEntityByCode(answer.getTargetCode());
                    if (target != null) {
                    Optional<EntityAttribute> optea = target.findEntityAttribute(answer.getAttributeCode());
                    Boolean changed = true;
                    if (optea.isPresent()) {
                        EntityAttribute ea = optea.get();
                        Boolean equaled = false;
                        String valueObj = ea.getAsString();
                        if (valueObj instanceof String) {
                            if (!StringUtils.isBlank(valueObj)) {
                                if (valueObj.equals(answer.getValue())) {
                                    equaled = true;
                                }
                            }
                        }
                        if (equaled && (!"PRI_SUBMIT".equals(answer.getAttributeCode()))) {
                            log.info("This Already exists! " + answer.getAttributeCode() + ":" + answer.getValue());
                            changed = false;
                        }
                    }

                    if (changed) {
                        String key = answer.getSourceCode() + ":" + answer.getTargetCode() + ":"
                                + answer.getAttributeCode();
                        answers.add(answer);
                    }
                    } else {
                    	log.warn(answer.getTargetCode()+" is not present in the system");
                    }
                }

                if (!answers.isEmpty()) {
                    rulesEngineBean.processMsg(dataMsg, payload.getString("token"));
                }
            } catch (com.google.gson.JsonSyntaxException e) {
                log.error("BAD Syntax converting to json from " + dataMsg);
                JsonObject json = new JsonObject(payload.toString());
                JsonObject answerData = json.getJsonObject("items");
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(answerData);
                json.put("items", jsonArray);
                dataMsg = JsonUtils.fromJson(json.toString(), QDataAnswerMessage.class);

                // Request Processor thread in RulesLoader will process
                getRulesLoader(payload.getString("token")).addNewItem(dataMsg, payload.getString("token"));
            }
        } else if (payload.getString("data_type").equals(GPS.class.getSimpleName())) {

            QDataGPSMessage dataGPSMsg = null;
            try {
                dataGPSMsg = JsonUtils.fromJson(payload.toString(), QDataGPSMessage.class);
                getRulesLoader(payload.getString("token")).addNewItem(dataGPSMsg, payload.getString("token"));
            } catch (com.google.gson.JsonSyntaxException e) {

                log.error("BAD Syntax converting to json from " + dataGPSMsg);
                JsonObject json = new JsonObject(payload.toString());
                JsonObject answerData = json.getJsonObject("items");
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(answerData);
                json.put("items", jsonArray);
                dataGPSMsg = JsonUtils.fromJson(json.toString(), QDataGPSMessage.class);
                getRulesLoader(payload.getString("token")).addNewItem(dataGPSMsg, payload.getString("token"));
            }
        } else if (payload.getString("data_type").equals(QDataPaymentsCallbackMessage.class.getSimpleName())) {
            QDataPaymentsCallbackMessage dataCallbackMsg = null;
            try {
                dataCallbackMsg = JsonUtils.fromJson(payload.toString(), QDataPaymentsCallbackMessage.class);
                getRulesLoader(payload.getString("token")).addNewItem(dataCallbackMsg, payload.getString("token"));
            } catch (com.google.gson.JsonSyntaxException e) {

                log.error("BAD Syntax converting to json from " + dataCallbackMsg);
                JsonObject json = new JsonObject(payload.toString());
                dataCallbackMsg = JsonUtils.fromJson(json.toString(), QDataPaymentsCallbackMessage.class);
                getRulesLoader(payload.getString("token")).addNewItem(dataCallbackMsg, payload.getString("token"));
            }
        }
//        long endTime = System.nanoTime();
//        log.info("********* Time taken from startTime *********" + startTime + " -> "+ (endTime - startTime)/1000000 + "ms");
        return message.ack();
    }

}
