package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.Session;

/**
 * Subinterface of {@link Session} to be implemented by
 * Session proxies. Allows access to the underlying target Session.
 *
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 * @see TransactionAwareContentSourceProxy
 * @see LazySessionContentSourceProxy
 * @see ContentSourceUtils#getTargetSession(Session)
 */
public interface SessionProxy extends Session {

	/**
	 * Return the target Session of this proxy.
	 * <p>This will typically be the native driver Session
	 * or a wrapper from a session pool.
	 * @return the underlying Session (never {@code null})
	 */
	Session getTargetSession();

}
