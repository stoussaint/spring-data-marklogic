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
package com._4dconcept.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Default implementation of a {@link MappingContext} for Marklogic using {@link BasicMarklogicPersistentEntity} and
 * {@link MarklogicPersistentProperty} as primary abstractions.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicMappingContext extends AbstractMappingContext<BasicMarklogicPersistentEntity<?>, MarklogicPersistentProperty> {

    @Override
    protected <T> BasicMarklogicPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicMarklogicPersistentEntity<>(typeInformation);
    }

    @Override
    protected MarklogicPersistentProperty createPersistentProperty(Property property,
            BasicMarklogicPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new BasicMarklogicPersistentProperty(property, owner, simpleTypeHolder);
    }
}
