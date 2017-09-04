package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.exceptions.XccException;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Fatal exception thrown when we can't connect to an Marklogic server using XDBC.
 *
 * @author Rod Johnson
 * @author Stephane Toussaint
 */
@SuppressWarnings("serial")
public class CannotGetXdbcSessionException extends DataAccessResourceFailureException {

    /**
     * Constructor for CannotGetXdbcSessionException.
     * @param msg the detail message
     * @param ex XccException root cause
     */
    public CannotGetXdbcSessionException(String msg, XccException ex) {
        super(msg, ex);
    }
}
