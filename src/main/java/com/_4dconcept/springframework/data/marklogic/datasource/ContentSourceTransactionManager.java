/*
 * Copyright 2017 the original author or authors.
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
package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.XccException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.transaction.PlatformTransactionManager}
 * implementation for a single XDBC {@link ContentSource}. This class is
 * capable of working in any environment with any XDBC driver, as long as the setup
 * uses a {@code com.marklogic.xcc.ContentSource} as its {@code Session} factory mechanism.
 * Binds a XDBC Session from the specified ContentSource to the current thread,
 * potentially allowing for one thread-bound Session per ContentSource.
 *
 * <p><b>Note: The ContentSource that this transaction manager operates on needs
 * to return independent Sessions.</b> The Sessions may come from a pool
 * (the typical case), but the ContentSource must not return thread-scoped /
 * request-scoped Sessions or the like. This transaction manager will
 * associate Sessions with thread-bound transactions itself, according
 * to the specified propagation behavior. It assumes that a separate,
 * independent Session can be obtained even during an ongoing transaction.
 *
 * <p>Application code is required to retrieve the XDBC Session via
 * {@link ContentSourceUtils#getSession(ContentSource)} instead of a standard
 * J2EE-style {@link ContentSource#newSession()} call.
 * If not used in combination with this transaction manager, the
 * {@link ContentSourceUtils} lookup strategy behaves exactly like the native
 * ContentSource lookup; it can thus be used in a portable fashion.
 *
 * <p>Alternatively, you can allow application code to work with the standard
 * J2EE-style lookup pattern {@link ContentSource#newSession()}, for example for
 * legacy code that is not aware of Spring at all. In that case, define a
 * {@link TransactionAwareContentSourceProxy} for your target ContentSource, and pass
 * that proxy ContentSource to your DAOs, which will automatically participate in
 * Spring-managed transactions when accessing it.
 *
 * <p>Supports custom isolation levels, and timeouts which get applied as
 * appropriate XDBC statement timeouts. To support the latter, application code
 * must either call {@link ContentSourceUtils#applyTransactionTimeout} for each created XDBC Statement,
 * or go through a {@link TransactionAwareContentSourceProxy} which will create
 * timeout-aware XDBC Sessions and Statements automatically.
 *
 * <p>Consider defining a {@link LazySessionContentSourceProxy} for your target
 * ContentSource, pointing both this transaction manager and your DAOs to it.
 * This will lead to optimized handling of "empty" transactions, i.e. of transactions
 * without any XDBC statements executed. A LazySessionContentSourceProxy will not fetch
 * an actual XDBC Session from the target ContentSource until a Statement gets executed,
 * lazily applying the specified transaction settings to the target Session.
 *
 * <p>This transaction manager can be used as a replacement for the
 * {@link org.springframework.transaction.jta.JtaTransactionManager} in the single
 * resource case, as it does not require a container that supports JTA, typically
 * in combination with a locally defined XDBC ContentSource (e.g. an Apache Commons
 * DBCP session pool). Switching between this local strategy and a JTA
 * environment is just a matter of configuration!
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 *
 * @see #setNestedTransactionAllowed
 * @see ContentSourceUtils#getSession(ContentSource)
 * @see ContentSourceUtils#applyTransactionTimeout
 * @see ContentSourceUtils#releaseSession
 * @see TransactionAwareContentSourceProxy
 * @see LazySessionContentSourceProxy
 */
@SuppressWarnings("serial")
public class ContentSourceTransactionManager extends AbstractPlatformTransactionManager
        implements ResourceTransactionManager, InitializingBean {

    private ContentSource contentSource;


    /**
     * Create a new ContentSourceTransactionManager instance.
     * A ContentSource has to be set to be able to use it.
     * @see #setContentSource
     */
    public ContentSourceTransactionManager() {
        setNestedTransactionAllowed(true);
    }

    /**
     * Create a new ContentSourceTransactionManager instance.
     * @param contentSource XDBC ContentSource to manage transactions for
     */
    public ContentSourceTransactionManager(ContentSource contentSource) {
        this();
        setContentSource(contentSource);
        afterPropertiesSet();
    }

    /**
     * @return the XDBC ContentSource that this instance manages transactions for.
     */
    public ContentSource getContentSource() {
        return this.contentSource;
    }

    /**
     * Set the XDBC ContentSource that this instance should manage transactions for.
     * <p>This will typically be a locally defined ContentSource, for example an
     * Apache Commons DBCP session pool. Alternatively, you can also drive
     * transactions for a non-XA J2EE ContentSource fetched from JNDI. For an XA
     * ContentSource, use JtaTransactionManager.
     * <p>The ContentSource specified here should be the target ContentSource to manage
     * transactions for, not a TransactionAwareContentSourceProxy. Only data access
     * code may work with TransactionAwareContentSourceProxy, while the transaction
     * manager needs to work on the underlying target ContentSource. If there's
     * nevertheless a TransactionAwareContentSourceProxy passed in, it will be
     * unwrapped to extract its target ContentSource.
     * <p><b>The ContentSource passed in here needs to return independent Sessions.</b>
     * The Sessions may come from a pool (the typical case), but the ContentSource
     * must not return thread-scoped / request-scoped Sessions or the like.
     * @see TransactionAwareContentSourceProxy
     * @see org.springframework.transaction.jta.JtaTransactionManager
     *
     * @param contentSource the contentSource the manager will work on
     */
    public void setContentSource(ContentSource contentSource) {
        if (contentSource instanceof TransactionAwareContentSourceProxy) {
            // If we got a TransactionAwareContentSourceProxy, we need to perform transactions
            // for its underlying target ContentSource, else data access code won't see
            // properly exposed transactions (i.e. transactions for the target ContentSource).
            this.contentSource = ((TransactionAwareContentSourceProxy) contentSource).getTargetContentSource();
        } else {
            this.contentSource = contentSource;
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (getContentSource() == null) {
            throw new IllegalArgumentException("Property 'contentSource' is required");
        }
    }

    /**
     * @return the resource factory
     */
    @Override
    public Object getResourceFactory() {
        return getContentSource();
    }

    @Override
    protected Object doGetTransaction() {
        ContentSourceTransactionObject txObject = new ContentSourceTransactionObject();
        SessionHolder sesHolder =
                (SessionHolder) TransactionSynchronizationManager.getResource(this.contentSource);
        txObject.setSessionHolder(sesHolder, false);
        return txObject;
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) transaction;
        return (txObject.getSessionHolder() != null && txObject.getSessionHolder().isTransactionActive());
    }

    /**
     * This implementation sets the isolation level but ignores the timeout.
     */
    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) transaction;
        Session ses = null;

        try {
            if (txObject.getSessionHolder() == null ||
                    txObject.getSessionHolder().isSynchronizedWithTransaction()) {
                Session newSes = this.contentSource.newSession();
                if (logger.isDebugEnabled()) {
                    logger.debug("Acquired Session [" + newSes + "] for XDBC transaction");
                }
                txObject.setSessionHolder(new SessionHolder(newSes), true);
            }

            txObject.getSessionHolder().setSynchronizedWithTransaction(true);
            ses = txObject.getSessionHolder().getSession();

            Integer previousIsolationLevel = ContentSourceUtils.prepareSessionForTransaction(ses, definition);
            txObject.setPreviousIsolationLevel(previousIsolationLevel);

            txObject.getSessionHolder().setTransactionActive(true);

            int timeout = determineTimeout(definition);
            if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                txObject.getSessionHolder().setTimeoutInSeconds(timeout);
            }

            // Bind the session holder to the thread.
            if (txObject.isNewSessionHolder()) {
                TransactionSynchronizationManager.bindResource(getContentSource(), txObject.getSessionHolder());
            }
        } catch (Throwable ex) {
            if (txObject.isNewSessionHolder()) {
                ContentSourceUtils.releaseSession(ses, this.contentSource);
                txObject.setSessionHolder(null, false);
            }
            throw new CannotCreateTransactionException("Could not open XDBC Session for transaction", ex);
        }
    }

    @Override
    protected Object doSuspend(Object transaction) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) transaction;
        txObject.setSessionHolder(null);
        SessionHolder sesHolder = (SessionHolder)
                TransactionSynchronizationManager.unbindResource(this.contentSource);
        return sesHolder;
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        SessionHolder sesHolder = (SessionHolder) suspendedResources;
        TransactionSynchronizationManager.bindResource(this.contentSource, sesHolder);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) status.getTransaction();
        Session ses = txObject.getSessionHolder().getSession();
        if (status.isDebug()) {
            logger.debug("Committing XDBC transaction on Session [" + ses + "]");
        }
        try {
            ses.commit();
        } catch (XccException ex) {
            throw new TransactionSystemException("Could not commit XDBC transaction", ex);
        }
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) status.getTransaction();
        Session ses = txObject.getSessionHolder().getSession();
        if (status.isDebug()) {
            logger.debug("Rolling back XDBC transaction on Session [" + ses + "]");
        }
        try {
            ses.rollback();
        } catch (XccException ex) {
            throw new TransactionSystemException("Could not roll back XDBC transaction", ex);
        }
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) status.getTransaction();
        if (status.isDebug()) {
            logger.debug("Setting XDBC transaction [" + txObject.getSessionHolder().getSession() +
                    "] rollback-only");
        }
        txObject.setRollbackOnly();
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        ContentSourceTransactionObject txObject = (ContentSourceTransactionObject) transaction;

        // Remove the session holder from the thread, if exposed.
        if (txObject.isNewSessionHolder()) {
            TransactionSynchronizationManager.unbindResource(this.contentSource);
        }

        // Reset session.
        Session ses = txObject.getSessionHolder().getSession();
        try {
            ContentSourceUtils.resetSessionAfterTransaction(ses, txObject.getPreviousIsolationLevel());
        } catch (Throwable ex) {
            logger.debug("Could not reset XDBC Session after transaction", ex);
        }

        if (txObject.isNewSessionHolder()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing XDBC Session [" + ses + "] after transaction");
            }
            ContentSourceUtils.releaseSession(ses, this.contentSource);
        }

        txObject.getSessionHolder().clear();
    }


    /**
     * ContentSource transaction object, representing a SessionHolder.
     * Used as transaction object by ContentSourceTransactionManager.
     */
    private static class ContentSourceTransactionObject extends XdbcTransactionObjectSupport {

        private boolean newSessionHolder;

        private boolean mustRestoreAutoCommit;

        public void setSessionHolder(SessionHolder sessionHolder, boolean newSessionHolder) {
            super.setSessionHolder(sessionHolder);
            this.newSessionHolder = newSessionHolder;
        }

        public boolean isNewSessionHolder() {
            return this.newSessionHolder;
        }

        public boolean isMustRestoreAutoCommit() {
            return this.mustRestoreAutoCommit;
        }

        public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
            this.mustRestoreAutoCommit = mustRestoreAutoCommit;
        }

        public void setRollbackOnly() {
            getSessionHolder().setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return getSessionHolder().isRollbackOnly();
        }
    }

}
