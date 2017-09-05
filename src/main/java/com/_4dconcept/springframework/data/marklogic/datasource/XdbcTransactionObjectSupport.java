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
package com._4dconcept.springframework.data.marklogic.datasource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * Convenient base class for XDBC-aware transaction objects.
 * Can contain a {@link SessionHolder}, and implements the
 * {@link org.springframework.transaction.SavepointManager}
 * interface based on that SessionHolder.
 *
 * @author Stéphane Toussaint
 * @author Juergen Hoeller
 */
public abstract class XdbcTransactionObjectSupport implements SmartTransactionObject {

    private static final Log logger = LogFactory.getLog(XdbcTransactionObjectSupport.class);


    private SessionHolder sessionHolder;

    private Integer previousIsolationLevel;

    public SessionHolder getSessionHolder() {
        return this.sessionHolder;
    }

    public void setSessionHolder(SessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    public boolean hasSessionHolder() {
        return (this.sessionHolder != null);
    }

    public Integer getPreviousIsolationLevel() {
        return this.previousIsolationLevel;
    }

    public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
        this.previousIsolationLevel = previousIsolationLevel;
    }

    @Override
    public void flush() {
        // no-op
    }

}