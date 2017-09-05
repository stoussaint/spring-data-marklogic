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

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.spi.ConnectionProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.logging.Logger;

/**
 * Abstract base class for Spring's {@link ContentSource}
 * implementations, taking care of the padding.
 *
 * @author Stéphane Toussaint
 * @author Juergen Hoeller
 */
public abstract class AbstractContentSource implements ContentSource {

    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * DefaultLogger methods are not supported.
     */
    @Override
    public Logger getDefaultLogger() {
        throw new UnsupportedOperationException("getDefaultLogger");
    }

    /**
     * DefaultLogger methods are not supported.
     */
    @Override
    public void setDefaultLogger(Logger log) {
        throw new UnsupportedOperationException("setDefaultLogger");
    }

    @Override
    public ConnectionProvider getConnectionProvider() {
        throw new UnsupportedOperationException("getConnectionProvider");
    }

    @Override
    public boolean isAuthenticationPreemptive() {
        throw new UnsupportedOperationException("isAuthenticationPreemptive");
    }

    @Override
    public void setAuthenticationPreemptive(boolean b) {
        throw new UnsupportedOperationException("setAuthenticationPreemptive");
    }
}
