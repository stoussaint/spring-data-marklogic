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

/**
 * A {@link Criteria} used for entity result order
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
