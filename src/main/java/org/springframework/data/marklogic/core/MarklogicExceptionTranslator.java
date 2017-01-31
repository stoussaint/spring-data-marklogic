package org.springframework.data.marklogic.core;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class MarklogicExceptionTranslator implements PersistenceExceptionTranslator {

    private static final Set<String> RESOURCE_FAILURE_EXCEPTIONS = new HashSet<String>(
            Arrays.asList("ServerConnectionException", "ServerResponseException", "XccConfigException"));

    private static final Set<String> RESOURCE_USAGE_EXCEPTIONS = new HashSet<String>(
            Arrays.asList("StreamingResultException", "UnexpectedResponseException", "XccException"));

    private static final Set<String> DATA_INTEGRETY_EXCEPTIONS = new HashSet<String>(
            Arrays.asList("ContentInsertEntityException", "ContentInsertException"));

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

        // Check for well-known MarklogicException subclasses.

        String exception = ClassUtils.getShortName(ClassUtils.getUserClass(ex.getClass()));

        if (RESOURCE_FAILURE_EXCEPTIONS.contains(exception)) {
            return new DataAccessResourceFailureException(ex.getMessage(), ex);
        }

        if (RESOURCE_USAGE_EXCEPTIONS.contains(exception)) {
            return new InvalidDataAccessResourceUsageException(ex.getMessage(), ex);
        }

        if (DATA_INTEGRETY_EXCEPTIONS.contains(exception)) {
            return new DataIntegrityViolationException(ex.getMessage(), ex);
        }

        return null;
    }
}
