package com._4dconcept.springframework.data.marklogic.core.mapping.event;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AfterRetrieveEvent<T> extends ApplicationEvent {

    private String uri;
    private Map<Object, Object> params;

    public AfterRetrieveEvent(T source, String uri) {
        super(source);
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public Map<Object, Object> getParams() {
        return params;
    }

    public void setParams(Map<Object, Object> params) {
        this.params = params;
    }
}
