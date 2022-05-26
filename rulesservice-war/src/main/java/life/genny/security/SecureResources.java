package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.utils.VertxUtils;

@ApplicationScoped
public class SecureResources {
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	/**
	 * @return the keycloakJsonMap
	 */
	public static Map<String, String> getKeycloakJsonMap() {

		return keycloakJsonMap;
	}

	private static Map<String, String> keycloakJsonMap = new ConcurrentHashMap<String, String>();

	public void init(@Observes @Initialized(ApplicationScoped.class) final Object init) {
		if (keycloakJsonMap.isEmpty()) {
			setup();
		}

	}
	
	public void setup()
	{
		
		Set<String> realmsSet = VertxUtils.fetchRealmsFromApi();

		// Now fetch all the Realms for keycloak json
		for (String realm : realmsSet) {
			log.info("Loaded realm is "+realm);
			getKeycloakJson("http://"+realm + ".genny.life");
			getKeycloakJson(GennySettings.projectUrl);
		}
	}

	public static String getKeycloakJson(String fullurl) {
		fullurl = StringUtils.removeEnd(fullurl, ".json");
		log.info("Getting KeycloakJson from SecureResource :"+fullurl);
		String keycloakJson = SecureResources.getKeycloakJsonMap().get(fullurl+".json");
		if ( keycloakJson != null) {
			return keycloakJson;
		}
		
		URL aURL = null;
		try {
			if (!fullurl.startsWith("http")) {
				fullurl = "http://"+fullurl;
				fullurl = StringUtils.removeEnd(fullurl, ".json");
			}
			aURL = new URL(fullurl);
			final String url = aURL.getHost();
			log.info("pure host url is "+url);
			JsonObject retInit = null;
			String token = null;
			// Fetch Project BE
			JsonObject jsonObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM, url.toUpperCase());
			BaseEntity projectBe = null;
			if ((jsonObj == null) || ("error".equals(jsonObj.getString("status")))) {
				log.error(url.toUpperCase() + " not found in cache");

			} else {
				String value = jsonObj.getJsonObject("value").toString();
				projectBe = JsonUtils.fromJson(value.toString(), BaseEntity.class);
				JsonObject tokenObj = VertxUtils.readCachedJson(GennySettings.GENNY_REALM,
						"TOKEN" + url.toUpperCase());
				token = tokenObj.getJsonObject("value").toString();

				log.info(projectBe.getRealm());
			}

			if ((projectBe != null) ) {
				retInit = new JsonObject(projectBe.getValue("ENV_KEYCLOAK_JSON", "{}"));
			//	log.info("KEYCLOAK JSON VALUE: " + retInit);
				String realm = projectBe.getRealm();
				keycloakJsonMap.put(realm,retInit.toString());
				keycloakJsonMap.put(url,retInit.toString());
				keycloakJsonMap.put(realm+".json",retInit.toString());
				keycloakJsonMap.put(url+".json",retInit.toString());
				return retInit.toString();
			}
		} catch (Exception e)
		{
					log.error("KeycloakJson not available for "+fullurl, e);
					e.printStackTrace();
		}
		return null;
				
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object init) {
		keycloakJsonMap.clear();
	}

	public static void addRealm(final String key, String keycloakJsonText) {

		keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);
		log.info("Adding keycloak key:" + key + "," + keycloakJsonText);

		keycloakJsonMap.put(key, keycloakJsonText);
	}

	public static void removeRealm(final String key) {
		log.info("Removing keycloak key:" + key);

		keycloakJsonMap.remove(key);
	}

	public static String reload() {
		keycloakJsonMap.clear();
		return readFilenamesFromDirectory(GennySettings.realmDir);
	}

	public static String fetchRealms() {
		String ret = "";
		for (String keycloakRealmKey : keycloakJsonMap.keySet()) {
			ret += keycloakRealmKey + ":" + keycloakJsonMap.get(keycloakRealmKey) + "\n";
		}
		return ret;
	}

	public static String readFilenamesFromDirectory(final String rootFilePath) {
		String ret = "";
		final File folder = new File(rootFilePath);
		final File[] listOfFiles = folder.listFiles();

		log.info("Loading Files! with HOSTIP=" + GennySettings.hostIP);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				log.info("Importing Keycloak Realm File " + listOfFiles[i].getName());
				try {
					String keycloakJsonText = getFileAsText(listOfFiles[i]);
					// Handle case where dev is in place with localhost

					if ("localhost.json".equalsIgnoreCase(listOfFiles[i].getName())) {
						keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);
						keycloakJsonMap.put(GennySettings.mainrealm + ".json", keycloakJsonText);
					}
					keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);

					// }
					final String key = listOfFiles[i].getName(); // .replaceAll(".json", "");
					log.info("keycloak key:" + key + "," + keycloakJsonText);

					keycloakJsonMap.put(key, keycloakJsonText);
					if (!StringUtils.endsWith(key, ".json")) {
						keycloakJsonMap.put(key + ".json", keycloakJsonText);
					}
					ret += keycloakJsonText + "\n";
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (listOfFiles[i].isDirectory()) {
				log.info("Directory " + listOfFiles[i].getName());
				readFilenamesFromDirectory(listOfFiles[i].getName());
			}
		}
		return ret;
	}

	private static String getFileAsText(final File file) throws IOException {
		final BufferedReader in = new BufferedReader(new FileReader(file));
		String ret = "";
		String line = null;
		while ((line = in.readLine()) != null) {
			ret += line;
		}
		in.close();

		return ret;
	}
	
	public void info()
	{
		log.info("Secure Resources Loading");
	}	
}
