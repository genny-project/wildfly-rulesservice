package life.genny.qwanda.jbpm;

import java.io.File;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.SessionContext;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.drools.core.impl.EnvironmentFactory;
import org.drools.persistence.jta.JtaTransactionManager;
import org.jbpm.persistence.JpaProcessPersistenceContextManager;
import org.jbpm.process.audit.AuditLoggerFactory;
import org.jbpm.services.cdi.impl.manager.InjectableRegisterableItemsFactory;
import org.jbpm.services.task.HumanTaskConfigurator;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.services.task.audit.JPATaskLifeCycleEventListener;
import org.jbpm.services.task.identity.DefaultUserInfo;
import org.jbpm.services.task.impl.command.CommandBasedTaskService;
import org.jbpm.services.task.lifecycle.listeners.TaskLifeCycleEventListener;
import org.jbpm.services.task.persistence.JPATaskPersistenceContextManager;
import org.jbpm.services.task.wih.ExternalTaskEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.cdi.qualifier.PerProcessInstance;
import org.kie.internal.runtime.manager.cdi.qualifier.PerRequest;
import org.kie.internal.runtime.manager.cdi.qualifier.Singleton;



@ApplicationScoped
@DependsOn({ "RulesServiceBean" })
public class Producer {

	@Inject
	RulesServiceBean rulesServiceBean;

	@Inject
	private BeanManager beanManager;

	@Resource
	private SessionContext ctx;

	private TaskService taskService;
	
	@Inject
	private UserGroupCallback usergroupCallback;

	@PersistenceUnit(unitName = "org.jbpm.domain")
	private EntityManagerFactory emf;

	@Produces
	public EntityManagerFactory produceEntityManagerFactory() {
		if (this.emf == null) {
			this.emf = Persistence.createEntityManagerFactory("org.jbpm.domain");
		}
		return this.emf;
	}

	@Produces
	public UserGroupCallback produceSelectedUserGroupCalback() {
		return usergroupCallback;
	}

	@Produces
	@Singleton
	@PerProcessInstance
	@PerRequest
	public RuntimeEnvironment produceEnvironment(EntityManagerFactory emf) {
		System.out.println("*********************Building RuntimeEnvironment***************************");

		Environment env = EnvironmentFactory.newEnvironment();
		TransactionManager utm = null;
		try {
			InitialContext ic = new InitialContext();
			utm = (TransactionManager) ic.lookup("java:/TransactionManager");
		} catch (NamingException e) {
			e.printStackTrace();
		}
		UserTransaction userTransaction = ctx.getUserTransaction();
		JtaTransactionManager JtaTransactionManager =
				new JtaTransactionManager(userTransaction,null,utm);
		
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		env.set(EnvironmentName.TRANSACTION_MANAGER, JtaTransactionManager);
		env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER, new JpaProcessPersistenceContextManager(env));
		env.set(EnvironmentName.TASK_PERSISTENCE_CONTEXT_MANAGER, new JPATaskPersistenceContextManager(env));
		env.set("IS_JTA_TRANSACTION", true);
		env.set("IS_SHARED_ENTITY_MANAGER", true);
		
		RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder()
				.entityManagerFactory(emf).userGroupCallback(usergroupCallback)
				.registerableItemsFactory(
						InjectableRegisterableItemsFactory.getFactory(beanManager, AuditLoggerFactory.newJPAInstance()))
		.addEnvironmentEntry(EnvironmentName.TRANSACTION_MANAGER, JtaTransactionManager)
		.addEnvironmentEntry(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER,
				new JpaProcessPersistenceContextManager(env))
		.addEnvironmentEntry(EnvironmentName.TASK_PERSISTENCE_CONTEXT_MANAGER,
				new JPATaskPersistenceContextManager(env));
		

		for (Map.Entry<File, ResourceType> entry : rulesServiceBean.getKieResources().entrySet()) {
			builder.addAsset(ResourceFactory.newFileResource(entry.getKey()), entry.getValue());
		}
		RuntimeEnvironment environment = builder.get();
		return environment;
	}
	
	@Named("Logs")
	@Produces
	public TaskLifeCycleEventListener produceTaskAuditListener() {  
		return new JPATaskLifeCycleEventListener(true); 
	}
	


}
