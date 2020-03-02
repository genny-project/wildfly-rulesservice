package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import life.genny.qwandautils.GennySettings;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class Hazel {
	
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	 HazelcastInstance instance;
	 
  
  public IMap getMapBaseEntitys(final String realm) {
	    return  instance.getMap(realm);
	  }

 

  @PostConstruct
  public void init() {
	log.info("Initialising Hazel ");
    Config cfg = new Config();
    cfg.getGroupConfig().setName(GennySettings.username);
    cfg.getGroupConfig().setPassword(GennySettings.username);

    instance = Hazelcast.newHazelcastInstance(cfg);
  }

}
