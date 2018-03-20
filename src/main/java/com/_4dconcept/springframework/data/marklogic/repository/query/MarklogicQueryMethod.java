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
package com._4dconcept.springframework.data.marklogic.repository.query;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

/**
 * Marklogic specific implementation of {@link QueryMethod}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicQueryMethod extends QueryMethod {

    /**
     * Creates a new {@link MarklogicQueryMethod} from the given {@link Method}.
     *
     * @param method must not be {@literal null}.
     * @param metadata must not be {@literal null}.
     * @param projectionFactory must not be {@literal null}.
     */
    public MarklogicQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory) {
        super(method, metadata, projectionFactory);
    }

}
