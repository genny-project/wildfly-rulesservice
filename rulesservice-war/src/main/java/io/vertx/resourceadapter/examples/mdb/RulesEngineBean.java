package io.vertx.resourceadapter.examples.mdb;



import javax.enterprise.context.RequestScoped;

import io.vavr.Tuple3;
import life.genny.qwandautils.KeycloakUtils;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import life.genny.rules.RulesLoader;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@RequestScoped
@Stateful
@StatefulTimeout(unit = TimeUnit.MINUTES, value = 20)
public class RulesEngineBean {

    /**
     * Stores logger object.
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private static final ConcurrentHashMap<String, RulesLoader> tokeRulesLoaderMapping = new ConcurrentHashMap<>();

    private RulesLoader getRulesLoader(String token) {
        String sessionState = (String) KeycloakUtils.getJsonMap(token).get("session_state");
        RulesLoader rulesLoader = tokeRulesLoaderMapping.get(sessionState);
        if (rulesLoader == null) {
            rulesLoader = new RulesLoader();
            tokeRulesLoaderMapping.put(sessionState, rulesLoader);
        }
        return rulesLoader;
    }

    //@Transactional
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processMsg(final Object msg, final String token) {
        UUID uuid = UUID.randomUUID();
        Tuple3<Object, String, UUID> tuple3 = new Tuple3<>(msg, token, uuid);
        RulesLoader rulesLoader = getRulesLoader(token);
        try {
            // RequestProcessor in RulesLoader will get item from queue and process it
            rulesLoader.addNewItem(tuple3);
        } catch (InterruptedException ie) {
            log.error("InterruptedException occurred:" + ie.getMessage());
        }
//        rulesLoader.processMsg(msg, token);
    }
}
