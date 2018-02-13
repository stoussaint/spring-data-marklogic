package com._4dconcept.springframework.data.marklogic.core.mapping.event;

import org.springframework.context.ApplicationEvent;

public class AfterRetrieveEvent<T> extends ApplicationEvent {

    private String uri;

    public AfterRetrieveEvent(T source, String uri) {
        super(source);
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
}
