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
package com._4dconcept.springframework.data.marklogic.core;

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
 * Simple {@link PersistenceExceptionTranslator} for Marklogic. Convert the given runtime exception to an appropriate
 * exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicExceptionTranslator implements PersistenceExceptionTranslator {

    private static final Set<String> RESOURCE_FAILURE_EXCEPTIONS = new HashSet<>(
            Arrays.asList("ServerConnectionException", "ServerResponseException", "XccConfigException"));

    private static final Set<String> RESOURCE_USAGE_EXCEPTIONS = new HashSet<>(
            Arrays.asList("StreamingResultException", "UnexpectedResponseException", "XccException"));

    private static final Set<String> DATA_INTEGRETY_EXCEPTIONS = new HashSet<>(
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
