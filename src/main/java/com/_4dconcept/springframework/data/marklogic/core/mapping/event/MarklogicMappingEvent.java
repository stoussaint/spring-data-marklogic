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
import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;

/**
 * Base {@link ApplicationEvent} triggered by Spring Data Marklogic.
 *
 * @author Stéphane Toussaint
 */
public abstract class MarklogicMappingEvent<T> extends ApplicationEvent {

    private final String uri;
    private final @Nullable Content content;

    MarklogicMappingEvent(T source, @Nullable Content content, String uri) {
        super(source);
        this.content = content;
        this.uri = uri;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the content
     */
    @Nullable
    public Content getContent() {
        return content;
    }

    /*
     * (non-Javadoc)
     * @see java.util.EventObject#getSource()
     */
    @SuppressWarnings("unchecked")
    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
