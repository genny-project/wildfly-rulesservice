package io.vertx.resourceadapter.examples.mdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;
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
import org.jboss.ejb3.annotation.ResourceAdapter;

import javax.ejb.Asynchronous;
import javax.transaction.Transactional;

/**
 * Message-Driven Bean implementation class for: EventBusSignalListener -
 * listen for Stateful JBPM Messages
 */

@MessageDriven(name = "EventBusSignalListener", messageListenerInterface = VertxListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "address", propertyValue = "signals"),})
@ResourceAdapter(value = "rulesservice-ear.ear#vertx-jca-adapter-3.5.4.rar")
public class EventBusSignalListener implements VertxListener {

    @Inject
    EventBusBean eventBus;

    @Inject
    RulesService rulesService;

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    static String token;
    private static final List<String> roles;

    static {
        roles = TokenIntrospection.setRoles("user");
    }

    /**
     * Default constructor.
     */
    public EventBusSignalListener() {
        //log.info("EventBusSignalListener started.");
    }

    private static ConcurrentHashMap<String, RulesLoader> tokeRulesLoaderMapping = new ConcurrentHashMap<>();

    private RulesLoader getRulesLoader(String token) {
        String sessionState = (String) KeycloakUtils.getJsonMap(token).get("session_state");
        RulesLoader rulesLoader = tokeRulesLoaderMapping.get(sessionState);
        if (rulesLoader == null) {
            rulesLoader = new RulesLoader();
            tokeRulesLoaderMapping.put(sessionState, rulesLoader);
        }
        return rulesLoader;
    }


    @Override
    // @Transactional
    // @Asynchronous
    public <T> void onMessage(Message<T> message) {

        log.info("********* THIS IS WILDFLY SIGNAL LISTENER!!!! *******************");

        final JsonObject payload = new JsonObject(message.body().toString());

        String token = payload.getString("token"); // GODO, this should be grabbed from header
        if (token != null /* && TokenIntrospection.checkAuthForRoles(userToken,roles, token)*/) { // do not allow empty tokens

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
                    log.error("No class def found [" + payload.toString() + "]");
                }
            }
            getRulesLoader(token).processMsg(eventMsg, token);
        }
    }

}