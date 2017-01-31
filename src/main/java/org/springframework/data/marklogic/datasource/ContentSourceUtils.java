/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.XccException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class that provides static methods for obtaining XDBC Sessions from
 * a {@link ContentSource}. Includes special support for Spring-managed
 * transactional Sessions, e.g. managed by {@link ContentSourceTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 * @see #getSession
 * @see #releaseSession
 * @see ContentSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see TransactionSynchronizationManager
 */
public abstract class ContentSourceUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up XDBC Sessions.
	 */
	public static final int SESSION_SYNCHRONIZATION_ORDER = 1000;

	private static final Log logger = LogFactory.getLog(ContentSourceUtils.class);


	/**
	 * Obtain a Session from the given ContentSource. Translates XCCExceptions into
	 * the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Session bound to the current thread, for example
	 * when using {@link ContentSourceTransactionManager}. Will bind a Session to the
	 * thread if transaction synchronization is active, e.g. when running within a
	 * {@link org.springframework.transaction.jta.JtaTransactionManager JTA} transaction).
	 * @param contentSource the ContentSource to obtain Sessions from
	 * @return a XDBC Session from the given ContentSource
	 * @throws CannotGetXdbcSessionException
	 * if the attempt to get a Session failed
	 * @see #releaseSession
	 */
	public static Session getSession(ContentSource contentSource) throws CannotGetXdbcSessionException {
		try {
			return doGetSession(contentSource);
		}
		catch (XccException ex) {
			throw new CannotGetXdbcSessionException("Could not get XDBC Session", ex);
		}
	}

	/**
	 * Actually obtain a XDBC Session from the given ContentSource.
	 * Same as {@link #getSession}, but throwing the original XccException.
	 * <p>Is aware of a corresponding Session bound to the current thread, for example
	 * when using {@link ContentSourceTransactionManager}. Will bind a Session to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * <p>Directly accessed by {@link TransactionAwareContentSourceProxy}.
	 * @param contentSource the ContentSource to obtain Sessions from
	 * @return a XDBC Session from the given ContentSource
	 * @throws XccException if thrown by XDBC methods
	 * @see #doReleaseSession
	 */
	public static Session doGetSession(ContentSource contentSource) throws XccException {
		Assert.notNull(contentSource, "No ContentSource specified");

		SessionHolder sesHolder = (SessionHolder) TransactionSynchronizationManager.getResource(contentSource);
		if (sesHolder != null && (sesHolder.hasSession() || sesHolder.isSynchronizedWithTransaction())) {
			sesHolder.requested();
			if (!sesHolder.hasSession()) {
				logger.debug("Fetching resumed XDBC Session from ContentSource");
				sesHolder.setSession(contentSource.newSession());
			}
			return sesHolder.getSession();
		}
		// Else we either got no holder or an empty thread-bound holder here.

		logger.debug("Fetching XDBC Session from ContentSource");
		Session ses = contentSource.newSession();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for XDBC Session");
			// Use same Session for further XDBC actions within the transaction.
			// Thread-bound object will get removed by synchronization at transaction completion.
			SessionHolder holderToUse = sesHolder;
			if (holderToUse == null) {
				holderToUse = new SessionHolder(ses);
			}
			else {
				holderToUse.setSession(ses);
			}
			holderToUse.requested();
			TransactionSynchronizationManager.registerSynchronization(
					new SessionSynchronization(holderToUse, contentSource));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != sesHolder) {
				TransactionSynchronizationManager.bindResource(contentSource, holderToUse);
			}
		}

		return ses;
	}

	/**
	 * Prepare the given Session with the given transaction semantics.
	 * @param ses the Session to prepare
	 * @param definition the transaction definition to apply
	 * @return the previous isolation level, if any
	 * @throws XccException if thrown by XDBC methods
	 * @see #resetSessionAfterTransaction
	 */
	public static Integer prepareSessionForTransaction(Session ses, TransactionDefinition definition) throws XccException {

		Assert.notNull(ses, "No Session specified");

		if (definition != null && definition.isReadOnly()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Setting XDBC Session [" + ses + "] read-only");
			}
			ses.setTransactionMode(Session.TransactionMode.QUERY);
		} else {
			ses.setTransactionMode(Session.TransactionMode.UPDATE);
		}
		
		// Apply specific isolation level, if any.
		Integer previousIsolationLevel = null;
		if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			if (logger.isDebugEnabled()) {
				logger.debug("Changing isolation level of XDBC Session [" + ses + "] to " +
						definition.getIsolationLevel());
			}
			int currentIsolation = ses.getTransactionMode().ordinal();
			if (currentIsolation != definition.getIsolationLevel()) {
				previousIsolationLevel = currentIsolation;
				ses.setTransactionMode(Session.TransactionMode.values()[definition.getIsolationLevel()]);
			}
		}

		return previousIsolationLevel;
	}

	/**
	 * Reset the given Session after a transaction,
	 * regarding read-only flag and isolation level.
	 * @param ses the Session to reset
	 * @param previousIsolationLevel the isolation level to restore, if any
	 * @see #prepareSessionForTransaction
	 */
	public static void resetSessionAfterTransaction(Session ses, Integer previousIsolationLevel) {
		Assert.notNull(ses, "No Session specified");
		try {
			// Reset transaction isolation to previous value, if changed for the transaction.
			if (previousIsolationLevel != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting isolation level of XDBC Session [" +
							ses + "] to " + previousIsolationLevel);
				}
				ses.setTransactionMode(Session.TransactionMode.values()[previousIsolationLevel]);
			}
		}
		catch (Throwable ex) {
			logger.debug("Could not reset XDBC Session after transaction", ex);
		}
	}

	/**
	 * Determine whether the given XDBC Session is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param ses the Session to check
	 * @param contentSource the ContentSource that the Session was obtained from
	 * (may be {@code null})
	 * @return whether the Session is transactional
	 */
	public static boolean isSessionTransactional(Session ses, ContentSource contentSource) {
		if (contentSource == null) {
			return false;
		}
		SessionHolder sesHolder = (SessionHolder) TransactionSynchronizationManager.getResource(contentSource);
		return (sesHolder != null && sessionEquals(sesHolder, ses));
	}

	/**
	 * Apply the current transaction timeout, if any,
	 * to the given XDBC Statement object.
	 * @param ses the XDBC Session object
	 * @param contentSource the ContentSource that the Session was obtained from
	 * @throws XccException if thrown by XDBC methods
	 * @see Session#setTransactionTimeout(int)
	 */
	public static void applyTransactionTimeout(Session ses, ContentSource contentSource) throws XccException {
		applyTimeout(ses, contentSource, -1);
	}

	/**
	 * Apply the specified timeout - overridden by the current transaction timeout,
	 * if any - to the given XDBC Statement object.
	 * @param ses the XDBC Statement object
	 * @param contentSource the ContentSource that the Session was obtained from
	 * @param timeout the timeout to apply (or 0 for no timeout outside of a transaction)
	 * @throws XccException if thrown by XDBC methods
	 * @see Session#setTransactionTimeout
	 */
	public static void applyTimeout(Session ses, ContentSource contentSource, int timeout) throws XccException {
		Assert.notNull(ses, "No Statement specified");
		Assert.notNull(contentSource, "No ContentSource specified");
		SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(contentSource);
		if (holder != null && holder.hasTimeout()) {
			// Remaining transaction timeout overrides specified value.
			ses.setTransactionTimeout(holder.getTimeToLiveInSeconds());
		}
		else if (timeout >= 0) {
			// No current transaction timeout -> apply specified value.
			ses.setTransactionTimeout(timeout);
		}
	}

	/**
	 * Close the given Session, obtained from the given ContentSource,
	 * if it is not managed externally (that is, not bound to the thread).
	 * @param ses the Session to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param contentSource the ContentSource that the Session was obtained from
	 * (may be {@code null})
	 * @see #getSession
	 */
	public static void releaseSession(Session ses, ContentSource contentSource) {
		try {
			doReleaseSession(ses, contentSource);
		}
		catch (XccException ex) {
			logger.debug("Could not close XDBC Session", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing XDBC Session", ex);
		}
	}

	/**
	 * Actually close the given Session, obtained from the given ContentSource.
	 * Same as {@link #releaseSession}, but throwing the original XccException.
	 * <p>Directly accessed by {@link TransactionAwareContentSourceProxy}.
	 * @param ses the Session to close if necessary
	 * (if this is {@code null}, the call will be ignored)
	 * @param contentSource the ContentSource that the Session was obtained from
	 * (may be {@code null})
	 * @throws XccException if thrown by XDBC methods
	 * @see #doGetSession
	 */
	public static void doReleaseSession(Session ses, ContentSource contentSource) throws XccException {
		if (ses == null) {
			return;
		}
		if (contentSource != null) {
			SessionHolder sesHolder = (SessionHolder) TransactionSynchronizationManager.getResource(contentSource);
			if (sesHolder != null && sessionEquals(sesHolder, ses)) {
				// It's the transactional Session: Don't close it.
				sesHolder.released();
				return;
			}
		}
		logger.debug("Returning XDBC Session to ContentSource");
		doCloseSession(ses, contentSource);
	}

	/**
	 * Close the Session, unless a {@link SmartContentSource} doesn't want us to.
	 * @param ses the Session to close if necessary
	 * @param contentSource the ContentSource that the Session was obtained from
	 * @throws XccException if thrown by XDBC methods
	 * @see Session#close()
	 * @see SmartContentSource#shouldClose(Session)
	 */
	public static void doCloseSession(Session ses, ContentSource contentSource) throws XccException {
		if (!(contentSource instanceof SmartContentSource) || ((SmartContentSource) contentSource).shouldClose(ses)) {
			ses.close();
		}
	}

	/**
	 * Determine whether the given two Sessions are equal, asking the target
	 * Session in case of a proxy. Used to detect equality even if the
	 * user passed in a raw target Session while the held one is a proxy.
	 * @param sesHolder the SessionHolder for the held Session (potentially a proxy)
	 * @param passedInCon the Session passed-in by the user
	 * (potentially a target Session without proxy)
	 * @return whether the given Sessions are equal
	 * @see #getTargetSession
	 */
	private static boolean sessionEquals(SessionHolder sesHolder, Session passedInCon) {
		if (!sesHolder.hasSession()) {
			return false;
		}
		Session heldCon = sesHolder.getSession();
		// Explicitly check for identity too: for Session handles that do not implement
		// "equals" properly, such as the ones Commons DBCP exposes).
		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
				getTargetSession(heldCon).equals(passedInCon));
	}

	/**
	 * Return the innermost target Session of the given Session. If the given
	 * Session is a proxy, it will be unwrapped until a non-proxy Session is
	 * found. Otherwise, the passed-in Session will be returned as-is.
	 * @param ses the Session proxy to unwrap
	 * @return the innermost target Session, or the passed-in one if no proxy
	 * @see SessionProxy#getTargetSession()
	 */
	public static Session getTargetSession(Session ses) {
		Session conToUse = ses;
		while (conToUse instanceof SessionProxy) {
			conToUse = ((SessionProxy) conToUse).getTargetSession();
		}
		return conToUse;
	}

	/**
	 * Determine the session synchronization order to use for the given
	 * ContentSource. Decreased for every level of nesting that a ContentSource
	 * has, checked through the level of DelegatingContentSource nesting.
	 * @param contentSource the ContentSource to check
	 * @return the session synchronization order to use
	 * @see #SESSION_SYNCHRONIZATION_ORDER
	 */
	private static int getSessionSynchronizationOrder(ContentSource contentSource) {
		int order = SESSION_SYNCHRONIZATION_ORDER;
		ContentSource currDs = contentSource;
		while (currDs instanceof DelegatingContentSource) {
			order--;
			currDs = ((DelegatingContentSource) currDs).getTargetContentSource();
		}
		return order;
	}


	/**
	 * Callback for resource cleanup at the end of a non-native XDBC transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class SessionSynchronization extends TransactionSynchronizationAdapter {

		private final SessionHolder sessionHolder;

		private final ContentSource contentSource;

		private int order;

		private boolean holderActive = true;

		public SessionSynchronization(SessionHolder sessionHolder, ContentSource contentSource) {
			this.sessionHolder = sessionHolder;
			this.contentSource = contentSource;
			this.order = getSessionSynchronizationOrder(contentSource);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.contentSource);
				if (this.sessionHolder.hasSession() && !this.sessionHolder.isOpen()) {
					// Release Session on suspend if the application doesn't keep
					// a handle to it anymore. We will fetch a fresh Session if the
					// application accesses the SessionHolder again after resume,
					// assuming that it will participate in the same transaction.
					releaseSession(this.sessionHolder.getSession(), this.contentSource);
					this.sessionHolder.setSession(null);
				}
			}
		}

		@Override
		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.contentSource, this.sessionHolder);
			}
		}

		@Override
		public void beforeCompletion() {
			// Release Session early if the holder is not open anymore
			// (that is, not used by another resource like a Hibernate Session
			// that has its own cleanup via transaction synchronization),
			// to avoid issues with strict JTA implementations that expect
			// the close call before transaction completion.
			if (!this.sessionHolder.isOpen()) {
				TransactionSynchronizationManager.unbindResource(this.contentSource);
				this.holderActive = false;
				if (this.sessionHolder.hasSession()) {
					releaseSession(this.sessionHolder.getSession(), this.contentSource);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			// If we haven't closed the Session in beforeCompletion,
			// close it now. The holder might have been used for other
			// cleanup in the meantime, for example by a Hibernate Session.
			if (this.holderActive) {
				// The thread-bound SessionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				TransactionSynchronizationManager.unbindResourceIfPossible(this.contentSource);
				this.holderActive = false;
				if (this.sessionHolder.hasSession()) {
					releaseSession(this.sessionHolder.getSession(), this.contentSource);
					// Reset the SessionHolder: It might remain bound to the thread.
					this.sessionHolder.setSession(null);
				}
			}
			this.sessionHolder.reset();
		}
	}

}
