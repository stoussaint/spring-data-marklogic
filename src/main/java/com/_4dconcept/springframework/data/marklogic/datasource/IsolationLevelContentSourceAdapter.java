package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.Session;
import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * An adapter for a target {@link com.marklogic.xcc.ContentSource}, applying the current
 * Spring transaction's isolation level (and potentially specified user credentials)
 * to every {@code newSession} call. Also applies the read-only flag,
 * if specified.
 *
 * <p>Can be used to proxy a target JNDI ContentSource that does not have the
 * desired isolation level (and user credentials) configured. Client code
 * can work with this ContentSource as usual, not worrying about such settings.
 *
 * <p>Inherits the capability to apply specific user credentials from its superclass
 * {@link UserCredentialsContentSourceAdapter}; see the latter's javadoc for details
 * on that functionality (e.g. {@link #setCredentialsForCurrentThread}).
 *
 * <p><b>WARNING:</b> This adapter simply calls
 * {@link Session#setTransactionMode} for every Session obtained from it.
 * It does, however, <i>not</i> reset those settings; it rather expects the target
 * ContentSource to perform such resetting as part of its session pool handling.
 * <b>Make sure that the target ContentSource properly cleans up such transaction state.</b>
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see #setIsolationLevel
 * @see #setIsolationLevelName
 * @see #setUsername
 * @see #setPassword
 */
public class IsolationLevelContentSourceAdapter extends UserCredentialsContentSourceAdapter {

	/** Constants instance for TransactionDefinition */
	private static final Constants constants = new Constants(TransactionDefinition.class);

	private Integer isolationLevel;


	/**
	 * Set the default isolation level by the name of the corresponding constant
	 * in {@link TransactionDefinition}, e.g.
	 * "ISOLATION_SERIALIZABLE".
	 * <p>If not specified, the target ContentSource's default will be used.
	 * Note that a transaction-specific isolation value will always override
	 * any isolation setting specified at the ContentSource level.
	 * @param constantName name of the constant
	 * @see TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see #setIsolationLevel
	 */
	public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
		if (constantName == null || !constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
			throw new IllegalArgumentException("Only isolation constants allowed");
		}
		setIsolationLevel(constants.asNumber(constantName).intValue());
	}

	/**
	 * Specify the default isolation level to use for Session retrieval,
	 * according to the XDBC {@link Session} constants
	 * (equivalent to the corresponding Spring
	 * {@link TransactionDefinition} constants).
	 * <p>If not specified, the target ContentSource's default will be used.
	 * Note that a transaction-specific isolation value will always override
	 * any isolation setting specified at the ContentSource level.
	 * @see Session.TransactionMode#UPDATE
	 * @see Session.TransactionMode#QUERY
	 * @see TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see TransactionDefinition#getIsolationLevel()
	 * @see TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 */
	public void setIsolationLevel(int isolationLevel) {
		if (!constants.getValues(DefaultTransactionDefinition.PREFIX_ISOLATION).contains(isolationLevel)) {
			throw new IllegalArgumentException("Only values of isolation constants allowed");
		}
		this.isolationLevel = (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevel : null);
	}

	/**
	 * Return the statically specified isolation level,
	 * or {@code null} if none.
	 */
	protected Integer getIsolationLevel() {
		return this.isolationLevel;
	}


	/**
	 * Applies the current isolation level value and read-only flag
	 * to the returned Session.
	 * @see #getCurrentIsolationLevel()
	 * @see #getCurrentReadOnlyFlag()
	 */
	@Override
	protected Session doGetSession(String username, String password) {
		Session ses = super.doGetSession(username, password);
		Boolean readOnlyToUse = getCurrentReadOnlyFlag();
		if (readOnlyToUse != null) {
			if (readOnlyToUse) {
				ses.setTransactionMode(Session.TransactionMode.QUERY);
			} else {
				ses.setTransactionMode(Session.TransactionMode.UPDATE);
			}
		}
		Integer isolationLevelToUse = getCurrentIsolationLevel();
		if (isolationLevelToUse != null) {
			ses.setTransactionMode(determineTransactionMode(isolationLevelToUse));
		}
		return ses;
	}

	/**
	 * Determine the current isolation level: either the transaction's
	 * isolation level or a statically defined isolation level.
	 * @return the current isolation level, or {@code null} if none
	 * @see TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
	 * @see #setIsolationLevel
	 */
	protected Integer getCurrentIsolationLevel() {
		Integer isolationLevelToUse = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
		if (isolationLevelToUse == null) {
			isolationLevelToUse = getIsolationLevel();
		}
		return isolationLevelToUse;
	}

	/**
	 * Determine the current read-only flag: by default,
	 * the transaction's read-only hint.
	 * @return whether there is a read-only hint for the current scope
	 * @see TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	protected Boolean getCurrentReadOnlyFlag() {
		boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
		return (txReadOnly ? Boolean.TRUE : null);
	}

	private Session.TransactionMode determineTransactionMode(Integer isolationLevel) {
		switch (isolationLevel) {
			case 1 :
			case 2 : return Session.TransactionMode.UPDATE;
			case 4 : return Session.TransactionMode.UPDATE;
			case 8 : return Session.TransactionMode.QUERY;
			default : return Session.TransactionMode.AUTO;
		}
	}

}
