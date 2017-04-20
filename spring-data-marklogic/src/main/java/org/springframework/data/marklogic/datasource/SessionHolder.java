/*
 * Copyright 2002-2012 the original author or authors.
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

import com.marklogic.xcc.Session;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Session holder, wrapping a XDBC Session.
 * {@link ContentSourceTransactionManager} binds instances of this class
 * to the thread, for a specific ContentSource.
 *
 * <p>Inherits rollback-only support for nested XDBC transactions
 * and reference count functionality from the base class.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 * @see ContentSourceTransactionManager
 * @see ContentSourceUtils
 */
public class SessionHolder extends ResourceHolderSupport {

	private SessionHandle sessionHandle;

	private Session currentSession;

	private boolean transactionActive = false;

	/**
	 * Create a new SessionHolder for the given SessionHandle.
	 * @param sessionHandle the SessionHandle to hold
	 */
	public SessionHolder(SessionHandle sessionHandle) {
		Assert.notNull(sessionHandle, "SessionHandle must not be null");
		this.sessionHandle = sessionHandle;
	}

	/**
	 * Create a new SessionHolder for the given XDBC Session,
	 * wrapping it with a {@link SimpleSessionHandle},
	 * assuming that there is no ongoing transaction.
	 * @param session the XDBC Session to hold
	 * @see SimpleSessionHandle
	 * @see #SessionHolder(Session, boolean)
	 */
	public SessionHolder(Session session) {
		this.sessionHandle = new SimpleSessionHandle(session);
	}

	/**
	 * Create a new SessionHolder for the given XDBC Session,
	 * wrapping it with a {@link SimpleSessionHandle}.
	 * @param session the XDBC Session to hold
	 * @param transactionActive whether the given Session is involved
	 * in an ongoing transaction
	 * @see SimpleSessionHandle
	 */
	public SessionHolder(Session session, boolean transactionActive) {
		this(session);
		this.transactionActive = transactionActive;
	}


	/**
	 * Return the SessionHandle held by this SessionHolder.
	 */
	public SessionHandle getSessionHandle() {
		return this.sessionHandle;
	}

	/**
	 * Return whether this holder currently has a Session.
	 */
	protected boolean hasSession() {
		return (this.sessionHandle != null);
	}

	/**
	 * Set whether this holder represents an active, XDBC-managed transaction.
	 * @see ContentSourceTransactionManager
	 */
	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	/**
	 * Return whether this holder represents an active, XDBC-managed transaction.
	 */
	protected boolean isTransactionActive() {
		return this.transactionActive;
	}


	/**
	 * Override the existing Session handle with the given Session.
	 * Reset the handle if given {@code null}.
	 * <p>Used for releasing the Session on suspend (with a {@code null}
	 * argument) and setting a fresh Session on resume.
	 */
	protected void setSession(Session session) {
		if (this.currentSession != null) {
			this.sessionHandle.releaseSession(this.currentSession);
			this.currentSession = null;
		}
		if (session != null) {
			this.sessionHandle = new SimpleSessionHandle(session);
		}
		else {
			this.sessionHandle = null;
		}
	}

	/**
	 * Return the current Session held by this SessionHolder.
	 * <p>This will be the same Session until {@code released}
	 * gets called on the SessionHolder, which will reset the
	 * held Session, fetching a new Session on demand.
	 * @see SessionHandle#getSession()
	 * @see #released()
	 */
	public Session getSession() {
		Assert.notNull(this.sessionHandle, "Active Session is required");
		if (this.currentSession == null) {
			this.currentSession = this.sessionHandle.getSession();
		}
		return this.currentSession;
	}

	/**
	 * Releases the current Session held by this SessionHolder.
	 * <p>This is necessary for SessionHandles that expect "Session borrowing",
	 * where each returned Session is only temporarily leased and needs to be
	 * returned once the data operation is done, to make the Session available
	 * for other operations within the same transaction.
	 */
	@Override
	public void released() {
		super.released();
		if (!isOpen() && this.currentSession != null) {
			this.sessionHandle.releaseSession(this.currentSession);
			this.currentSession = null;
		}
	}


	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}

}
