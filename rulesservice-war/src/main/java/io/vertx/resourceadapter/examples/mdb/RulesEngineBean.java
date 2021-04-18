package io.vertx.resourceadapter.examples.mdb;



import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.Logger;

import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.rules.RulesLoader;
import life.genny.rules.RulesLoaderFactory;

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
