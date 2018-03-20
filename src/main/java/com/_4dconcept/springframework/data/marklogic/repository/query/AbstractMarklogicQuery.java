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

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Marklogic.
 *
 * @author Stéphane Toussaint
 */
public abstract class AbstractMarklogicQuery implements RepositoryQuery {

    private final MarklogicQueryMethod method;
    private final MarklogicOperations operations;

    /**
     * Creates a new {@link AbstractMarklogicQuery} from the given {@link MarklogicQueryMethod} and {@link MarklogicOperations}.
     *
     * @param method     must not be {@literal null}.
     * @param operations must not be {@literal null}.
     */
    AbstractMarklogicQuery(MarklogicQueryMethod method, MarklogicOperations operations) {
        Assert.notNull(operations, "MarklogicOperations must not be null!");
        Assert.notNull(method, "MarklogicQueryMethod must not be null!");

        this.method = method;
        this.operations = operations;
    }

    public MarklogicQueryMethod getQueryMethod() {
        return method;
    }

    public Object execute(Object[] parameters) {
        ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), parameters);
        Query query = createQuery(accessor);

        ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);

        if (isDeleteQuery()) {
//            operations.remove(query);
            return null;
        } else if (method.isCollectionQuery()) {
            return operations.find(query, processor.getReturnedType().getDomainType());
        } else {
            return operations.findOne(query, processor.getReturnedType().getDomainType());
        }

    }

    /**
     * Creates a {@link Query} instance using the given {@link ParameterAccessor}
     *
     * @param accessor must not be {@literal null}.
     * @return a created Query instance
     */
    protected abstract Query createQuery(ParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

}
