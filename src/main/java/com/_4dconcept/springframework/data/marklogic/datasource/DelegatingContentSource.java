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
import com.marklogic.xcc.Session;
import com.marklogic.xcc.spi.ConnectionProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.logging.Logger;

/**
 * XDBC {@link ContentSource} implementation that delegates all calls
 * to a given target {@link ContentSource}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #newSession()}) that should not simply
 * delegate to the target ContentSource.
 *
 * @author Stephane Toussaint
 * @author Juergen Hoeller
 * @see #newSession
 */
public class DelegatingContentSource implements ContentSource, InitializingBean {

    private ContentSource targetContentSource;


    /**
     * Create a new DelegatingContentSource.
     * @see #setTargetContentSource
     */
    public DelegatingContentSource() {
    }

    /**
     * Create a new DelegatingContentSource.
     * @param targetContentSource the target ContentSource
     */
    public DelegatingContentSource(ContentSource targetContentSource) {
        setTargetContentSource(targetContentSource);
    }

    /**
     * @return the target ContentSource that this ContentSource should delegate to.
     */
    public ContentSource getTargetContentSource() {
        return this.targetContentSource;
    }

    /**
     * @param targetContentSource the target ContentSource that this ContentSource should delegate to.
     */
    public void setTargetContentSource(ContentSource targetContentSource) {
        Assert.notNull(targetContentSource, "'targetContentSource' must not be null");
        this.targetContentSource = targetContentSource;
    }

    @Override
    public void afterPropertiesSet() {
        if (getTargetContentSource() == null) {
            throw new IllegalArgumentException("Property 'targetContentSource' is required");
        }
    }


    @Override
    public Session newSession() {
        return getTargetContentSource().newSession();
    }

    @Override
    public Session newSession(String contentbaseId) {
        return getTargetContentSource().newSession(contentbaseId);
    }

    @Override
    public Session newSession(String username, String password) {
        return getTargetContentSource().newSession(username, password);
    }

    @Override
    public Session newSession(String username, String password, String contentbaseId) {
        return getTargetContentSource().newSession(username, password, contentbaseId);
    }

    @Override
    public Logger getDefaultLogger() {
        return getTargetContentSource().getDefaultLogger();
    }

    @Override
    public void setDefaultLogger(Logger defaultLogger) {
        getTargetContentSource().setDefaultLogger(defaultLogger);
    }

    public boolean isAuthenticationPreemptive() {
        return getTargetContentSource().isAuthenticationPreemptive();
    }

    @Override
    public void setAuthenticationPreemptive(boolean isAuthenticationPreemptive) {
        getTargetContentSource().setAuthenticationPreemptive(isAuthenticationPreemptive);
    }

    @Override
    public ConnectionProvider getConnectionProvider() {
        return getTargetContentSource().getConnectionProvider();
    }

}
