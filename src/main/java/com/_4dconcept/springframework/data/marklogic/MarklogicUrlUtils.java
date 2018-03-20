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
package com._4dconcept.springframework.data.marklogic;

/**
 * Helper class featuring helper methods for working with Marklogic urls.
 * Mainly intended for internal use within the framework.
 *
 * @author Stéphane Toussaint
 */
public abstract class MarklogicUrlUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private MarklogicUrlUtils() {}

    /**
     * Obtains the uri name to use for the provided class
     *
     * @param entityClass The class to determine the preferred uri name for
     * @return The preferred rul name
     */
    public static String getPreferredUrlPattern(Class<?> entityClass) {
        String URL_PREFIX = "/content/";
        String URL_SUFFIX = "/#{id}.xml";
        return URL_PREFIX + entityClass.getSimpleName().toLowerCase() + URL_SUFFIX;
    }
}
