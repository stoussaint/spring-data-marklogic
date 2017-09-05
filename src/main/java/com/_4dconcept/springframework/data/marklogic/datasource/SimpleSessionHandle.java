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

import com.marklogic.xcc.Session;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link SessionHandle} interface,
 * containing a given XDBC Session.
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 */
public class SimpleSessionHandle implements SessionHandle {

    private final Session session;


    /**
     * Create a new SimpleSessionHandle for the given Session.
     * @param session the XDBC Session
     */
    public SimpleSessionHandle(Session session) {
        Assert.notNull(session, "Session must not be null");
        this.session = session;
    }

    /**
     * Return the specified Session as-is.
     */
    @Override
    public Session getSession() {
        return this.session;
    }

    /**
     * This implementation is empty, as we're using a standard
     * Session handle that does not have to be released.
     */
    @Override
    public void releaseSession(Session ses) {
    }


    @Override
    public String toString() {
        return "SimpleSessionHandle: " + this.session;
    }

}
