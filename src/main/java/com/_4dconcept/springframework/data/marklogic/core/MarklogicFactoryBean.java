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
package com._4dconcept.springframework.data.marklogic.core;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import java.net.URI;

/**
 * Convenient factory for configuring Marklogic ContentSource.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicFactoryBean extends AbstractFactoryBean<ContentSource> implements PersistenceExceptionTranslator {

    private static final PersistenceExceptionTranslator DEFAULT_EXCEPTION_TRANSLATOR = new MarklogicExceptionTranslator();

    private URI uri;
    private PersistenceExceptionTranslator exceptionTranslator = DEFAULT_EXCEPTION_TRANSLATOR;

    /**
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * @param exceptionTranslator the exceptionTranslator to set
     */
    public void setExceptionTranslator(PersistenceExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator == null ? DEFAULT_EXCEPTION_TRANSLATOR : exceptionTranslator;
    }

    @Override
    public Class<?> getObjectType() {
        return ContentSource.class;
    }

    @Override
    protected ContentSource createInstance() throws Exception {
        return ContentSourceFactory.newContentSource(uri);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }


}
