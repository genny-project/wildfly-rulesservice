package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.annotations.Merge;
import io.vertx.core.json.JsonObject;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.User;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.service.RulesService;
import life.genny.rules.RulesLoader;

@DependsOn("StartupService")
@Startup
@Singleton
public class EventBusCmdListener {
	

@Inject
EventBusBean eventBus;

@Inject
RulesService rulesService;

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	static Map<String, Object> decodedToken = null;
	static Set<String> userRoles = null;
	private static Map<String, User> usersSession = new HashMap<String, User>();

	
	@Inject DummyObject dummy;

	public EventBusCmdListener(){
	}

	@PostConstruct
	public void dummy(){
	}


	static String token;


	@Incoming("cmds")
  @Merge
  public CompletionStage<Void> fromWebCmds(Message<String>  message) {
		final JsonObject payload = new JsonObject(message.getPayload());
		log.info("Get a data message from Vert.x: " + payload);
		log.info("********* THIS IS WILDFLY CMD LISTENER!!!! *******************");

		QCmdMessage cmdMsg = null;

		String token = payload.getString("token");

		GennyToken gennyToken = new GennyToken(token);


		if (gennyToken.hasRole("dev")) {

			if (payload.getString("msg_type").equals("CMD_MSG")) {
				if (payload.getString("cmd_type").equals("CMD_RELOAD_RULES")) {
					if (payload.getString("code").equals("RELOAD_RULES_FROM_FILES")) {
						RulesLoader.loadRules(gennyToken.getRealm(),"/rules");
					}
				}
			}
		}
		return message.ack();
  }

  


}
