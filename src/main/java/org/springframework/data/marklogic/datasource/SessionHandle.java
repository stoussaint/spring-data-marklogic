package org.springframework.data.marklogic.datasource;

import com.marklogic.xcc.Session;

/**
 * Simple interface to be implemented by handles for a XDBC Session.
 *
 * @author Juergen Hoeller
 * @author Stephane Toussaint
 * @see SimpleSessionHandle
 * @see SessionHolder
 */
public interface SessionHandle {

    /**
     * Fetch the XDBC Session that this handle refers to.
     */
    Session getSession();

    /**
     * Release the XDBC Session that this handle refers to.
     *
     * @param ses the XDBC Session to release
     */
    void releaseSession(Session ses);

}