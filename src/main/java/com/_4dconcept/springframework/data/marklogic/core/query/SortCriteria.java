package com._4dconcept.springframework.data.marklogic.core.query;

import javax.xml.namespace.QName;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-02
 */
public class SortCriteria {

    private QName qname;
    private boolean descending;

    public SortCriteria(QName qname) {
        this.qname = qname;
    }

    public SortCriteria(QName qname, boolean descending) {
        this.qname = qname;
        this.descending = descending;
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
     * @return the descending
     */
    public boolean isDescending() {
        return descending;
    }

    /**
     * @param descending the descending to set
     */
    public void setDescending(boolean descending) {
        this.descending = descending;
    }
}
