package com._4dconcept.springframework.data.marklogic.config;

import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractMarklogicConfigurationTest {

    @Test
    public void marklogicContentSource_WithWellDefineURI_ReturnsContentSource() throws Exception {
        TestAbstractMarklogicConfiguration configuration = new TestAbstractMarklogicConfiguration("xdbc://user:password@localhost:8888");
        ContentSource contentSource = configuration.marklogicContentSource().getObject();

        assertThat(contentSource, notNullValue());
        assertThat(configuration.getMarklogicUri().toString(), is("xdbc://user:password@localhost:8888"));
    }

    @Test
    public void marklogicTemplate_WithContentSource_ReturnsMarklogicTemplateAndTriggerBeforeAfterMethods() throws Exception {
        TestAbstractMarklogicConfiguration configuration = new TestAbstractMarklogicConfiguration("xdbc://user:password@localhost:8888");
        MarklogicTemplate marklogicTemplate = configuration.marklogicTemplate(ContentSourceFactory.newContentSource(URI.create("xdbc://user:password@localhost:8888")));

        assertThat(marklogicTemplate, notNullValue());
        assertThat(configuration.beforeMarklogicTemplateCreationCalled, is(true));
        assertThat(configuration.afterMarklogicTemplateCreation, is(true));
    }

    @Test
    public void getMappingBasePackage__ReturnsCurrentPackage() {
        TestAbstractMarklogicConfiguration configuration = new TestAbstractMarklogicConfiguration("xdbc://user:password@localhost:8888");
        String mappingBasePackage = configuration.getMappingBasePackage();

        assertThat(mappingBasePackage, is(this.getClass().getPackage().getName()));
    }

    private class TestAbstractMarklogicConfiguration extends AbstractMarklogicConfiguration {

        private boolean beforeMarklogicTemplateCreationCalled;
        private boolean afterMarklogicTemplateCreation;
        private String uri;

        TestAbstractMarklogicConfiguration(String uri) {
            this.uri = uri;
        }

        @Override
        protected URI getMarklogicUri() {
            return URI.create(uri);
        }

        @Override
        protected void beforeMarklogicTemplateCreation(ContentSource contentSource) {
            assertThat(contentSource, notNullValue());
            beforeMarklogicTemplateCreationCalled = true;
        }

        @Override
        protected void afterMarklogicTemplateCreation(MarklogicTemplate marklogicTemplate) {
            assertThat(marklogicTemplate, notNullValue());
            afterMarklogicTemplateCreation = true;
        }
    }

}