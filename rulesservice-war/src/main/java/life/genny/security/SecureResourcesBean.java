package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.apache.logging.log4j.Logger;

import life.genny.qwandautils.GennySettings;
import life.genny.security.SecureResources;
import javax.annotation.PostConstruct;


@ApplicationScoped
public class SecureResourcesBean {
	
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public Map<String,String> getKeycloakJsonMap()
	{
		return SecureResources.getKeycloakJsonMap();
	}

	@PostConstruct
	public void init() {
		log.info("Initialising SecureReresourcxes in wildfly-rulesservice");
		readFilenamesFromDirectory(GennySettings.realmDir);
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object init) {
		SecureResources.clear();
	}

	public static void addRealm(final String key, String keycloakJsonText) {

		keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);
		log.info("Adding keycloak key:" + key + "," + keycloakJsonText);

		SecureResources.getKeycloakJsonMap().put(key, keycloakJsonText);
	}

	public static void removeRealm(final String key) {
		log.info("Removing keycloak key:" + key);

		SecureResources.getKeycloakJsonMap().remove(key);
	}

	public static String reload() {
		SecureResources.getKeycloakJsonMap().clear();
		return readFilenamesFromDirectory(GennySettings.realmDir);
	}

	public static String fetchRealms() {
		String ret = "";
		for (String keycloakRealmKey : SecureResources.getKeycloakJsonMap().keySet()) {
			ret += keycloakRealmKey + ":" + SecureResources.getKeycloakJsonMap().get(keycloakRealmKey) + "\n";
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

					// if (!"localhost.json".equalsIgnoreCase(listOfFiles[i].getName())) {
					keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);

					// }
					final String key = listOfFiles[i].getName(); // .replaceAll(".json", "");
					log.info("keycloak key:" + key + "," + keycloakJsonText);

					SecureResources.getKeycloakJsonMap().put(key, keycloakJsonText);
					SecureResources.getKeycloakJsonMap().put(key+".json", keycloakJsonText);
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
}
