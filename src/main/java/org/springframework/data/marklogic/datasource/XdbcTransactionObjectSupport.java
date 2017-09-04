package org.springframework.data.marklogic.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * Convenient base class for XDBC-aware transaction objects.
 * Can contain a {@link SessionHolder}, and implements the
 * {@link org.springframework.transaction.SavepointManager}
 * interface based on that SessionHolder.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public abstract class XdbcTransactionObjectSupport implements SmartTransactionObject {

	private static final Log logger = LogFactory.getLog(XdbcTransactionObjectSupport.class);


	private SessionHolder sessionHolder;

	private Integer previousIsolationLevel;

	public void setSessionHolder(SessionHolder sessionHolder) {
		this.sessionHolder = sessionHolder;
	}

	public SessionHolder getSessionHolder() {
		return this.sessionHolder;
	}

	public boolean hasSessionHolder() {
		return (this.sessionHolder != null);
	}

	public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
		this.previousIsolationLevel = previousIsolationLevel;
	}

	public Integer getPreviousIsolationLevel() {
		return this.previousIsolationLevel;
	}

	@Override
	public void flush() {
		// no-op
	}

}