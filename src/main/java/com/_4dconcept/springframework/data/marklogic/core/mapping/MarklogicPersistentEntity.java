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

import org.springframework.data.mapping.PersistentEntity;

/**
 * Marklogic specific {@link PersistentEntity} abstraction.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicPersistentEntity<T> extends PersistentEntity<T, MarklogicPersistentProperty> {

    /**
     * @return the uri the entity shall be persisted to.
     */
    String getUri();

    /**
     * @return the default defaultCollection the entity should be created with
     */
    String getDefaultCollection();

    /**
     * @return true if the id is to be found in property fragment
     */
    boolean idInPropertyFragment();

}
