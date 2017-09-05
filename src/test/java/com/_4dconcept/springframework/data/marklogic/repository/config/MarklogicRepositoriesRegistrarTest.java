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
package com._4dconcept.springframework.data.marklogic.repository.config;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com.marklogic.xcc.ContentSourceFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    @EnableMarklogicRepositories(basePackages = "com._4dconcept.springframework.data.marklogic.repository")
    static class Config {

        @Bean
        public MarklogicOperations marklogicTemplate() throws Exception {
            return new MarklogicTemplate(ContentSourceFactory.newContentSource("localhost", 8888));
        }
    }

    @Test
    public void testConfiguration() {}

}