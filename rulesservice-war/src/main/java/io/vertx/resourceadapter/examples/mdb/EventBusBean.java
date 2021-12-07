package io.vertx.resourceadapter.examples.mdb;


import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
//import life.genny.channel.Producer;
import life.genny.eventbus.EventBusInterface;

@ApplicationScoped
public class EventBusBean implements EventBusInterface {

	@Inject 
	Producer producer;

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  

	public void write(final String channel, final Object msg) throws NamingException 
	{ 
		String json = msg.toString();
		JsonObject event = new JsonObject(json);
		if("answer".equals(channel)){
			producer.getToanswer().send(event.toString());
		} else if (!StringUtils.isBlank(event.getString("token"))) {
			if(channel.equals("events"))
			{
				producer.getToEvents().send(event.toString());;
			}
			else if(channel.equals("data"))
			{
				producer.getToData().send(event.toString());;
			}
			else if(channel.equals("valid_data"))
			{
				producer.getToValidData().send(event.toString());;
			}
			else if(channel.equals("webdata"))
			{
				producer.getToWebData().send(event.toString());;
			}
			else if(channel.equals("webcmds"))
			{
				producer.getToWebCmds().send(event.toString());;
			}
			else if(channel.equals("cmds"))
			{
				producer.getToCmds().send(event.toString());
			}
			else if(channel.equals("social"))
			{
				producer.getToSocial().send(event.toString());
			}
			else if(channel.equals("signals"))
			{
				producer.getToSignals().send(event.toString());
			}
			else if(channel.equals("statefulmessages"))
			{
				producer.getToStatefulMessages().send(event.toString());
			}
			else if(channel.equals("health"))
			{
				producer.getToHealth().send(event.toString());
			}
			if(channel.equals("messages"))
			{
				producer.getToMessages().send(event.toString());;
			}
			if(channel.equals("services"))
			{
				producer.getToServices().send(event.toString());;
			}
			if(channel.equals("search_events"))
			{
				producer.getToSearchEvents().send(event.toString());;
			}

		} else {
			log.error("No token set for message");
		}

	}

	public void send(final String channel, final Object msg) throws NamingException 
	{
		write(channel,msg);
	}
}
