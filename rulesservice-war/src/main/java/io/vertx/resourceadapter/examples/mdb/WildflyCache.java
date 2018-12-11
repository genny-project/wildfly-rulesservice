package io.vertx.resourceadapter.examples.mdb;


import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import life.genny.channel.DistMap;
import life.genny.qwanda.service.Hazel;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.qwandautils.GennySettings;
import life.genny.eventbus.WildflyCacheInterface;

//@ApplicationScoped
public class WildflyCache implements WildflyCacheInterface {
	

	Hazel inDb;
	
	public WildflyCache(Hazel inDb)
	{
		this.inDb = inDb;
	}

	@Override
	public Object readCache(String key, String token) {

		Object ret = inDb.getMapBaseEntitys().get(key);

		return ret;
	}

	@Override
	public void writeCache(String key, String value, String token,long ttl_seconds) {
		if (value == null) {
			
			inDb.getMapBaseEntitys().remove(key);
		} else {
			inDb.getMapBaseEntitys().put(key, value);
		}
	}

	@Override
	public void clear() {
		inDb.getMapBaseEntitys().clear();
		
	}




}
