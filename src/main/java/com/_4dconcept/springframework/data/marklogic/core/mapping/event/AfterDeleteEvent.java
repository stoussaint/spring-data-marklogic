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
package com._4dconcept.springframework.data.marklogic.core.mapping.event;

/**
 * Event being thrown after the content is deleted
 *
 * @author Stéphane Toussaint
 */
public class AfterDeleteEvent<T> extends MarklogicMappingEvent<T> {

    private final transient Object id;

    /**
     * Creates new {@link AfterDeleteEvent}
     *
     * @param source the entity as before its deletion
     * @param id the identifier of the deleted entity
     * @param uri the location of the deleted entity
     */
    public AfterDeleteEvent(T source, Object id, String uri) {
        super(source, uri);
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
