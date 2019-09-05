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
import javax.enterprise.context.ApplicationScoped;
import io.vertx.core.json.JsonObject;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.JsonUtils;
import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class EventBusBean implements EventBusInterface {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  

	public void write(final String channel, final Object msg) throws NamingException 
	{
		String json = msg.toString();
		JsonObject event = new JsonObject(json);
		if (!StringUtils.isBlank(event.getString("token"))) {
		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		      conn = connFactory.getVertxConnection();
		    //  log.info("Publishing Vertx Bus Message on channel "+channel+":");

		      conn.vertxEventBus().publish(channel, event);
		    //  log.info("Published Vertx Bus Message on channel "+channel);
		    } catch (Exception e) {
		      e.printStackTrace();
		    } finally {
		      if (ctx != null) {
		        ctx.close();
		      }
		      if (conn != null) {
		    	  try {
		        conn.close();
		    	  } catch (ResourceException e) {
		    		  e.printStackTrace();
		    	  }
		      }
		    }
		} else {
			log.error("No token set for message");
		}
	}
  

	public void send(final String channel, final Object msg) throws NamingException 
	{
		//String msgStr = JsonUtils.toJson(msg);
	   //   JsonObject event = new JsonObject(msgStr);
		String json = msg.toString();
		JsonObject event = new JsonObject(json);  // TODO, change this to use an original JsonObject
	      

		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		     // log.info("Sending Vertx Bus Message on channel "+channel+":");
		      DeliveryOptions options = new DeliveryOptions();
		      conn = connFactory.getVertxConnection();

		      conn.vertxEventBus().send(channel, event,options);
		     // log.info("Sent Vertx Bus Message on channel "+channel);
		    } catch (Exception e) {
		      e.printStackTrace();
		    } finally {
		      if (ctx != null) {
		        ctx.close();
		      }
		      if (conn != null) {
		    	  try {
		        conn.close();
		    	  } catch (ResourceException e) {
		    		  e.printStackTrace();
		    	  }
		      }
		    }

	}
 

}
