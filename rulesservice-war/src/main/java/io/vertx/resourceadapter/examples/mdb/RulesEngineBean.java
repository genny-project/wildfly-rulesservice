package io.vertx.resourceadapter.examples.mdb;



import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;

import io.vavr.Tuple3;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.KeycloakUtils;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import life.genny.rules.RulesLoaderFactory;
import org.apache.logging.log4j.Logger;
import life.genny.rules.RulesLoader;
import javax.ejb.Stateful;
import javax.ejb.StatefulTimeout;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

//@RequestScoped
//@Stateful
//@StatefulTimeout(unit = TimeUnit.MINUTES, value = 20)
@ApplicationScoped
public class RulesEngineBean {

    /**
     * Stores logger object.
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    private static final ConcurrentHashMap<String, RulesLoader> tokeRulesLoaderMapping = new ConcurrentHashMap<>();

    private RulesLoader getRulesLoader(String token) {
        String sessionState = (String) KeycloakUtils.getJsonMap(token).get("session_state");
        return RulesLoaderFactory.getRulesLoader(sessionState);
    }

    //@Transactional
    //@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processMsg(final Object msg, final String token) {
        RulesLoader rulesLoader = getRulesLoader(token);
        // Add item to queue, process request thread in RulesLoader will pick and process
      //  
        if (GennySettings.useEventQueue) {
        	rulesLoader.addNewItem(msg, token);
        } else {
        	rulesLoader.processMsg(msg, token);
        }

    }
}
