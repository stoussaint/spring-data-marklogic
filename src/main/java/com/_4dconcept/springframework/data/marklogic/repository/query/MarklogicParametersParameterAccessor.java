package com._4dconcept.springframework.data.marklogic.repository.query;

import org.springframework.data.repository.query.ParametersParameterAccessor;

import java.util.Arrays;
import java.util.List;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-03
 */
public class MarklogicParametersParameterAccessor extends ParametersParameterAccessor {

    private final MarklogicQueryMethod method;
    private final List<Object> values;

    public MarklogicParametersParameterAccessor(MarklogicQueryMethod method, Object[] values) {
        super(method.getParameters(), values);

        this.method = method;
        this.values = Arrays.asList(values);
    }

}
