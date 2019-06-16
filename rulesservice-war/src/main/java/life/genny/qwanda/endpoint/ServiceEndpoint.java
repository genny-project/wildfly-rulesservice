package life.genny.qwanda.endpoint;


import org.apache.logging.log4j.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import io.swagger.annotations.Api;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.GitUtils;
import life.genny.rules.RulesLoader;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import life.genny.qwanda.service.SecurityService;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * VService endpoint
 *
 * @author Adam Crow
 */

@Path("/service")
@Api(value = "/service", tags = "service")
@Produces(MediaType.APPLICATION_JSON)

@Stateless
public class ServiceEndpoint {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	@Inject
	private SecurityService securityService;

	@Inject
	EventBusBean eventBus;


  
	@GET
	@Path("/loadrules")
	public Response reloadRules() {
		RulesLoader.loadRules(GennySettings.rulesDir);
		return Response.status(200).entity("Loaded").build();
	}

	@GET
	@Path("/loadrulesfull")
	public Response reloadRulesFull() {
		RulesLoader.loadRules(GennySettings.rulesDir);
		RulesLoader.triggerStartupRules(GennySettings.rulesDir, eventBus);
		return Response.status(200).entity("Loaded").build();
	}

	
	@POST
	@Consumes("application/json")
	@Path("/signals")

	public Response sendSignal(final QEventMessage message) {

		return Response.status(200).entity("Sent").build();
	}



}
