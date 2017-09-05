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

/**
 * Simple interface to be implemented by handles for a XDBC Session.
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 *
 * @see SimpleSessionHandle
 * @see SessionHolder
 */
public interface SessionHandle {

    /**
     * Fetch the XDBC Session that this handle refers to.
     *
     * @return the handled xdbc session
     */
    Session getSession();

    /**
     * Release the XDBC Session that this handle refers to.
     *
     * @param ses the XDBC Session to release
     */
    void releaseSession(Session ses);

}