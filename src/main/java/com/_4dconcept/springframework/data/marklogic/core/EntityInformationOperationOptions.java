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

import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;

/**
 * This class use entityInformation to resolve operation options values
 *
 * @author Stéphane Toussaint
 */
public class EntityInformationOperationOptions implements MarklogicCreateOperationOptions {

    private MarklogicEntityInformation entityInformation;

    private String[] extraCollections;

    public EntityInformationOperationOptions(MarklogicEntityInformation entityInformation) {
        this(entityInformation, null);
    }

    public EntityInformationOperationOptions(MarklogicEntityInformation entityInformation, String[] extraCollections) {
        this.entityInformation = entityInformation;
        this.extraCollections = extraCollections;
    }

    @Override
    public String defaultCollection() {
        return entityInformation.getDefaultCollection();
    }

    @Override
    public String uri() {
        return entityInformation.getUri();
    }

    @Override
    public String[] extraCollections() {
        return extraCollections;
    }

    @Override
    public boolean idInPropertyFragment() {
        return entityInformation.idInPropertyFragment();
    }

    @Override
    public Class entityClass() {
        return entityInformation.getJavaType();
    }
}
