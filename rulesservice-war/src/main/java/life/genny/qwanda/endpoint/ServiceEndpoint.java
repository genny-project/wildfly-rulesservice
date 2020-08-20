package life.genny.qwanda.endpoint;

import org.apache.logging.log4j.Logger;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.swagger.annotations.Api;
import life.genny.models.BaseEntityImport;
import life.genny.qwanda.Ask;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.GitUtils;
import life.genny.rules.RulesLoader;
import life.genny.utils.ImportUtils;

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
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import life.genny.qwanda.service.RulesService;
import life.genny.qwanda.service.SecurityService;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ejb.Asynchronous;

import io.vertx.core.json.JsonObject;

/**
 * VService endpoint
 *
 * @author Adam Crow
 */

@Path("/ruleservice")
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
	private RulesService ruleService;

	@Inject
	EventBusBean eventBus;

	@GET
	@Path("/loadrules")
	public Response reloadRules() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {

			RulesLoader.loadRules(securityService.getRealm(), GennySettings.rulesDir);
			return Response.status(200).entity("Loaded").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}
	}

	@GET
	@Path("/loadrules/full/{realm}")
	public Response reloadRulesFull(@PathParam("realm") String realm) {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {
			if (realm == null) {
				realm = securityService.getRealm();
			}
			loadRulesFull(realm);
			return Response.status(200).entity("Loaded").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}

	}

	@GET
	@Path("/loadrules/skipinit/{realm}")
	public Response reloadRulesSkip(@PathParam("realm") String realm) {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {
			if (realm == null) {
				realm = securityService.getRealm();
			}
// Ideally we use the token realm , but it ois not working for me ACC
			loadRules(realm);
			return Response.status(200).entity("Load Process Begun").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}

	}

	@Asynchronous
	private void loadRules(final String realm) {
		RulesLoader.persistRules = false;
		RulesLoader.loadRules(realm, GennySettings.rulesDir);
		RulesLoader.persistRules = true;
	}

	@Asynchronous
	private void loadRulesFull(final String realm) {
		RulesLoader.loadRules(realm, GennySettings.rulesDir);
		(new RulesLoader()).triggerStartupRules(securityService.getRealm(), GennySettings.rulesDir);

	}

	@POST
	@Consumes("application/json")
	@Path("/signals")

	public Response sendSignal(final QEventMessage message) {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {

			return Response.status(200).entity("Sent Signal").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}
	}

	@POST
	@Path("/import/{realm}")
	@Consumes("multipart/form-data")
	@Transactional
	@TransactionTimeout(value = 500, unit = TimeUnit.SECONDS)
	public Response uploadFile(final MultipartFormDataInput input) throws IOException {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			final Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

			// Get file data to save
			final List<InputPart> inputParts = uploadForm.get("attachment");

			for (final InputPart inputPart : inputParts) {
				try {

					final MultivaluedMap<String, String> header = inputPart.getHeaders();
					final String fileName = getFileName(header);

					// convert the uploaded file to inputstream
					final InputStream inputStream = inputPart.getBody(InputStream.class, null);

					// byte[] bytes = IOUtils.toByteArray(inputStream);
					// constructs upload file path
					// writeFile(bytes, fileName);
					// service.importBaseEntitys(inputStream, fileName);

					return Response.status(200).entity("Imported file name : " + fileName).build();

				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String getFileName(final MultivaluedMap<String, String> header) {

		final String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

		for (final String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {

				final String[] name = filename.split("=");

				final String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}

	@GET
	@Path("/googleimport")
	@Produces("application/json")
	@Transactional
	public Response googleImport(@Context final UriInfo uriInfo) {
		log.info("RulesService import using " + securityService.getUserCode());
		if (securityService.inRole("admin") || securityService.inRole("superadmin")
				|| "PER_SERVICE".equals(securityService.getUserCode()) || securityService.inRole("dev")
				|| GennySettings.devMode) {

			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<>();
			qparams.putAll(params);

			final String googleId = params.getFirst("googleid");
			Map<String,String> fieldMapping = new ConcurrentHashMap<String,String>();
			for (String attributeCode : qparams.keySet()) {
				String mapFieldValue = qparams.get(attributeCode).get(0);
				log.info("AttributeCode:"+attributeCode+" <= "+mapFieldValue);
				fieldMapping.put(attributeCode, mapFieldValue);
			}
			Integer count = qparams.keySet().size();
			String sheetName = "Sheet1"; // default
			List<String> sheets = qparams.get("sheet_name");
			if ((sheets != null) && (!sheets.isEmpty())) {
				sheetName = sheets.get(0);
			}
			
			List<BaseEntityImport> beImports =ImportUtils.importGoogleDoc(googleId, sheetName, fieldMapping);
			log.info(beImports);
			return Response.status(200).entity(googleId+":"+count+" items loaded").build();
		} else {
			return Response.status(503).build();
		}
	}

}
