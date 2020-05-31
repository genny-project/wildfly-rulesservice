/**
 * 
 */
package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventMessage;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.TableUtils;

/**
 * @author acrow
 *
 */

@ApplicationScoped

public class TableService {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	public void init() {

	}

	public QDataBaseEntityMessage getData(QEventMessage message, BaseEntityUtils beUtils) {
		QDataBaseEntityMessage ret = null; //new QDataBaseEntityMessage();
		log.info("********** RUNNING TABLE SPEEDUP HACK **********");
		Boolean cache = false;
		String code = message.getData().getCode();
		System.out.println("QUESTION CODE   ::   " + code);

		code = StringUtils.removeStart(code, "QUE_TREE_ITEM_");
		System.out.println("removing prefix CODE   ::   " + code);

		code = StringUtils.removeEnd(code, "_GRP");


		long totalTime = TableUtils.searchTable(beUtils,code, cache);

		
		return ret;
	}
	
	@PreDestroy
	public void shutdown() {
	
	}

	public void info()

	{
		log.info("Table Service info");
	}
}
