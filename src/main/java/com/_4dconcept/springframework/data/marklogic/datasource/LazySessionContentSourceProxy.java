package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.XccException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Constants;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy for a target ContentSource, fetching actual XDBC Sessions lazily,
 * i.e. not until first creation of a Statement. Session initialization
 * properties like auto-commit mode, transaction isolation and read-only mode
 * will be kept and applied to the actual XDBC Session as soon as an
 * actual Session is fetched (if ever). Consequently, commit and rollback
 * calls will be ignored if no Statements have been created.
 *
 * <p>This ContentSource proxy allows to avoid fetching XDBC Sessions from
 * a pool unless actually necessary. XDBC transaction control can happen
 * without fetching a Session from the pool or communicating with the
 * database; this will be done lazily on first creation of a XDBC Statement.
 *
 * <p><b>If you configure both a LazySessionContentSourceProxy and a
 * TransactionAwareContentSourceProxy, make sure that the latter is the outermost
 * ContentSource.</b> In such a scenario, data access code will talk to the
 * transaction-aware ContentSource, which will in turn work with the
 * LazySessionContentSourceProxy.
 *
 * <p>Lazy fetching of physical XDBC Sessions is particularly beneficial
 * in a generic transaction demarcation environment. It allows you to demarcate
 * transactions on all methods that could potentially perform data access,
 * without paying a performance penalty if no actual data access happens.
 *
 * <p>This ContentSource proxy gives you behavior analogous to JTA and a
 * transactional JNDI ContentSource (as provided by the J2EE server), even
 * with a local transaction strategy like ContentSourceTransactionManager or
 * HibernateTransactionManager. It does not add value with Spring's
 * JtaTransactionManager as transaction strategy.
 *
 * <p>Lazy fetching of XDBC Sessions is also recommended for read-only
 * operations with Hibernate, in particular if the chances of resolving the
 * result in the second-level cache are high. This avoids the need to
 * communicate with the database at all for such read-only operations.
 * You will get the same effect with non-transactional reads, but lazy fetching
 * of XDBC Sessions allows you to still perform reads in transactions.
 *
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 * @since 1.1.4
 * @see ContentSourceTransactionManager
 */
public class LazySessionContentSourceProxy extends DelegatingContentSource {

	/** Constants instance for TransactionDefinition */
	private static final Constants constants = new Constants(Session.class);

	private static final Log logger = LogFactory.getLog(LazySessionContentSourceProxy.class);

	private String defaultTransactionMode;

	/**
	 * Create a new LazySessionContentSourceProxy.
	 * @see #setTargetContentSource
	 */
	public LazySessionContentSourceProxy() {
	}

	/**
	 * Create a new LazySessionContentSourceProxy.
	 * @param targetContentSource the target ContentSource
	 */
	public LazySessionContentSourceProxy(ContentSource targetContentSource) {
		setTargetContentSource(targetContentSource);
		afterPropertiesSet();
	}

	public void setDefaultTransactionMode(String defaultTransactionMode) {
		this.defaultTransactionMode = defaultTransactionMode;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// Determine default auto-commit and transaction isolation
		// via a Session from the target ContentSource, if possible.
		if (this.defaultTransactionMode == null) {
			Session ses = getTargetContentSource().newSession();
			try {
				checkDefaultSessionProperties(ses);
			}
			finally {
				ses.close();
			}
		}
	}

	/**
	 * Check the default session properties (auto-commit, transaction isolation),
	 * keeping them to be able to expose them correctly without fetching an actual
	 * XDBC Session from the target ContentSource.
	 * <p>This will be invoked once on startup, but also for each retrieval of a
	 * target Session. If the check failed on startup (because the database was
	 * down), we'll lazily retrieve those settings.
	 * @param ses the Session to use for checking
	 */
	protected synchronized void checkDefaultSessionProperties(Session ses) {
		if (this.defaultTransactionMode == null) {
			this.defaultTransactionMode = ses.getTransactionMode().toString();
		}
	}

	/**
	 * Expose the default transaction mode value.
	 */
	protected String defaultTransactionMode() {
		return this.defaultTransactionMode;
	}

	/**
	 * Return a Session handle that lazily fetches an actual XDBC Session
	 * when asked for a Statement (or PreparedStatement or CallableStatement).
	 * <p>The returned Session handle implements the SessionProxy interface,
	 * allowing to retrieve the underlying target Session.
	 * @return a lazy Session handle
	 * @see SessionProxy#getTargetSession()
	 */
	@Override
	public Session newSession() {
		return (Session) Proxy.newProxyInstance(
				SessionProxy.class.getClassLoader(),
				new Class<?>[] {SessionProxy.class},
				new LazySessionInvocationHandler());
	}

	/**
	 * Return a Session handle that lazily fetches an actual XDBC Session
	 * when asked for a Statement (or PreparedStatement or CallableStatement).
	 * <p>The returned Session handle implements the SessionProxy interface,
	 * allowing to retrieve the underlying target Session.
	 * @param username the per-Session username
	 * @param password the per-Session password
	 * @return a lazy Session handle
	 * @see SessionProxy#getTargetSession() ()
	 */
	@Override
	public Session newSession(String username, String password) {
		return (Session) Proxy.newProxyInstance(
				SessionProxy.class.getClassLoader(),
				new Class<?>[] {SessionProxy.class},
				new LazySessionInvocationHandler(username, password));
	}


	/**
	 * Invocation handler that defers fetching an actual XDBC Session
	 * until first creation of a Statement.
	 */
	private class LazySessionInvocationHandler implements InvocationHandler {

		private String username;

		private String password;

		private String transactionMode;

		private boolean closed = false;

		private Session target;

		public LazySessionInvocationHandler() {
			this.transactionMode = defaultTransactionMode();
		}

		public LazySessionInvocationHandler(String username, String password) {
			this();
			this.username = username;
			this.password = password;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on SessionProxy interface coming in...

			if (method.getName().equals("equals")) {
				// We must avoid fetching a target Session for "equals".
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (method.getName().equals("hashCode")) {
				// We must avoid fetching a target Session for "hashCode",
				// and we must return the same hash code even when the target
				// Session has been fetched: use hashCode of Session proxy.
				return System.identityHashCode(proxy);
			}
			else if (method.getName().equals("getTargetSession")) {
				// Handle getTargetSession method: return underlying session.
				return getTargetSession(method);
			}

			if (!hasTargetSession()) {
				// No physical target Session kept yet ->
				// resolve transaction demarcation methods without fetching
				// a physical XDBC Session until absolutely necessary.

				if (method.getName().equals("toString")) {
					return "Lazy Session proxy for target ContentSource [" + getTargetContentSource() + "]";
				}
				else if (method.getName().equals("getTransactionMode")) {
					if (this.transactionMode != null) {
						return this.transactionMode;
					}
					// Else fetch actual Session and check there,
					// because we didn't have a default specified.
				}
				else if (method.getName().equals("setTransactionMode")) {
					this.transactionMode = Session.TransactionMode.AUTO.toString();
					return null;
				}
				else if (method.getName().equals("commit")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("rollback")) {
					// Ignore: no statements created yet.
					return null;
				}
				else if (method.getName().equals("close")) {
					// Ignore: no target session yet.
					this.closed = true;
					return null;
				}
				else if (method.getName().equals("isClosed")) {
					return this.closed;
				}
				else if (this.closed) {
					// Session proxy closed, without ever having fetched a
					// physical XDBC Session: throw corresponding SQLException.
					throw new SessionClosedException();
				}
			}

			// Target Session already fetched,
			// or target Session necessary for current operation ->
			// invoke method on target session.
			try {
				return method.invoke(getTargetSession(method), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		/**
		 * Return whether the proxy currently holds a target Session.
		 */
		private boolean hasTargetSession() {
			return (this.target != null);
		}

		/**
		 * Return the target Session, fetching it and initializing it if necessary.
		 */
		private Session getTargetSession(Method operation) {
			if (this.target == null) {
				// No target Session held -> fetch one.
				if (logger.isDebugEnabled()) {
					logger.debug("Connecting to database for operation '" + operation.getName() + "'");
				}

				// Fetch physical Session from ContentSource.
				this.target = (this.username != null) ?
						getTargetContentSource().newSession(this.username, this.password) :
						getTargetContentSource().newSession();

				// If we still lack default session properties, check them now.
				checkDefaultSessionProperties(this.target);

				// Apply kept transaction settings, if any.
				if (this.transactionMode != null) {
					try {
						this.target.setTransactionMode(Session.TransactionMode.valueOf(this.transactionMode));
					}
					catch (Exception ex) {
						logger.debug("Could not set XDBC Session transactionMode", ex);
					}
				}
			}

			else {
				// Target Session already held -> return it.
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing database session for operation '" + operation.getName() + "'");
				}
			}

			return this.target;
		}

		public class SessionClosedException extends XccException {
			public SessionClosedException() {
				super("Session handle already closed");
			}
		}
	}

}
