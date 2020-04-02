package io.vertx.resourceadapter.examples.mdb;


import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import io.vertx.resourceadapter.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import life.genny.channel.Producer;
import life.genny.cluster.CurrentVtxCtx;
import life.genny.eventbus.EventBusInterface;
import life.genny.qwanda.entity.BaseEntity;

import javax.enterprise.context.RequestScoped;

import io.vertx.core.json.JsonObject;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.JsonUtils;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import life.genny.rules.RulesLoader;

import javax.transaction.Transactional;
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

    private static HashMap<String, RulesLoader> tokeRulesLoaderMapping = new HashMap<>();

    private RulesLoader getRulesLoader(String token) {
        RulesLoader rulesLoader = tokeRulesLoaderMapping.get(token);
        if (rulesLoader == null) {
            rulesLoader = new RulesLoader();
            tokeRulesLoaderMapping.put(token, rulesLoader);
        }
        return rulesLoader;
    }

    //@Transactional
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processMsg(final Object msg, final String token) {
        getRulesLoader(token).processMsg(msg, token);
    }
}
