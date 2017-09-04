package com._4dconcept.springframework.data.marklogic.datasource.lookup;

import org.springframework.core.Constants;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * ContentSource that routes to one of various target ContentSources based on the
 * current transaction isolation level. The target ContentSources need to be
 * configured with the isolation level name as key, as defined on the
 * {@link TransactionDefinition TransactionDefinition interface}.
 *
 * <p>This is particularly useful in combination with JTA transaction management
 * (typically through Spring's {@link org.springframework.transaction.jta.JtaTransactionManager}).
 * Standard JTA does not support transaction-specific isolation levels. Some JTA
 * providers support isolation levels as a vendor-specific extension (e.g. WebLogic),
 * which is the preferred way of addressing this. As alternative (e.g. on WebSphere),
 * the target database can be represented through multiple JNDI ContentSources, each
 * configured with a different isolation level (for the entire ContentSource).
 * The present ContentSource router allows to transparently switch to the
 * appropriate ContentSource based on the current transaction's isolation level.
 *
 * <p>The configuration can for example look like this, assuming that the target
 * ContentSources are defined as individual Spring beans with names
 * "myRepeatableReadContentSource", "mySerializableContentSource" and "myDefaultContentSource":
 *
 * <pre class="code">
 * &lt;bean id="contentSourceRouter" class="com._4dconcept.contentfactory.extra.datasource.lookup.IsolationLevelContentSourceRouter"&gt;
 *   &lt;property name="targetContentSources"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="ISOLATION_REPEATABLE_READ" value-ref="myRepeatableReadContentSource"/&gt;
 *       &lt;entry key="ISOLATION_SERIALIZABLE" value-ref="mySerializableContentSource"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 *   &lt;property name="defaultTargetContentSource" ref="myDefaultContentSource"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note: If you are using this router in combination with Spring's
 * {@link org.springframework.transaction.jta.JtaTransactionManager},
 * don't forget to switch the "allowCustomIsolationLevels" flag to "true".
 * (By default, JtaTransactionManager will only accept a default isolation level
 * because of the lack of isolation level support in standard JTA itself.)
 *
 * <pre class="code">
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="allowCustomIsolationLevels" value="true"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see #setTargetContentSources
 * @see #setDefaultTargetContentSource
 * @see TransactionDefinition#ISOLATION_READ_UNCOMMITTED
 * @see TransactionDefinition#ISOLATION_READ_COMMITTED
 * @see TransactionDefinition#ISOLATION_REPEATABLE_READ
 * @see TransactionDefinition#ISOLATION_SERIALIZABLE
 * @see org.springframework.transaction.jta.JtaTransactionManager
 */
public class IsolationLevelContentSourceRouter extends AbstractRoutingContentSource {

	/** Constants instance for TransactionDefinition */
	private static final Constants constants = new Constants(TransactionDefinition.class);


	/**
	 * Supports Integer values for the isolation level constants
	 * as well as isolation level names as defined on the
	 * {@link TransactionDefinition TransactionDefinition interface}.
	 */
	@Override
	protected Object resolveSpecifiedLookupKey(Object lookupKey) {
		if (lookupKey instanceof Integer) {
			return lookupKey;
		}
		else if (lookupKey instanceof String) {
			String constantName = (String) lookupKey;
			if (!constantName.startsWith(DefaultTransactionDefinition.PREFIX_ISOLATION)) {
				throw new IllegalArgumentException("Only isolation constants allowed");
			}
			return constants.asNumber(constantName);
		}
		else {
			throw new IllegalArgumentException(
					"Invalid lookup key - needs to be isolation level Integer or isolation level name String: " + lookupKey);
		}
	}

	@Override
	protected Object determineCurrentLookupKey() {
		return TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
	}

}
