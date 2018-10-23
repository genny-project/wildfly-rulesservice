package life.genny.qwanda.service;

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

@ApplicationScoped
public class Hazel {

	 HazelcastInstance instance;
	 
  private IMap mapBaseEntitys;

  /**
   * @return the mapBaseEntitys
   */
  public IMap getMapBaseEntitys() {
    return mapBaseEntitys;
  }
  
  public IMap getMapBaseEntitys(final String realm) {
	    return  instance.getMap(realm);
	  }

  /**
   * @param mapBaseEntitys the mapBaseEntitys to set
   */
  public void setMapBaseEntitys(final IMap mapBaseEntitys) {
    this.mapBaseEntitys = mapBaseEntitys;
  }

  @PostConstruct
  public void init() {
    Config cfg = new Config();
    cfg.getGroupConfig().setName(GennySettings.username);
    cfg.getGroupConfig().setPassword(GennySettings.username);

    instance = Hazelcast.newHazelcastInstance(cfg);
    mapBaseEntitys = instance.getMap(GennySettings.mainrealm); // To fix
  }

}
