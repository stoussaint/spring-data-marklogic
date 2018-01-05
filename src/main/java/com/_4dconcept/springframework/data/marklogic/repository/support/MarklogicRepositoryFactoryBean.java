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
package com._4dconcept.springframework.data.marklogic.repository.support;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create {@link MarklogicRepository} instances.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
        RepositoryFactoryBeanSupport<T, S, ID> {

    private MarklogicOperations marklogicOperations;
    private boolean mappingContextConfigured = false;

    public MarklogicRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        RepositoryFactorySupport factory = getFactoryInstance(marklogicOperations);

        return factory;
    }

    /**
     * Configures the {@link MarklogicOperations} used for Marklogic data access operations.
     *
     * @param marklogicOperations {@link MarklogicOperations} used to perform CRUD, Query and general data access operations
     * on Marklogic.
     */
    public void setMarklogicOperations(MarklogicOperations marklogicOperations) {
        this.marklogicOperations = marklogicOperations;
    }

    protected RepositoryFactorySupport getFactoryInstance(MarklogicOperations operations) {
        return new MarklogicRepositoryFactory(operations);
    }

    @Override
    protected void setMappingContext(MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
        this.mappingContextConfigured = true;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        Assert.notNull(marklogicOperations, "MarklogicOperations must not be null!");

        if (!mappingContextConfigured) {
            setMappingContext(marklogicOperations.getConverter().getMappingContext());
        }
    }

}
