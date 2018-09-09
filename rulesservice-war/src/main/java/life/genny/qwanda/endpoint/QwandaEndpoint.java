package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.servlet.ServletContext;
import javax.transaction.Transactional;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.json.JSONObject;
import org.keycloak.representations.AccessTokenResponse;
import org.mortbay.log.Log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import life.genny.qwanda.Answer;
import life.genny.qwanda.AnswerLink;
import life.genny.qwanda.Ask;
import life.genny.qwanda.GPS;
import life.genny.qwanda.GPSLocation;
import life.genny.qwanda.GPSRoute;
import life.genny.qwanda.GPSRouteStatus;
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.QuestionSourceTarget;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeBoolean;
import life.genny.qwanda.attribute.AttributeDate;
import life.genny.qwanda.attribute.AttributeDateTime;
import life.genny.qwanda.attribute.AttributeDouble;
import life.genny.qwanda.attribute.AttributeInteger;
import life.genny.qwanda.attribute.AttributeLong;
import life.genny.qwanda.attribute.AttributeMoney;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.AttributeTime;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.controller.Controller;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QDataQSTMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.rule.Rule;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GPSUtils;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.security.SecureResources;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/qwanda")
@Api(value = "/qwanda", description = "Qwanda API", tags = "qwanda")
@Produces(MediaType.APPLICATION_JSON)

// @Stateless
@RequestScoped
public class QwandaEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	Boolean searchDevMode = "TRUE".equals(System.getenv("SEARCHDEV"));
	
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

	@POST
	@Consumes("application/json")
	@Path("/rules")
	public Response create(final Rule entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/attributes")
	public Response create(final Attribute entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/questions")
	@Transactional
	public Response create(final Question entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/asks")
	public Response create(final Ask entity) {

		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}


	// TODO: should be POST
	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {

		List<Ask> asks = service.createAsksByQuestionCode(questionCode, sourceCode, targetCode);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		String json = JsonUtils.toJson(askMsgs);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks3" + "/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsks2(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {
		List<Ask> asks = service.createAsksByQuestionCode2(questionCode, sourceCode, targetCode);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
		String json = JsonUtils.toJson(askMsgs);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks2" + "/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsks3(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {
		List<Ask> asks = null;

		asks = service.createAsksByQuestionCode2(questionCode, sourceCode, targetCode);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
		String json = JsonUtils.toJson(askMsgs);
		return Response.status(200).entity(json).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/asks/qst")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsksUsingQST(final QDataQSTMessage msg) {
		List<Ask> asks = null;
		final QuestionSourceTarget defaultQST = msg.getRootQST();
		Question rootQuestion = service.findQuestionByCode(msg.getRootQST().getQuestionCode());
		asks = service.findAsksUsingQuestionSourceTarget(rootQuestion, msg.getItems(), defaultQST);
		// asks = service.createAsksByQuestionCode2(questionSourceTargetArray);
		log.debug("Number of asks=" + asks.size());
		log.debug("Number of asks=" + asks);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
		String json = JsonUtils.toJson(askMsgs);
		return Response.status(200).entity(json).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/gpss")
	public Response create(final GPS entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/answers/bulk2")

	public Response createBulk2(final QDataAnswerMessage entitys) {

		for (Answer entity : entitys.getItems()) {
			if (entity == null) {
				log.error("Null Entity posted");
				continue;
			}
			if (entity.getAttribute() == null) {
				Attribute attribute = null;

				try {
					attribute = service.findAttributeByCode(entity.getAttributeCode());
				} catch (NoResultException e) {
					// Create it (ideally if user is admin)
					String name = entity.getAttributeCode().substring(4).toLowerCase();
					name = name.replaceAll("_", " ");
					attribute = new AttributeText(entity.getAttributeCode(),
							StringUtils.capitalize(name));
					service.insert(attribute);
					attribute = service.findAttributeByCode(entity.getAttributeCode());
				}
				entity.setAttribute(attribute);
			}

		}
		service.insert(entitys.getItems());
		return Response.status(200).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/answers/bulk")

	public Response createBulk(final QDataAnswerMessage entitys) {

		for (Answer entity : entitys.getItems()) {
			if (entity == null) {
				log.error("Null Entity posted");
				continue;
			}
			if (entity.getAttribute() == null) {
				Attribute attribute = null;

				try {
					attribute = service.findAttributeByCode(entity.getAttributeCode());
				} catch (NoResultException e) {
					// Create it (ideally if user is admin)
					if (entity.getAttributeCode().startsWith("PRI_IS_")) {
						attribute = new AttributeBoolean(entity.getAttributeCode(),
								StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
					} else {
						if (entity.getDataType()!=null) {
							switch (entity.getDataType()) {
							case "java.lang.Integer":
							case "Integer":
								attribute = new AttributeInteger(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;
							case "java.time.LocalDateTime":
							case "LocalDateTime":
								attribute = new AttributeDateTime(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;
							case "java.lang.Long":
							case "Long":
								attribute = new AttributeLong(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;
							case "java.time.LocalTime":
							case "LocalTime":
								attribute = new AttributeTime(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;
							case "org.javamoney.moneta.Money":
							case "Money":
								attribute = new AttributeMoney(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;

							case "java.lang.Double":
							case "Double":
								attribute = new AttributeDouble(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;

							case "java.lang.Boolean":
								attribute = new AttributeBoolean(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;

							case "java.time.LocalDate":
								attribute = new AttributeDate(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;


							case "java.lang.String":
							default:
								attribute = new AttributeText(entity.getAttributeCode(),
										StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
								break;

							}						
							} else {
					attribute = new AttributeText(entity.getAttributeCode(),
							StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
					}}
					service.insert(attribute);
					attribute = service.findAttributeByCode(entity.getAttributeCode());
				}
				entity.setAttribute(attribute);
			}
			service.insert(entity);
		}

		return Response.status(200).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/answers")

	public Response create(final Answer entity) {
		Attribute attribute = null;
		if (entity.getAttribute() == null) {
			

			try {
				attribute = service.findAttributeByCode(entity.getAttributeCode());
			} catch (NoResultException e) {
				// Create it (ideally if user is admin)
				if (entity.getAttributeCode().startsWith("PRI_IS_")) {
					attribute = new AttributeBoolean(entity.getAttributeCode(),
							StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
				} else {
					if (entity.getDataType()!=null) {
						switch (entity.getDataType()) {
						case "java.lang.Integer":
						case "Integer":
							attribute = new AttributeInteger(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;
						case "java.time.LocalDateTime":
						case "LocalDateTime":
							attribute = new AttributeDateTime(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;
						case "java.lang.Long":
						case "Long":
							attribute = new AttributeLong(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;
						case "java.time.LocalTime":
						case "LocalTime":
							attribute = new AttributeTime(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;
						case "org.javamoney.moneta.Money":
						case "Money":
							attribute = new AttributeMoney(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;

						case "java.lang.Double":
						case "Double":
							attribute = new AttributeDouble(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;

						case "java.lang.Boolean":
							attribute = new AttributeBoolean(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;

						case "java.time.LocalDate":
							attribute = new AttributeDate(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;


						case "java.lang.String":
						default:
							attribute = new AttributeText(entity.getAttributeCode(),
									StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
							break;

						}						
						} else {
				attribute = new AttributeText(entity.getAttributeCode(),
						StringUtils.capitalize(entity.getAttributeCode().substring(4).toLowerCase()));
				}
					}
				service.insert(attribute);
				attribute = service.findAttributeByCode(entity.getAttributeCode());
			}
			entity.setAttribute(attribute);
		}
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys")
	public Response create(final BaseEntity entity) {
		Long ret = service.insert(entity);
		return Response.status(200).entity(ret).build();
	}

	@GET
	@Path("/rules/{id}")
	public Response fetchRuleById(@PathParam("id") final Long id) {
		final Rule entity = service.findRuleById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/search")
	@Produces("application/json")
	@Transactional
	public Response findBySearchBE(@Context final UriInfo uriInfo) {

		if ((securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || GennySettings.devMode) {

		BaseEntity searchBE = new BaseEntity("SER_");
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		if (pageStartStr != null) {
			Attribute attributeInteger = new AttributeInteger("QRY_PAGE_START", "PageStart");
			try {
				searchBE.setValue(attributeInteger, Integer.decode(pageStartStr));
			} catch (NumberFormatException | BadDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			Attribute attributeInteger = new AttributeInteger("QRY_PAGE_SIZE", "PageSize");
			try {
				searchBE.setValue(attributeInteger, Integer.decode(pageSizeStr));
			} catch (NumberFormatException | BadDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			qparams.remove("pageSize");
		}
		List<BaseEntity> results = service.findBySearchBE(searchBE);

		Long total = -1L;

		try {
			total = service.findBySearchBECount(searchBE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			total = -1L;
		}

		BaseEntity[] beArr = new BaseEntity[results.size()];
		beArr = results.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, searchBE.getCode(), null);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();
		} else {
			return Response.status(503).build();
		}
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys/search2")
	@Produces("application/json")
	@Transactional
	public Response findBySearchBE3(final String hql) {

		Log.info("Search " + hql);
		if ((securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || GennySettings.devMode) {

			List<BaseEntity> results = service.findBySearchBE2(hql);
			BaseEntity[] beArr = new BaseEntity[results.size()];
			beArr = results.toArray(beArr);
			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, "GRP_ROOT", null);
			msg.setTotal(0L);
			String json = JsonUtils.toJson(msg);
			return Response.status(200).entity(json).build();
		} else {
			return Response.status(401).build();
		}
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys/search3")
	@Produces("application/json")
	@Transactional
	public Response findBySearchBE4(final String hql) {

		Log.info("Search " + hql);
		if ((securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || GennySettings.devMode) {

			List<Object> results = service.findBySearchBE3(hql);
			String json = JsonUtils.toJson(results);
			return Response.status(200).entity(json).build();
		} else {
			return Response.status(401).build();
		}
	}

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/search2/{hql}")
	@Produces("application/json")
	@Transactional
	public Response findBySearchBE2(@PathParam("hql") final String hql) {

		Log.info("Search " + hql);
		if ((securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || GennySettings.devMode) {

			List<BaseEntity> results = service.findBySearchBE2(hql);
			BaseEntity[] beArr = new BaseEntity[results.size()];
			beArr = results.toArray(beArr);
			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, "GRP_ROOT", null);
			msg.setTotal(0L);
			String json = JsonUtils.toJson(msg);
			return Response.status(200).entity(json).build();
		} else {
			return Response.status(401).build();
		}
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys/search")
	@Produces("application/json")
	@Transactional
	public Response findBySearchBE(final BaseEntity searchBE) {

		Log.info("Search " + searchBE);
		

		// Force any user that is not admin to have to use their own code
		if (!(securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || searchDevMode) {  // TODO Remove the true!
			String stakeHolderCode = null;
			stakeHolderCode = "PER_"+QwandaUtils.getNormalisedUsername((String) securityService.getUserMap().get("username")).toUpperCase();

			Attribute stakeHolderAttribute = new AttributeText("SCH_STAKEHOLDER_CODE", "StakeholderCode");
			Attribute sourceStakeHolderAttribute = new AttributeText("SCH_SOURCE_STAKEHOLDER_CODE", "SourceStakeholderCode");
			try {
				
				if(searchBE.containsEntityAttribute("SCH_SOURCE_STAKEHOLDER_CODE")) {
					searchBE.addAttribute(sourceStakeHolderAttribute, new Double(1.0),
							stakeHolderCode);
				}else {
					searchBE.addAttribute(stakeHolderAttribute, new Double(1.0),
							stakeHolderCode);
				}
			
			} catch (BadDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// pass the stakeHolderCode through
			
		}
		Long startTime = System.nanoTime();
			List<BaseEntity> results = service.findBySearchBE(searchBE);
			System.out.println("search from db takes us to " + ((System.nanoTime() - startTime) / 1e6) + "ms");
			Long total = -1L;
 
			try {
				total = service.findBySearchBECount(searchBE);
				System.out.println("search count takes us to " + ((System.nanoTime() - startTime) / 1e6) + "ms");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				total = -1L;
			}

			BaseEntity[] beArr = null;
			if (!((results==null)||(results.isEmpty()))) {
				beArr = new BaseEntity[results.size()];
				beArr = results.toArray(beArr);
			} else {
				beArr = new BaseEntity[0];
				total = 0L;
			}
			
			// Override returned parent code if it is supplied, otherwise use search BE code
			String parentCode = searchBE.getCode();
			Optional<EntityAttribute> parentAttribute = searchBE.findEntityAttribute("SCH_SOURCE_CODE");
			if (parentAttribute.isPresent()) {
				parentCode = parentAttribute.get().getValueString();
			}
			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, parentCode, null);
			msg.setTotal(total);
			String json = JsonUtils.toJson(msg);
			return Response.status(200).entity(json).build();
		

	}

	@GET
	@Path("/baseentitys/{sourceCode}")
	public Response fetchBaseEntityByCode(@Context final ServletContext servletContext,
			@PathParam("sourceCode") final String code) {

		BaseEntity entity = null;

		try {
			entity = service.findBaseEntityByCode(code);

		} catch (NoResultException e) {
			return Response.status(204).build();
		}

		String json = JsonUtils.toJson(entity);

		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/questions/{id}")
	public Response fetchQuestionById(@PathParam("id") final Long id) {
		final Question entity = service.findQuestionById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/questions")
	public Response fetchQuestions() {
		final List<Question> entitys = service.findQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/attributes")
	public Response fetchAttributes() {
		final List<Attribute> entitys = service.findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		msg.setToken(securityService.getToken());
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();
	}
	
	@GET
	@Path("/attributes/{code}")
	public Response fetchAttribute(@PathParam("attributeCode") final String attributeCode) {
		final Attribute attribute = service.findAttributeByCode(attributeCode);
		Attribute[] atArr = new Attribute[1];
		atArr[0] = attribute;
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		msg.setToken(securityService.getToken());
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();
	}
	


	@GET
	@Path("/questions/{questionCode}")
	public Response fetchQuestions(@PathParam("questionCode") final String questionCode) {
		final List<Question> entitys = service.findQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/rules")
	public Response fetchRules() {
		final List<Rule> entitys = service.findRules();

		log.debug(entitys);
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/asks")
	// @RolesAllowed("admin")
	public Response fetchAsks() {
		final List<Ask> entitys = service.findAsksWithQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/asksmsg")
	public Response fetchAsksMsg() {
		final List<Ask> entitys = service.findAsks();
		final QDataAskMessage qasks = new QDataAskMessage(entitys.toArray(new Ask[0]));
		log.debug(qasks);
		return Response.status(200).entity(qasks).build();
	}

	// TODO: This should be deprecated
	@GET
	@Path("/asksmsg/{questionCode}")
	@Transactional
	public Response fetchAsksMsgByQuestionCode(@PathParam("questionCode") final String questionCode) {
		// work out who the sourceCode and targetCode
		BaseEntity user = service.getUser();
		List<Ask> asks = service.createAsksByQuestionCode(questionCode, user.getCode(), user.getCode());
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
		String json = JsonUtils.toJson(askMsgs);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/attributes/{id}")
	public Response fetchAttributeById(@PathParam("id") final Long id) {
		final Attribute entity = service.findAttributeById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/asks/{id}")
	public Response fetchAskById(@PathParam("id") final Long id) {
		final Ask entity = service.findAskById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/answers/{id}")
	public Response fetchAnswerById(@PathParam("id") final Long id) {
		final Answer entity = service.findAnswerById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/contexts/{id}")
	public Response fetchContextById(@PathParam("id") final Long id) {
		final life.genny.qwanda.Context entity = service.findContextById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/baseentitys/{sourceCode}/attributes")
	@ApiOperation(value = "attributes", notes = "BaseEntity Attributes")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchAttributesByBaseEntityCode(@PathParam("sourceCode") final String code) {
		BaseEntity entity = null;

		try {
			entity = service.findBaseEntityByCode(code, true);

		} catch (NoResultException e) {
			return Response.status(204).build();
		}

		String json = JsonUtils.toJson(entity);

		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/baseentitys/{id}/gps")
	@ApiOperation(value = "gps", notes = "Target BaseEntity GPS")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchGPSByTargetBaseEntityId(@PathParam("id") final Long id) {
		final List<GPS> items = service.findGPSByTargetBaseEntityId(id);
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{code}/asks/source")
	@ApiOperation(value = "asks", notes = "Source BaseEntity Asks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchAsksBySourceBaseEntityCode(@PathParam("code") final String code) {
		final List<Ask> items = service.findAsksBySourceBaseEntityCode(code);
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/baseentitys/{code}/asks/target")
	@ApiOperation(value = "asks", notes = "Target BaseEntity Asks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchAsksByTargetBaseEntityCode(@PathParam("code") final String code) {
		final List<Ask> items = service.findAsksByTargetBaseEntityCode(code);
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{id}/asks/target")
	@ApiOperation(value = "asks", notes = "BaseEntity Asks about Targets")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchAsksByTargetBaseEntityId(@PathParam("id") final Long id) {
		final List<Ask> items = service.findAsksBySourceBaseEntityId(id);
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{targetCode}/answers")
	@ApiOperation(value = "answers", notes = "BaseEntity AnswerLinks")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchAnswersByTargetBaseEntityCode(@PathParam("targetCode") final String targetCode) {
		final List<AnswerLink> items = service.findAnswersByTargetBaseEntityCode(targetCode);
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/answers")
	@ApiOperation(value = "answers", notes = "AnswerLinks")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchAnswerLinks() {
		final List<AnswerLink> items = service.findAnswerLinks();
		String json = JsonUtils.toJson(items);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys")
	@Produces("application/json")
	public Response getBaseEntitys(@Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = false;
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}
		final List<BaseEntity> targets = service.findBaseEntitysByAttributeValues(qparams, includeAttributes, pageStart,
				pageSize);
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}
		String json = JsonUtils.toJson(targets);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}")
	@Produces("application/json")
	public Response getTargets(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode, @Context final UriInfo uriInfo) {
	//	log.info("Entering GET TARGETS /baseentitys/{sourceCode}/linkcodes/{linkCode}");
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = false;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}

		final List<BaseEntity> targets = service.findChildrenByAttributeLink(sourceCode, linkCode, includeAttributes,
				pageStart, pageSize, 1, qparams);

		// remove the attributes
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}

		//log.info("Entering GET TARGETSCOUNT/baseentitys/{sourceCode}/linkcodes/{linkCode}");
		Long total = -1L;

		try {
			total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			total = -1L;
		}

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys2/{sourceCode}/linkcodes/{linkCode}/linkValue/{linkValue}")
	@Produces("application/json")
	public Response getTargetsUsingLinkValue(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode,
			@PathParam("linkValue") final String linkValue, @Context final UriInfo uriInfo) {
		//log.info("Entering GET TARGETS /baseentitys/{sourceCode}/linkcodes/{linkCode}/linkValue/{linkValue}");
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = false;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}

		final List<BaseEntity> targets = service.findChildrenByLinkValue(sourceCode, linkCode, linkValue,
				includeAttributes, pageStart, pageSize, new Integer(1), qparams, null);

		// remove the attributes
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}

		//log.info("Entering GET TARGETSCOUNT/baseentitys/{sourceCode}/linkcodes/{linkCode}");
		Long total = -1L;

		try {
			total = service.findChildrenByLinkValueCount(sourceCode, linkCode, linkValue, qparams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			total = -1L;
		}

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();

	}
	
	@GET
	@Path("/baseentitys2/{sourceCode}/linkcodes/{linkCode}/linkValue/{linkValue}/attributes")
	@Produces("application/json")
	public Response getTargetsUsingLinkValueWithAttributes(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode,
			@PathParam("linkValue") final String linkValue, @Context final UriInfo uriInfo) {
		//log.info("Entering GET TARGETS /baseentitys/{sourceCode}/linkcodes/{linkCode}/linkValue/{linkValue}");
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = true;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}

		final List<BaseEntity> targets = service.findChildrenByLinkValue(sourceCode, linkCode, linkValue,
				includeAttributes, pageStart, pageSize, new Integer(1), qparams, null);

		// remove the attributes
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}

		//log.info("Entering GET TARGETSCOUNT/baseentitys/{sourceCode}/linkcodes/{linkCode}");
		Long total = -1L;

		try {
			total = service.findChildrenByLinkValueCount(sourceCode, linkCode, linkValue, qparams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			total = -1L;
		}

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}/attributes")
	@Produces("application/json")
	public Response getTargetsWithAttributes(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode, @Context final UriInfo uriInfo) {
		String stakeholderCode = null;
//		if (!(securityService.inRole("admin") || securityService.inRole("superadmin")
//				|| securityService.inRole("dev"))) {
//			// stakeholderCode = "PER_" + ((String)
//			// securityService.getUserMap().get("username")).toUpperCase();
//			stakeholderCode = "PER_" + QwandaUtils
//					.getNormalisedUsername((String) securityService.getUserMap().get("username")).toUpperCase();
//		}

		Integer pageStart = 0;
		Integer pageSize = 10; // default
		Integer level = 1;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String levelStr = params.getFirst("level");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (levelStr != null) {
			level = Integer.decode(levelStr);
			// params.remove("level");
		}
		final List<BaseEntity> targets = service.findChildrenByAttributeLink(sourceCode, linkCode, true, pageStart,
				pageSize, level, qparams,stakeholderCode);

		// for (final BaseEntity be : targets) {
		// log.info("\n" + be.getCode() + " + attributes");
		// be.getBaseEntityAttributes().stream().forEach(p ->
		// log.debug(p.getAttributeCode()));
		// }

		Long total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams,stakeholderCode);

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}/attributes/{stakeholderCode}")
	@Produces("application/json")
	public Response getTargetsWithAttributesAndStakeholderCode(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode,
			@DefaultValue("USER") @PathParam("stakeholderCode") String stakeholderCode,
			@Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 500; // default
		Integer level = 1;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String levelStr = params.getFirst("level");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (levelStr != null) {
			level = Integer.decode(levelStr);
			// params.remove("level");
		}

		// Force any user that is not admin to have to use their own code

		if (!(securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev"))) {
			// stakeholderCode = "PER_" + ((String)
			// securityService.getUserMap().get("username")).toUpperCase();
			stakeholderCode = "PER_" + QwandaUtils
					.getNormalisedUsername((String) securityService.getUserMap().get("username")).toUpperCase();
		}

	     SearchEntity searchBE = new SearchEntity("Targets","Targets")
	      	     .addColumn("PRI_IMAGE_URL","Image")
	      	     .addColumn("PRI_USERNAME","Username")
	      	     .addColumn("PRI_FIRSTNAME","Firstname")
	      	     .addColumn("PRI_LASTNAME","Surname")
	      	     .addColumn("PRI_MOBILE","Mobile")
	      	     .addColumn("PRI_EMAIL","Email")
	      	     .addColumn("PRI_REGISTRATION_DATETIME", "Registered Date")
	      	     .addColumn("PRI_LAST_LOGIN_DATETIME","Last Login Date")
	      	     
	      	     .addSort("PRI_FIRSTNAME","Firstname",SearchEntity.Sort.ASC)
	      	     .addFilter("PRI_CODE",SearchEntity.StringFilter.LIKE,"PER_%")
	      	     
	      	     .setPageStart(pageStart)
	      	     .setPageSize(pageSize);
	     
	     if (stakeholderCode!=null) {
	    	 searchBE.setStakeholder(stakeholderCode);
	     }
	     
	//     List<BaseEntity> targets = service.findBySearchBE(searchBE);
	     

		final List<BaseEntity> targets = service.findChildrenByAttributeLink(sourceCode, linkCode, true, pageStart,
				pageSize, level, qparams, stakeholderCode);

//		for (final BaseEntity be : targets) {
//			log.info("\n" + be.getCode() + " + attributes");
//			be.getBaseEntityAttributes().stream().forEach(p -> log.debug(p.getAttributeCode()));
//		}

		Long total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams); // TODO add stakeholder
																								// filtering

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		String json = JsonUtils.toJson(msg);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/baseentitys/test2")
	@Produces("application/json")
	public Response findBaseEntitysByAttributeValues(@Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}

		final List<BaseEntity> targets = service.findBaseEntitysByAttributeValues(qparams, true, pageStart, pageSize);

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		final Long total = service.findBaseEntitysByAttributeValuesCount(params);

		final QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, "", "", total);

		String json = JsonUtils.toJson(msg);
		// log.debug("BE:" + json);
		return Response.status(200).entity(json).build();

		// return Response.status(200).entity(msg).build();

	}

	@GET
	@Path("/baseentitys/importkeycloak")
	@Produces("application/json")
	@Transactional
	// user MUST BE SUPERADMIN
	public Response importKeycloakUsers(@QueryParam("keycloakurl") final String keycloakUrl,
			@QueryParam("realm") final String realm, @QueryParam("username") final String username,
			@QueryParam("password") final String password, @QueryParam("clientid") final String clientId,
			@QueryParam("max") final Integer max,
			@DefaultValue("GRP_USERS") @QueryParam("parentgroups") final String parentGroupCodes) {
		log.error("IMPORT KEYCLOAK DISABLED IN CODE");
		Long usersAddedCount = 0L; // service.importKeycloakUsers(keycloakUrl,
									// realm, username,
									// password, clientId,
									// max, parentGroupCodes);
		return Response.status(200).entity(usersAddedCount).build();
	}

	@POST
	@Path("/baseentitys/uploadcsv")
	@Consumes("multipart/form-data")
	@Transactional
	public Response uploadFile(final MultipartFormDataInput input) throws IOException {

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
				service.importBaseEntitys(inputStream, fileName);

				return Response.status(200).entity("Imported file name : " + fileName).build();

			} catch (final Exception e) {
				e.printStackTrace();
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


	
	@PUT
	@Consumes("application/json")
	@Path("/links")
	@Produces("application/json")

	public Response updateLink(final Link link) {

		Log.info("Updating Link " + link.getSourceCode() + ":" + link.getTargetCode() + ":" + link.getAttributeCode()
				+ ":" + link.getLinkValue() + ":" + link.getWeight());

		Integer result = null;
		
		try {
			result = service.updateEntityEntity(link);
		} catch (NoResultException e) {
			return Response.status(400).entity("Link data does not exist").build();
		}

		return Response.status(200).entity(result).build();
	}

	@PUT
	@Consumes("application/json")
	@Path("/entityentitys")
	@Produces("application/json")

	public Response updateEntityEntity(final Link link) {

		Log.info("Updating Link " + link.getSourceCode() + ":" + link.getTargetCode() + ":" + link.getAttributeCode()
				+ ":" + link.getLinkValue() + ":" + link.getWeight() + ":" + link.getParentColor() + ":"
				+ link.getChildColor() + ":" + link.getRule());

		Integer result = null;
		
		try {
			result = service.updateEntityEntity(link);
		} catch (NoResultException e) {
			return Response.status(400).entity("Link data does not exist").build();
		}
		return Response.status(200).entity(result).build();
	}

	@PUT
	@Consumes("application/json")
	@Path("/baseentitys")
	@Produces("application/json")

	public Response updateBaseEntity(final BaseEntity baseEntity) {

		Log.info("Updating  baseEntity " + baseEntity.getCode() + ":" + baseEntity.getName());
		BaseEntity be = service.findBaseEntityByCode(baseEntity.getCode());
		be.setName(baseEntity.getName());
		be.setBaseEntityAttributes(baseEntity.getBaseEntityAttributes());
		Long result = service.update(be);

		return Response.status(200).entity(result).build();
	}
	
	@PUT
	@Consumes("application/json")
	@Path("/baseentitys/force")
	@Produces("application/json")

	public Response forceBaseEntityAttributes(final BaseEntity baseEntity) {
		Map<String,Object> map = securityService.getUserMap();
		String username = (String) securityService.getUserMap().get("username");
		String userCode = "PER_"+QwandaUtils.getNormalisedUsername(username);
		
		if ((securityService.inRole("admin") || securityService.inRole("superadmin")
				|| (userCode.equalsIgnoreCase(baseEntity.getCode()))) ) {  // TODO Remove the true!

		Log.info("forcing baseEntity attributes" + baseEntity.getCode() + ":" + baseEntity.getName());
		BaseEntity be = service.findBaseEntityByCode(baseEntity.getCode());
		be.setName(baseEntity.getName());
		// now remove the attributes that are not left over
		for (EntityAttribute ea : be.getBaseEntityAttributes()) {  // go through wanted survivors
			Optional<EntityAttribute> optEA = baseEntity.findEntityAttribute(ea.getAttributeCode());
			if (!optEA.isPresent()) {
				service.removeEntityAttribute(ea);
				log.info("Removed attribute "+ ea.getAttributeCode()+" for be "+baseEntity.getCode());
			}
		}
		// get updated one
		be = service.findBaseEntityByCode(baseEntity.getCode());
		service.writeToDDT(be);
		QEventAttributeValueChangeMessage msg = new QEventAttributeValueChangeMessage(be.getCode(),be.getCode(),be,securityService.getToken());
		service.sendQEventAttributeValueChangeMessage(msg);
		return Response.status(200).build();
		} else {
			return Response.status(401).build();
		}
	}
	
	@DELETE
	@Consumes("application/json")
	@Path("/baseentitys/{baseEntityCode}/{attributeCode}")
	@Produces("application/json")

	public Response removeEntityAttribute(@PathParam("baseEntityCode") final String baseEntityCode,@PathParam("attributeCode") final String attributeCode) {
		
		String username = (String) securityService.getUserMap().get("preferred_username");
		String userCode = QwandaUtils.getNormalisedUsername(username);
		
		if (!(securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev")) || (userCode.equalsIgnoreCase(baseEntityCode))) {  // TODO Remove the true!

		service.removeEntityAttribute(baseEntityCode, attributeCode);
		return Response.status(200).build();
		}

		return Response.status(401).entity("Unauthorised").build();
	}
	
	@DELETE
	@Consumes("application/json")
	@Path("/baseentitys/attributes/{attributeCode}")
	@Produces("application/json")

	public Response removeEntityAttributes(@PathParam("attributeCode") final String attributeCode) {
		
		String username = (String) securityService.getUserMap().get("preferred_username");
		String userCode = QwandaUtils.getNormalisedUsername(username);
		
		if (!(securityService.inRole("admin") || securityService.inRole("superadmin")
				|| securityService.inRole("dev"))) {  // TODO Remove the true!

		service.removeEntityAttributes(attributeCode);
		return Response.status(200).build();
		}

		return Response.status(401).entity("Unauthorised").build();
	}

	@POST
	@Consumes("application/json")
	@Path("/entityentitys")
	@Produces("application/json")

	public Response addLink(final Link ee) {

		Log.info("Creating new Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode()
				+ ":" + ee.getLinkValue() + ":" + ee.getWeight());

		EntityEntity newEntityEntity = null;

		try {
			newEntityEntity = service.addLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode(),
					ee.getLinkValue(), ee.getWeight());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(newEntityEntity)).build())
				.build();
	}

	@DELETE
	@Consumes("application/json")
	@Path("/entityentitys")
	@Produces("application/json")

	public Response removeLink(final Link ee) {

		Log.info("Removing Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode());

		service.removeLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode());
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}



	
	@POST
	@Consumes("application/json")
	@Path("/baseentitys/move/{targetCode}")
	@Produces("application/json")
	@Transactional
	public Response moveLink(@PathParam("targetCode") final String targetCode, final Link ee) {

		Log.info("moving Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode()
				+ " to new Parent " + targetCode);
		Link newEntityEntity = service.moveLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode(),
				targetCode);
		if (newEntityEntity != null) {
			// TODO: This is a terrible hack.but logically will work
			newEntityEntity.setAttributeCode(ee.getAttributeCode());
			newEntityEntity.setSourceCode(targetCode);
			newEntityEntity.setTargetCode(ee.getTargetCode());
			if (ee.getLinkValue() != null) {
				newEntityEntity.setLinkValue(ee.getLinkValue());
				try {
					service.updateLink(newEntityEntity);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadDataException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(newEntityEntity)).build())
				.build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}")
	@ApiOperation(value = "baseentitys/{targetCode}/links", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchLinks(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final List<Link> items = service.findLinks(targetCode, linkCode);
		Link[] array = items.toArray(new Link[0]);
		String json = JsonUtils.toJson(array);
		return Response.status(200).entity(json).build();

	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}/count")
	@ApiOperation(value = "baseentitys/{targetCode}/links/count", notes = "Links count")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchLinksCount(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final Long count = service.findLinksCount(targetCode, linkCode);

		return Response.status(200).entity(count).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}/children/count")
	@ApiOperation(value = "baseentitys/{targetCode}/links/children/count", notes = "Links children count")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchChildLinksCount(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final Long count = service.findChildLinksCount(targetCode, linkCode);

		return Response.status(200).entity(count).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}/parent/count")
	@ApiOperation(value = "baseentitys/{targetCode}/links/parent/count", notes = "Links parent count")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response findParentLinksCount(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final Long count = service.findParentLinksCount(targetCode, linkCode);

		return Response.status(200).entity(count).build();
	}

	@GET
	@Path("/entityentitys/{sourceCode}/linkcodes/{linkCode}/children")
	@ApiOperation(value = "baseentitys/{sourceCode}/linkcodes/{linkCode}/children", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchChildLinks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("linkCode") final String linkCode) {
		final List<Link> items = service.findChildLinks(sourceCode, linkCode);
		Link[] array = items.toArray(new Link[0]);
		String json = JsonUtils.toJson(array);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/entityentitys/{sourceCode}/linkcodes/{linkCode}/children/{linkValue}")
	@ApiOperation(value = "baseentitys/{sourceCode}/linkcodes/{linkCode}/children/{linkValue}", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchChildLinks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("linkCode") final String linkCode, @PathParam("linkValue") final String linkValue) {
		final List<Link> items = service.findChildLinks(sourceCode, linkCode, linkValue);

		Link[] array = items.toArray(new Link[0]);
		String json = JsonUtils.toJson(array);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}/parents")
	@ApiOperation(value = "baseentitys/{targetCode}/linkcodes/{linkCode}/parents", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchParentLinks(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final List<Link> items = service.findParentLinks(targetCode, linkCode);
		Link[] array = items.toArray(new Link[0]);
		String json = JsonUtils.toJson(array);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}/parents/{linkValue}")
	@ApiOperation(value = "baseentitys/{targetCode}/linkcodes/{linkCode}/parents/{linkValue}", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchParentLinks(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode, @PathParam("linkValue") final String linkValue) {
		final List<Link> items = service.findParentLinks(targetCode, linkCode, linkValue);
		Link[] array = items.toArray(new Link[0]);
		String json = JsonUtils.toJson(array);
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/realms/sync")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response syncrealms() {
		log.debug("Sync Keycloak Realms");
		String keycloakRealms = SecureResources.reload();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@GET
	@Path("/realms")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchrealms() {
		log.debug("Fetch Keycloak Realms");
		String keycloakRealms = SecureResources.fetchRealms();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/realms")
	@Transactional
	public Response createRealm(final String entity) {
		log.debug("Add Keycloak Realm");

		JSONObject json = new JSONObject(entity);
		String key = json.getString("clientId");
		key = key + ".json";

		SecureResources.addRealm(key, entity);

		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	@DELETE
	@Consumes("application/json")
	@Path("/realms")
	@Produces("application/json")
	@Transactional
	public Response removeRealm(final String key) {

		Log.info("Removing Realm " + key);

		SecureResources.removeRealm(key);
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	Controller ctl = new Controller();

	@GET
	@Path("/synchronize/{sheetId}/{tables}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response synchronizeSheets2DB(@PathParam("sheetId") final String sheetId,
			@PathParam("tables") final String tables) {
		String response = "Success";
		ctl.getProject(service, sheetId, tables);
		return Response.status(200).entity(response).build();
	}

	@GET
	@Path("/templates/{templateCode}")
	@Produces("application/json")
	public Response getMessageTemplates(@PathParam("templateCode") final String code) {
		QBaseMSGMessageTemplate template = service.findTemplateByCode(code);

		String json = JsonUtils.toJson(template);
		return Response.status(200).entity(json).build();
	}



}
