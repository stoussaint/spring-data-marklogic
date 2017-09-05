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
import org.springframework.data.mapping.PersistentProperty;

import javax.xml.namespace.QName;

/**
 * Marklogic specific {@link org.springframework.data.mapping.PersistentProperty} extension.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicPersistentProperty extends PersistentProperty<MarklogicPersistentProperty> {

    /**
     * @return whether the property is explicitly marked as an identifier property of the owning {@link PersistentEntity}.
     */
    boolean isExplicitIdProperty();

    /**
     * @return the full qualified name of the property
     */
    QName getQName();

}