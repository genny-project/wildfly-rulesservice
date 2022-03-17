package io.vertx.resourceadapter.examples.mdb;

import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecordMetadata;
import io.vertx.core.json.JsonObject;
// import life.genny.channel.Producer;
import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.NamingException;

import life.genny.data.BridgeSwitch;
import life.genny.eventbus.EventBusInterface;
import life.genny.models.GennyToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class EventBusBean implements EventBusInterface {

  @Inject Producer producer;

  /** Stores logger object. */
  protected static final Logger log =
      org.apache.logging.log4j.LogManager.getLogger(
          MethodHandles.lookup().lookupClass().getCanonicalName());

  public void write(final String channel, final Object msg) throws NamingException {
    String json = msg.toString();
    JsonObject event = new JsonObject(json);
    GennyToken userToken = new GennyToken(event.getString("token"));

	// create metadata for correct bridge
	OutgoingKafkaRecordMetadata<String> metadata = null;

	if ("webcmds".equals(channel) || "webdata".equals(channel)) {

		String bridgeId = BridgeSwitch.get(userToken);

		if (bridgeId == null) {
			log.warn("No Bridge ID found for " + userToken.getUserCode() + " : " + userToken.getUniqueId());

			bridgeId = BridgeSwitch.activeBridgeIds.iterator().next();
			log.warn("Sending to " + bridgeId + " instead!");
		}
		metadata = OutgoingKafkaRecordMetadata.<String>builder()
			.withTopic(bridgeId + "-" + channel)
			.build();
	}

    if ("answer".equals(channel)) {
      producer.getToanswer().send(event.toString());
    } else if (!StringUtils.isBlank(event.getString("token"))) {
      if (channel.equals("events")) {
        producer.getToEvents().send(event.toString());

      } else if (channel.equals("data")) {
        producer.getToData().send(event.toString());

      } else if (channel.equals("valid_data")) {
        producer.getToValidData().send(event.toString());

      } else if (channel.equals("webdata")) {
        producer.getToWebData().send(Message.of(event.toString()).addMetadata(metadata));
        // producer.getToWebData().send(event.toString());

      } else if (channel.equals("webcmds")) {
        producer.getToWebCmds().send(Message.of(event.toString()).addMetadata(metadata));
        // producer.getToWebCmds().send(event.toString());

      } else if (channel.equals("cmds")) {
        producer.getToCmds().send(event.toString());

      } else if (channel.equals("social")) {
        producer.getToSocial().send(event.toString());

      } else if (channel.equals("signals")) {
        producer.getToSignals().send(event.toString());

      } else if (channel.equals("statefulmessages")) {
        producer.getToStatefulMessages().send(event.toString());

      } else if (channel.equals("health")) {
        producer.getToHealth().send(event.toString());

      } else if (channel.equals("messages")) {
        producer.getToMessages().send(event.toString());

      } else if (channel.equals("services")) {
        producer.getToServices().send(event.toString());

      } else if (channel.equals("search_events")) {
        producer.getToSearchEvents().send(event.toString());

      }

    } else {
      log.error("No token set for message");
    }
  }

  public void send(final String channel, final Object msg) throws NamingException {
    write(channel, msg);
  }
}
