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

import com.marklogic.xcc.Content;

/**
 * Event being thrown after content is saved
 *
 * @author Stéphane Toussaint
 */
public class AfterSaveEvent<T> extends MarklogicMappingEvent<T> {

    /**
     * Creates new {@link AfterSaveEvent}
     *
     * @param source The stored domain object
     * @param content The content saved
     * @param uri The uri the content is saved
     */
    public AfterSaveEvent(T source, Content content, String uri) {
        super(source, content, uri);
    }
}
