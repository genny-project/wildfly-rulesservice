package life.genny.qwanda.jbpm;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.kie.api.io.ResourceType;

import life.genny.qwanda.service.RulesService;


@Singleton
@Startup
public class RulesServiceBean {

	@Inject
	RulesService rulesService;

	@PostConstruct
	public void initialize() {
	}

	@PreDestroy
	public void terminate() {
	}

	public Map<File, ResourceType> getKieResources() {
		return rulesService.getKieResources();		
	}


}