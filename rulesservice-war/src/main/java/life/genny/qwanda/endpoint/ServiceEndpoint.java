package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import io.swagger.annotations.Api;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import life.genny.models.BaseEntityImport;
import life.genny.models.GennyToken;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.service.RulesService;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwandautils.GennySettings;
import life.genny.rules.RulesLoader;
import life.genny.rules.listeners.GennyRuleTimingListener;
import life.genny.utils.DefUtils;
import life.genny.utils.ImportUtils;
import life.genny.utils.RulesUtils;
import life.genny.utils.VertxUtils;

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
	private RulesService ruleService;

	@Inject
	EventBusBean eventBus;

	@GET
	@Path("/loaddefs")
	public Response reloadDefs() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {

			// get token
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				

			DefUtils.loadDEFS(userToken);
			return Response.status(200).entity("Loaded").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}
	}

	
	@GET
	@Path("/loadrules")
	public Response reloadRules() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {

			// get token!
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				
			
			Boolean changeInRules = RulesLoader.loadRules(productCode,GennySettings.rulesDir,false);
			Boolean noskip = true;
			JsonObject skipJson = VertxUtils.readCachedJson("JENNY", "SKIP");
			if (skipJson.containsKey("status")) {
				if ("ok".equalsIgnoreCase(skipJson.getString("status"))) {
					String val = skipJson.getString("value");
					if ("TRUE".equalsIgnoreCase(val)) {
						changeInRules = false;
						noskip = false;
					}
				}
			}
			log.info("SKIP JENNY JSON = "+skipJson.toString());
			log.info("Change in rules that requires generateResources is "+(changeInRules?"YES":"NO"));
			if (noskip &&(changeInRules) /*|| (!"TRUE".equalsIgnoreCase(System.getenv("DISABLE_INIT_RULES_STARTUP"))))*/) {
				log.info("Rulesservice triggering rules");
				(new RulesLoader()).triggerStartupRules(GennySettings.rulesDir);
			} else {
				log.warn("DISABLE_INIT_RULES_STARTUP IS TRUE -> No Init Rules triggered. SKIP CACHE = "+(noskip?"FALSE":"TRUE"));
			}log.info("Loaded Rules!");
			return Response.status(200).entity("Loaded").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}
	}
	
	@GET
	@Path("/loadrules/defs")
	public Response reloadRulesDefs() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {

			// get token
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				

			Boolean noChangeInRules = RulesLoader.loadRules(productCode,GennySettings.rulesDir,true);
			Boolean noskip = true;
			JsonObject skipJson = VertxUtils.readCachedJson("JENNY", "SKIP");
			if (skipJson.containsKey("status")) {
				if ("ok".equalsIgnoreCase(skipJson.getString("status"))) {
					String val = skipJson.getString("value");
					if ("TRUE".equalsIgnoreCase(val)) {
						noChangeInRules = true;
						noskip = false;
					}
				}
			}
			log.info("SKIP JENNY JSON = "+skipJson.toString());
			
			if (noskip &&((!noChangeInRules) || (!"TRUE".equalsIgnoreCase(System.getenv("DISABLE_INIT_RULES_STARTUP"))))) {
				log.info("Rulesservice triggering rules");
				(new RulesLoader()).triggerStartupRules(GennySettings.rulesDir);
			} else {
				log.warn("DISABLE_INIT_RULES_STARTUP IS TRUE -> No Init Rules triggered. SKIP CACHE = "+(noskip?"FALSE":"TRUE"));
			}
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
			
			// get token
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				

			
			if (realm == null) {
				realm = securityService.getRealm();
			}
			loadRulesFull(productCode);
			return Response.status(200).entity("Loaded").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}

	}	
	
	
	@GET
	@Path("/loadattributes/{realm}")
	public Response loadAttributes(@PathParam("realm") String realm) {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {
			// get token
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				

			if (realm == null) {
				realm = securityService.getRealm();
			}
			GennyToken gennyToken = new GennyToken(securityService.getToken());
			gennyToken.setProjectCode(realm);
			RulesUtils.loadAllAttributesIntoCache(gennyToken);
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
			// get token
			String token = securityService.getToken();
			GennyToken userToken = new GennyToken(token);
			
			String productCode = userToken.getRealm();
				

			if (realm == null) {
				realm = securityService.getRealm();
			}
// Ideally we use the token realm , but it ois not working for me ACC
			loadRules(productCode);
			return Response.status(200).entity("Load Process Begun").build();
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}

	}

	
	@GET
	@Path("/showruleslogs/{realm}/{count}")
	public Response showRulesLogs(@PathParam("realm") String realm,@PathParam("count") Integer count) {
	
	if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
			|| GennySettings.devMode) {
		if (realm == null) {
			realm = securityService.getRealm();
		}	
		String filter = "";
		if (count >0 ) {
			filter = "count:"+count;
		}
		
		GennyRuleTimingListener.showLogs(filter);
	return Response.status(200).entity("Load Process Begun").build();
} else {
	return Response.status(401).entity("Unauthorized").build();
}

}
	
	@GET
	@Path("/clearruleslogs/{realm}")
	public Response clearRulesLogs(@PathParam("realm") String realm) {
	
	if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
			|| GennySettings.devMode) {
		if (realm == null) {
			realm = securityService.getRealm();
		}	
		
		GennyRuleTimingListener.clearLogs();
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
	@Consumes("multipart/form-data")
	@Path("/postrule")
	public Response postRule(final MultipartFormDataInput input) {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")
				|| GennySettings.devMode) {
      String realm = securityService.getRealm();
			final Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

			log.info("Rules postrule used");
			// Get file data to save
			final List<InputPart> inputParts = uploadForm.get("attachment");

			for (final InputPart inputPart : inputParts) {
				try {

					final MultivaluedMap<String, String> header = inputPart.getHeaders();
					final String fileName = getFileName(header);

					RulesLoader.reloadRules(securityService.getRealm(),fileName,inputPart.getBodyAsString());

					//List<Tuple3<String, String, String>> rules = RulesLoader.processFileRealmsFromFiles(
							//"genny",
							//GennySettings.rulesDir, 
							//RulesLoader.realms
							//);

					//Comparator<Tuple3<String, String, String>> byRealm = (o1, o2)-> 
						//Optional.of(o1._1)
						//.filter("genny"::equals)
						//.map(d-> -1)
						//.orElse(1);

					//Map<String, String> distictRules = rules.stream()
						//.sorted(byRealm)
						//.map(d -> {
							//Map<String, String> map = new HashMap<>();
							//map.put(d._2, d._3);
							//return map;
						//})
					//.reduce((d1,d2)-> {
						//d1.putAll(d2);
						//return d1;
					//})
					//.get();

					//KieServices ks = RulesLoader.ks;
					//KieFileSystem kfs = ks.newKieFileSystem();

					//String kjarFolerPath ="src/main/resources/life/genny/rules/";
					//String newResourceAbsolutePath =kjarFolerPath+ fileName;
					//distictRules.entrySet().stream().forEach(d ->{
						//String resourceAbsolutePath =kjarFolerPath+ d.getKey();
						//Resource rs = ks.getResources().newReaderResource(new StringReader(d.getValue()));
						//kfs.write(resourceAbsolutePath, rs.setResourceType(ResourceType.DRL));
						//kfs.delete(newResourceAbsolutePath);
					//});
					//Resource rs = ks.getResources().newReaderResource(new StringReader(inputPart.getBodyAsString()));
					//kfs.write(newResourceAbsolutePath, rs.setResourceType(ResourceType.DRL));

					//final KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
					//ReleaseId releaseId = kieBuilder.getKieModule().getReleaseId();
					//final KieContainer kContainer = ks.newKieContainer(releaseId);
					//final KieBaseConfiguration kbconf = ks.newKieBaseConfiguration();
					//kbconf.setProperty("name",realm);
					//kbconf.setProperty(ConsequenceExceptionHandlerOption.PROPERTY_NAME, "life.genny.utils.GennyRulesExceptionHandler");
					//final KieBase kbase = kContainer.newKieBase(kbconf);

					//log.info("Put rules KieBase into Custom Cache");
					//if (RulesLoader.getKieBaseCache().containsKey(realm)) {
						//RulesLoader.getKieBaseCache().remove(realm);
					//}
					//RulesLoader.getKieBaseCache().put(realm, kbase);
					//log.info("Internmatch"+ " rules installed\n");
					return Response.status(200).entity("Imported file name : " + fileName).build();

				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		return null;
		} else {
			return Response.status(401).entity("Unauthorized").build();
		}
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
