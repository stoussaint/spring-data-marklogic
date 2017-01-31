package org.springframework.data.marklogic.repository.config;

import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.impl.ContentSourceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.marklogic.core.MarklogicOperations;
import org.springframework.data.marklogic.core.MarklogicTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link MarklogicRepositoriesRegistrar}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MarklogicRepositoriesRegistrarTest {

    @Configuration
    @EnableMarklogicRepositories(basePackages = "org.springframework.data.marklogic.repository")
    static class Config {

        @Bean
        public MarklogicOperations marklogicTemplate() throws Exception {
            return new MarklogicTemplate(ContentSourceFactory.newContentSource("localhost", 8888));
        }
    }

    @Test
    public void testConfiguration() {}

}