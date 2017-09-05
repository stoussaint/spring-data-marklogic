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
 * Subinterface of {@link Session} to be implemented by
 * Session proxies. Allows access to the underlying target Session.
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 *
 * @see TransactionAwareContentSourceProxy
 * @see LazySessionContentSourceProxy
 * @see ContentSourceUtils#getTargetSession(Session)
 */
public interface SessionProxy extends Session {

    /**
     * Return the target Session of this proxy.
     * <p>This will typically be the native driver Session
     * or a wrapper from a session pool.
     *
     * @return the underlying Session (never {@code null})
     */
    Session getTargetSession();

}
