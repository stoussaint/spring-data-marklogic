package org.springframework.data.marklogic.datasource;

import com.marklogic.xcc.Session;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link SessionHandle} interface,
 * containing a given XDBC Session.
 *
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 */
public class SimpleSessionHandle implements SessionHandle {

	private final Session session;


	/**
	 * Create a new SimpleSessionHandle for the given Session.
	 * @param session the XDBC Session
	 */
	public SimpleSessionHandle(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * Return the specified Session as-is.
	 */
	@Override
	public Session getSession() {
		return this.session;
	}

	/**
	 * This implementation is empty, as we're using a standard
	 * Session handle that does not have to be released.
	 */
	@Override
	public void releaseSession(Session ses) {
	}


	@Override
	public String toString() {
		return "SimpleSessionHandle: " + this.session;
	}

}
