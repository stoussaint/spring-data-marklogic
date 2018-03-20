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

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.lang.Nullable;

/**
 * {@link MarklogicEntityInformation} implementation using a {@link MarklogicPersistentEntity} instance to lookup the necessary
 * information. Can be configured with a custom uri or default collection to be returned which will override the one returned by the
 * {@link MarklogicPersistentEntity} if given.
 *
 * @author Stéphane Toussaint
 */
public class MappingMarklogicEntityInformation<T, ID> extends PersistentEntityInformation<T, ID>
        implements MarklogicEntityInformation<T, ID> {

    private final MarklogicPersistentEntity<T> entityMetadata;
    private final @Nullable String customUri;
    private final @Nullable String customDefaultCollection;

    public MappingMarklogicEntityInformation(MarklogicPersistentEntity<T> entity) {
        this(entity, null, null);
    }

    public MappingMarklogicEntityInformation(MarklogicPersistentEntity<T> entity, @Nullable String customUri, @Nullable String customDefaultCollection) {
        super(entity);

        this.entityMetadata = entity;
        this.customUri = customUri;
        this.customDefaultCollection = customDefaultCollection;
    }

    @Override
    public String getUri() {
        return customUri == null ? entityMetadata.getUri() : customUri;
    }

    @Override
    public String getDefaultCollection() {
        return customDefaultCollection == null ? entityMetadata.getDefaultCollection() : customDefaultCollection;
    }

    @Override
    public boolean idInPropertyFragment() {
        return entityMetadata.idInPropertyFragment();
    }
}
