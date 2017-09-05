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

import com._4dconcept.springframework.data.marklogic.core.mapping.Document;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationSource;

import java.util.Collection;

import static org.junit.Assert.fail;

/**
 * Unit tests for {@link MarklogicRepositoryConfigurationExtension}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryConfigurationExtensionTest {

    StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(Config.class, true);
    ResourceLoader loader = new PathMatchingResourcePatternResolver();
    Environment environment = new StandardEnvironment();
    RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
            EnableMarklogicRepositories.class, loader, environment);

    @Test
    public void isStrictMatchIfDomainTypeIsAnnotatedWithDocument() {

        MarklogicRepositoryConfigurationExtension extension = new MarklogicRepositoryConfigurationExtension();
        assertHasRepo(SampleRepository.class, extension.getRepositoryConfigurations(configurationSource, loader, true));
    }

    @Test
    public void isStrictMatchIfRepositoryExtendsStoreSpecificBase() {

        MarklogicRepositoryConfigurationExtension extension = new MarklogicRepositoryConfigurationExtension();
        assertHasRepo(StoreRepository.class, extension.getRepositoryConfigurations(configurationSource, loader, true));
    }

    @Test
    public void isNotStrictMatchIfDomainTypeIsNotAnnotatedWithXmlRootElement() {

        MarklogicRepositoryConfigurationExtension extension = new MarklogicRepositoryConfigurationExtension();
        assertDoesNotHaveRepo(UnannotatedRepository.class,
                extension.getRepositoryConfigurations(configurationSource, loader, true));
    }

    private static void assertHasRepo(Class<?> repositoryInterface,
                                      Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

        for (RepositoryConfiguration<?> config : configs) {
            if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
                return;
            }
        }

        fail("Expected to find config for repository interface ".concat(repositoryInterface.getName()).concat(" but got ")
                .concat(configs.toString()));
    }

    private static void assertDoesNotHaveRepo(Class<?> repositoryInterface,
                                              Collection<RepositoryConfiguration<RepositoryConfigurationSource>> configs) {

        for (RepositoryConfiguration<?> config : configs) {
            if (config.getRepositoryInterface().equals(repositoryInterface.getName())) {
                fail("Expected not to find config for repository interface ".concat(repositoryInterface.getName()));
            }
        }
    }

    @EnableMarklogicRepositories(considerNestedRepositories = true)
    static class Config {

    }

    @Document
    static class Sample {}

    interface SampleRepository extends Repository<Sample, Long> {}

    interface UnannotatedRepository extends Repository<Object, Long> {}

    interface StoreRepository extends MarklogicRepository<Object, Long> {}

}