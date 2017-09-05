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

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * Marklogic specific implementation of {@link QueryMethod}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicQueryMethod extends QueryMethod {

    private final Method method;
    private final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    /**
     * Creates a new {@link MarklogicQueryMethod} from the given {@link Method}.
     *
     * @param method must not be {@literal null}.
     * @param metadata must not be {@literal null}.
     * @param projectionFactory must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public MarklogicQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
                            MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {

        super(method, metadata, projectionFactory);

        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.method = method;
        this.mappingContext = mappingContext;
    }

}
