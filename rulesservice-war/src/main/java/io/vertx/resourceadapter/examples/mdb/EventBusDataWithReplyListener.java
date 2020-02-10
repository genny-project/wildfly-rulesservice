package io.vertx.resourceadapter.examples.mdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
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
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
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
import life.genny.utils.BaseEntityUtils;
import life.genny.models.GennyToken;

import life.genny.eventbus.EventBusInterface;
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
//  @Asynchronous
	public <T> void onMessage(Message<T> message) {
		final JsonObject payload = new JsonObject(message.body().toString());
		QDataAnswerMessage dataMsg = null;
	    dataMsg = JsonUtils.fromJson(message.body().toString(), QDataAnswerMessage.class);
        dataMsg.setAliasCode("STATELESS");

        String token = payload.getString("token");

		GennyToken userToken = new GennyToken("userToken", token);
		String serviceTokenStr = VertxUtils.getObject(userToken.getRealm(), "CACHE", "SERVICE_TOKEN", String.class);
		GennyToken serviceToken = new GennyToken("PER_SERVICE", serviceTokenStr);

		List<Tuple2<String, Object>> globals = new ArrayList<Tuple2<String, Object>>();
	    List<Object> facts = new ArrayList<Object>();
	    facts.add(dataMsg);
	    facts.add(userToken);
	    facts.add(serviceToken);
	    RulesLoader.executeStateless(globals, facts, serviceToken, userToken);

        System.out.println("here is the payload::::::::"+payload);
        message.reply(payload);
	}

}
