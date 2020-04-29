package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.jboss.ejb3.annotation.TransactionTimeout;

import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.SecureResources;
import life.genny.utils.FrameUtils2;


/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional
@TransactionTimeout(value = 4500, unit = TimeUnit.SECONDS)
public class StartupService {

    /**
     * Stores logger object.
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    @Inject
    private SecurityService securityService;

    @Inject
    private SecureResources secureResources;

    @Inject
    private RulesService rulesservice;

    @PostConstruct
    @Transactional
    @TransactionTimeout(value = 4500, unit = TimeUnit.SECONDS)
    public void init() {
        log.info("---------------- Commencing Startup - v 3.1.0  ----------------");
        long startTime = System.nanoTime();

        rulesservice.init();
        securityService.setImportMode(false); // force this to start up
        secureResources.setup(); // force start up

        // Now check if themes and frames loaded up
        log.info("Checking " + FrameUtils2.ruleFires.keySet().size() + " rules for unfired rules ...");

        List<String> fullRuleList = new ArrayList<>();
        List<String> notFiredRuleList = new ArrayList<>();

        for (String rule : FrameUtils2.ruleFires.keySet()) {
            fullRuleList.add(rule);
            if (!FrameUtils2.ruleFires.get(rule)) {
                notFiredRuleList.add(rule);
            }
        }
        log.info("Rule checks done. ");
        log.info("Elements in FullRuleList: " + fullRuleList.size());
        log.info("Elements in NotFiredRuleList: " + notFiredRuleList.size());
        log.info("FullRuleList: " + fullRuleList);
        log.info("NotFiredRuleList: " + notFiredRuleList);


        double difference = (System.nanoTime() - startTime) / 1e9; // get s

        log.info("---------------- Completed Startup in " + difference + " sec ----------------");
    }


}
