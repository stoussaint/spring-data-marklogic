package org.springframework.data.marklogic.core.query;

import javax.xml.namespace.QName;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Criteria {

    private QName qname;
    private Object value;

    public Criteria() {
    }

    public Criteria(QName qname, Object value) {
        this.qname = qname;
        this.value = value;
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
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

}
