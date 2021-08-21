package com._4dconcept.springframework.data.marklogic.core.mapping.event;

import java.util.Map;

public class AfterRetrieveEvent<T> extends MarklogicMappingEvent<T> {

    private Map<Object, Object> params;

    public AfterRetrieveEvent(T source, String uri) {
        super(source, uri);
    }

    public Map<Object, Object> getParams() {
        return params;
    }

    public void setParams(Map<Object, Object> params) {
        this.params = params;
    }
}
