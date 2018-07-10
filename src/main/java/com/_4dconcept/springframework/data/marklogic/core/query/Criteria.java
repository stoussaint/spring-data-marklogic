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
package com._4dconcept.springframework.data.marklogic.core.query;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Representation of a {@link Query} criteria.
 * Note that operators doesn't have provided QName
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Criteria implements CriteriaDefinition {

    public enum Operator {and, or, not, collection}

    private QName qname;
    private Object criteriaObject;
    private Operator operator;

    public Criteria() {
    }

    public Criteria(QName qname, Object criteriaObject) {
        this.qname = qname;
        this.criteriaObject = criteriaObject;
    }

    public Criteria(Operator operator, Object criteriaObject) {
        this.operator = operator;
        this.criteriaObject = criteriaObject;
    }

    /**
     * @return the qname
     */
    public QName getQname() {
        return qname;
    }

    /**
     * @param qname the qname to set
     */
    public void setQname(QName qname) {
        this.qname = qname;
    }

    /**
     * @return the criteriaObject
     */
    public Object getCriteriaObject() {
        return criteriaObject;
    }

    /**
     * @param criteriaObject the criteriaObject to set
     */
    public void setCriteriaObject(Object criteriaObject) {
        this.criteriaObject = criteriaObject;
    }

    /**
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * @param operator the operator to set
     */
    void setOperator(Operator operator) {
        this.operator = operator;
    }

    @SuppressWarnings("unchecked")
    public void add(Criteria criteria) {
        if (criteriaObject instanceof Collection<?>) {
            ((Collection<Criteria>)criteriaObject).add(criteria);
        }
    }

    @Override
    public String toString() {
        return "Criteria{" +
                "qname=" + qname +
                ", criteriaObject=" + criteriaObject +
                ", operator=" + operator +
                '}';
    }
}
