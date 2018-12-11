package life.genny.qwanda.jbpm;

/**
 * WorkflowEntity represents an entity that contains a workflow. 
 * 
 * 
 * @author Adam Crow
 * @version %I%, %G%
 * @since 1.0
 */

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

import javax.persistence.Cacheable;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.google.gson.annotations.Expose;

import life.genny.qwanda.CodedEntity;
import com.cdi.crud.infra.model.BaseEntityInterface;



@XmlRootElement
@XmlAccessorType(value = XmlAccessType.FIELD)

@Table(name = "workflow", 
indexes = {
        @Index(columnList = "code", name =  "code_idx"),
        @Index(columnList = "realm", name = "code_idx")
    })
@Entity
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)


@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class WorkflowEntity extends CodedEntity implements Serializable, Comparable<Object>, BaseEntityInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	private static final String DEFAULT_CODE_PREFIX = "WFL_";


		
	@Version
	private int version;
	
	@Expose
	private long processInstanceId;
	
	@Expose
	private String processId;
	
	@Expose
	private Boolean active;
	

	/**
	 * Constructor.
	 * 
	 * @param none
	 */
	@SuppressWarnings("unused")
	protected WorkflowEntity() {
		super();
	}

	 /**
	   * Constructor.
	   * 
	   * @param Name the summary name of the core entity
	   */
	  public WorkflowEntity(final String aName) {
	    super(getDefaultCodePrefix() + UUID.randomUUID().toString(), aName);

	  }

	  /**
	   * Constructor.
	   * 
	   * @param Code the unique code of the core entity
	   * @param Name the summary name of the core entity
	   */
	  public WorkflowEntity(final String aCode, final String aName) {
	    super(aCode, aName);

	  }


	/**
	 * @return the version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	
	
	/**
	 * @return the defaultCodePrefix
	 */
	public static String getDefaultCodePrefix() {
		return DEFAULT_CODE_PREFIX;
	}
	
	

	/**
	 * @return the processInstanceId
	 */
	public long getProcessInstanceId() {
		return processInstanceId;
	}

	/**
	 * @param processInstanceId the processInstanceId to set
	 */
	public void setProcessInstanceId(long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	/**
	 * @return the processId
	 */
	public String getProcessId() {
		return processId;
	}

	/**
	 * @param processId the processId to set
	 */
	public void setProcessId(String processId) {
		this.processId = processId;
	}

	/**
	 * @return the active
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) {
			return 0;
		}
		if (!(o instanceof WorkflowEntity)) {
			WorkflowEntity wfe = (WorkflowEntity)o;
			return this.getId().compareTo(wfe.getId());
		} else 
		{
			return 0;
		}
	}
	
    public Long getId() {
        return super.getId();
    }

}
