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
 * Event being thrown before a domain object is converted to be persisted
 *
 * @author Stéphane Toussaint
 */
public class BeforeConvertEvent<T> extends MarklogicMappingEvent<T> {

    /**
     * Create new {@link BeforeConvertEvent}
     * @param source The domain object to be converted
     * @param uri The location the content will be persisted
     */
    public BeforeConvertEvent(T source, String uri) {
        super(source, null, uri);
    }
}
