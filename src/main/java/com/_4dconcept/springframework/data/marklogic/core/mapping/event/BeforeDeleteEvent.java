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

import org.springframework.context.ApplicationEvent;

/**
 * Event being thrown before content is deleted
 *
 * @author Stéphane Toussaint
 */
public class BeforeDeleteEvent<T> extends ApplicationEvent {

    private final String uri;
    private final transient Object id;

    /**
     * Creates new {@link BeforeDeleteEvent}
     *
     * @param source the entity to be deleted
     * @param id the identifier of the entity to delete
     * @param uri the location of the entity to delete
     */
    public BeforeDeleteEvent(T source, Object id, String uri) {
        super(source);
        this.id = id;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public Object getId() {
        return id;
    }

}
