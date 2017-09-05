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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy for a target XDBC {@link ContentSource}, adding awareness of
 * Spring-managed transactions. Similar to a transactional JNDI ContentSource
 * as provided by a J2EE server.
 *
 * <p>Data access code that should remain unaware of Spring's data access support
 * can work with this proxy to seamlessly participate in Spring-managed transactions.
 * Note that the transaction manager, for example {@link ContentSourceTransactionManager},
 * still needs to work with the underlying ContentSource, <i>not</i> with this proxy.
 *
 * <p><b>Make sure that TransactionAwareContentSourceProxy is the outermost ContentSource
 * of a chain of ContentSource proxies/adapters.</b> TransactionAwareContentSourceProxy
 * can delegate either directly to the target session pool or to some
 * intermediary proxy/adapter like {@link LazySessionContentSourceProxy} or
 * {@link UserCredentialsContentSourceAdapter}.
 *
 * <p>Delegates to {@link ContentSourceUtils} for automatically participating in
 * thread-bound transactions, for example managed by {@link ContentSourceTransactionManager}.
 * {@code newSession} calls and {@code close} calls on returned Sessions
 * will behave properly within a transaction, i.e. always operate on the transactional
 * Session. If not within a transaction, normal ContentSource behavior applies.
 *
 * <p>This proxy allows data access code to work with the plain XDBC API and still
 * participate in Spring-managed transactions, similar to XDBC code in a J2EE/JTA
 * environment. However, if possible, use Spring's ContentSourceUtils, XdbcTemplate or
 * XDBC operation objects to get transaction participation even without a proxy for
 * the target ContentSource, avoiding the need to define such a proxy in the first place.
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 *
 * @see ContentSource#newSession()
 * @see Session#close()
 * @see ContentSourceUtils#doGetSession
 * @see ContentSourceUtils#applyTransactionTimeout
 * @see ContentSourceUtils#doReleaseSession
 */
public class TransactionAwareContentSourceProxy extends DelegatingContentSource {

    private boolean reobtainTransactionalSessions = false;


    /**
     * Create a new TransactionAwareContentSourceProxy.
     *
     * @see #setTargetContentSource
     */
    public TransactionAwareContentSourceProxy() {
    }

    /**
     * Create a new TransactionAwareContentSourceProxy.
     *
     * @param targetContentSource the target ContentSource
     */
    public TransactionAwareContentSourceProxy(ContentSource targetContentSource) {
        super(targetContentSource);
    }

    /**
     * Specify whether to reobtain the target Session for each operation
     * performed within a transaction.
     * <p>The default is "false". Specify "true" to reobtain transactional
     * Sessions for every call on the Session proxy; this is advisable
     * on JBoss if you hold on to a Session handle across transaction boundaries.
     * <p>The effect of this setting is similar to the
     * "hibernate.connection.release_mode" value "after_statement".
     */
    public void setReobtainTransactionalSessions(boolean reobtainTransactionalSessions) {
        this.reobtainTransactionalSessions = reobtainTransactionalSessions;
    }


    /**
     * Delegates to ContentSourceUtils for automatically participating in Spring-managed
     * transactions. Throws the original XccException, if any.
     * <p>The returned Session handle implements the SessionProxy interface,
     * allowing to retrieve the underlying target Session.
     *
     * @return a transactional Session if any, a new one else
     * @see ContentSourceUtils#doGetSession
     * @see SessionProxy#getTargetSession
     */
    @Override
    public Session newSession() {
        ContentSource ds = getTargetContentSource();
        Assert.state(ds != null, "'targetContentSource' is required");
        return getTransactionAwareSessionProxy(ds);
    }

    /**
     * Wraps the given Session with a proxy that delegates every method call to it
     * but delegates {@code close()} calls to ContentSourceUtils.
     *
     * @param targetContentSource ContentSource that the Session came from
     * @return the wrapped Session
     * @see Session#close()
     * @see ContentSourceUtils#doReleaseSession
     */
    protected Session getTransactionAwareSessionProxy(ContentSource targetContentSource) {
        return (Session) Proxy.newProxyInstance(
                SessionProxy.class.getClassLoader(),
                new Class<?>[]{SessionProxy.class},
                new TransactionAwareInvocationHandler(targetContentSource));
    }

    /**
     * Determine whether to obtain a fixed target Session for the proxy
     * or to reobtain the target Session for each operation.
     * <p>The default implementation returns {@code true} for all
     * standard cases. This can be overridden through the
     * {@link #setReobtainTransactionalSessions "reobtainTransactionalSessions"}
     * flag, which enforces a non-fixed target Session within an active transaction.
     * Note that non-transactional access will always use a fixed Session.
     *
     * @param targetContentSource the target ContentSource
     */
    protected boolean shouldObtainFixedSession(ContentSource targetContentSource) {
        return (!TransactionSynchronizationManager.isSynchronizationActive() ||
                !this.reobtainTransactionalSessions);
    }


    /**
     * Invocation handler that delegates close calls on XDBC Sessions
     * to ContentSourceUtils for being aware of thread-bound transactions.
     */
    private class TransactionAwareInvocationHandler implements InvocationHandler {

        private final ContentSource targetContentSource;

        private Session target;

        private boolean closed = false;

        public TransactionAwareInvocationHandler(ContentSource targetContentSource) {
            this.targetContentSource = targetContentSource;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Invocation on SessionProxy interface coming in...

            if (method.getName().equals("equals")) {
                // Only considered as equal when proxies are identical.
                return (proxy == args[0]);
            } else if (method.getName().equals("hashCode")) {
                // Use hashCode of Session proxy.
                return System.identityHashCode(proxy);
            } else if (method.getName().equals("toString")) {
                // Allow for differentiating between the proxy and the raw Session.
                StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Session ");
                if (this.target != null) {
                    sb.append("[").append(this.target.toString()).append("]");
                } else {
                    sb.append(" from ContentSource [").append(this.targetContentSource).append("]");
                }
                return sb.toString();
            } else if (method.getName().equals("close")) {
                // Handle close method: only close if not within a transaction.
                ContentSourceUtils.doReleaseSession(this.target, this.targetContentSource);
                this.closed = true;
                return null;
            } else if (method.getName().equals("isClosed")) {
                return this.closed;
            }

            if (this.target == null) {
                if (this.closed) {
                    throw new SessionClosedException();
                }
                if (shouldObtainFixedSession(this.targetContentSource)) {
                    this.target = ContentSourceUtils.doGetSession(this.targetContentSource);
                }
            }
            Session actualTarget = this.target;
            if (actualTarget == null) {
                actualTarget = ContentSourceUtils.doGetSession(this.targetContentSource);
            }

            if (method.getName().equals("getTargetSession")) {
                // Handle getTargetSession method: return underlying Session.
                return actualTarget;
            }

            // Invoke method on target Session.
            try {
                Object retVal = method.invoke(actualTarget, args);

//				// If return value is a Statement, apply transaction timeout.
//				// Applies to createStatement, prepareStatement, prepareCall.
//				if (retVal instanceof Statement) {
//					ContentSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetContentSource);
//				}

                return retVal;
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            } finally {
                if (actualTarget != this.target) {
                    ContentSourceUtils.doReleaseSession(actualTarget, this.targetContentSource);
                }
            }
        }

        public class SessionClosedException extends XccException {
            public SessionClosedException() {
                super("Session handle already closed");
            }
        }
    }


}
