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
