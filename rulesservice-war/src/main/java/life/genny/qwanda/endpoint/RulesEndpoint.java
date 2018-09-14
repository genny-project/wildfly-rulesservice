package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/rules")
@Api(value = "/rules", description = "Rules API", tags = "rules")
@Produces(MediaType.APPLICATION_JSON)


@RequestScoped
public class RulesEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	
	@Inject
	private Service service;

	@Inject
	private SecurityService securityService;

	public static class HibernateLazyInitializerSerializer extends JsonSerializer<JavassistLazyInitializer> {

		@Override
		public void serialize(final JavassistLazyInitializer initializer, final JsonGenerator jsonGenerator,
				final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
			jsonGenerator.writeNull();
		}
	}



	@GET
	@Path("/test/{test}")
	@Produces("application/json")
	public Response getTest() {
		log.info("Rules Test");
		return Response.status(200).build();
	}



}
