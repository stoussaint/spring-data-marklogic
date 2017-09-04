package com._4dconcept.springframework.data.marklogic.core.query;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Criteria implements CriteriaDefinition {

    public enum Operator {and, or}

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
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @SuppressWarnings("unchecked")
    public void add(Criteria criteria) {
        if (criteriaObject instanceof Collection) {
            ((Collection<Criteria>)criteriaObject).add(criteria);
        }
    }
}
