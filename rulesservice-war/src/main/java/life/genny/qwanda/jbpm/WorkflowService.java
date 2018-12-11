package life.genny.qwanda.jbpm;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import javax.persistence.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cdi.crud.infra.CrudService;
import com.cdi.crud.infra.exception.CustomException;
import com.cdi.crud.infra.model.Filter;


@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class WorkflowService  extends  CrudService<WorkflowEntity> {

	private static final long serialVersionUID = 1L;
	private final ReentrantLock lock = new ReentrantLock();

	@Resource
	private SessionContext ctx;

	public WorkflowEntity findProcessByCode(String code, String realm) {
		UserTransaction ut = ctx.getUserTransaction();
		List<WorkflowEntity> workflowEntitys = null;
		try {
			ut.begin();
//				String sql = "select WorkflowEntity we where we.code ="+code+" and we.realm ="+realm+" and we.active=true";
//	           Query<WorkflowEntity> query = ctx.getSession().createQuery(sql);
	           	           
	           // Execute query.
	          workflowEntitys = crud().eq("code", code).eq("realm", realm).eq("active", true).list();
			ut.commit();
		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		}
		if (workflowEntitys != null && workflowEntitys.size() > 0) {
			return workflowEntitys.get(0);
		}
		return null;
	}

	public WorkflowEntity findProcessById(Long processId, String realm) {
		UserTransaction ut = ctx.getUserTransaction();
		List<WorkflowEntity> workflowEntitys = null;
		try {
			ut.begin();
			String sql = "select WorkflowEntity we where we.processId ="+processId+" and we.realm ="+realm+" and we.active=true";
	//           Query<WorkflowEntity> query = ctx.getSession().createQuery(sql);
	           	           
	           // Execute query.
//	          workflowEntitys = query.list();
			ut.commit();
		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		}
		if (workflowEntitys != null && workflowEntitys.size() > 0) {
			return workflowEntitys.get(0);
		}
		return null;
	}

	public void insert(WorkflowEntity entity) {
		if (entity == null) {
			throw new CustomException("Entity cannot be null");
		}

		if (entity.getId() != null) {
			throw new CustomException("Entity must be transient");
		}
		beforeInsert(entity);
		UserTransaction ut = ctx.getUserTransaction();
		try {
			ut.begin();
	//          ctx.getSession().save(entity);

			ut.commit();
		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		}
		afterInsert(entity);
	}

	public void remove(WorkflowEntity entity) {
		if (entity == null) {
			throw new CustomException("Entity cannot be null");
		}

		if (entity.getId() == null) {
			throw new CustomException("Entity cannot be transient");
		}
		UserTransaction ut = ctx.getUserTransaction();
		try {
			ut.begin();
//			ctx.getSession().delete(entity);
			ut.commit();
		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void update(WorkflowEntity entity) {
		UserTransaction ut = ctx.getUserTransaction();
		if (entity == null) {
			throw new CustomException("Entity cannot be null");
		}

		if (entity.getId() == null) {
			throw new CustomException("Entity cannot be transient");
		}
		try {
			ut.begin();
	//		ctx.getSession().update(entity);
			ut.commit();

		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		}
	}
	

	
	public void update(Long processInstanceId) {
		UserTransaction ut = ctx.getUserTransaction();
		lock.lock();
		try {
			ut.begin();
			List<WorkflowEntity> workflowEntitys = crud().eq("processId", processInstanceId).eq("active", true).list();
			WorkflowEntity workflowEntity = null;
			if (workflowEntitys != null && workflowEntitys.size() > 0) {
				workflowEntity = workflowEntitys.get(0);
				// make changes
				
		//		ctx.getSession().update(workflowEntity);
			}
			ut.commit();

		} catch (SecurityException | IllegalStateException | RollbackException | HeuristicMixedException
				| HeuristicRollbackException | NotSupportedException | SystemException e) {
			e.printStackTrace();
			try {
				if (ut.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					ut.rollback();
				}
			} catch (IllegalStateException | SecurityException | SystemException e1) {
				e1.printStackTrace();
			}
		} finally {
			  lock.unlock();
		}
	}

}