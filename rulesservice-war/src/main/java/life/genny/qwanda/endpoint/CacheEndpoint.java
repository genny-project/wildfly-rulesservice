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
import life.genny.qwandautils.GitUtils;
import javax.inject.Inject;
import life.genny.qwanda.service.Hazel;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.Logger;



/**
 * Cache endpoint
 *
 * @author Adam Crow
 */

@Path("/cache")
@Api(value = "/cache", tags = "cache")
@Produces(MediaType.APPLICATION_JSON)

@Stateless


public class CacheEndpoint {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
  @Inject
  Hazel hazel;
  
	@GET
	@Path("/read/{key}")
	@ApiOperation(value = "cache", notes = "read cache data located at Key")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cacheRead(@PathParam("key") final String key) {
		String results = null;
		results = (String)hazel.getMapBaseEntitys().get(key);
		return Response.status(200).entity(results).build();
	}




}
