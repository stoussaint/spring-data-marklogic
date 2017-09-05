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
package com._4dconcept.springframework.data.marklogic.config;

import com._4dconcept.springframework.data.marklogic.core.MarklogicFactoryBean;
import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.Document;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com.marklogic.xcc.ContentSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.support.CachingIsNewStrategyFactory;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for Spring Data Marklogic configuration using JavaConfig.
 *
 * @author Stéphane Toussaint
 */
@Configuration
public abstract class AbstractMarklogicConfiguration {

    @Autowired(required = false)
    private GenericConversionService conversionService;

    /**
     * Return the name of the database to connect to.
     * @return must not be {@literal null}.
     */
    protected abstract String getDatabaseName();

    protected void beforeMarklogicTemplateCreation(ContentSource contentSource) {}

    protected void afterMarklogicTemplateCreation(MarklogicTemplate marklogicTemplate) {}

    @Bean
    public abstract MarklogicFactoryBean marklogicContentSource();

    @Bean
    public MarklogicTemplate marklogicTemplate(ContentSource contentSource) throws Exception {
        beforeMarklogicTemplateCreation(contentSource);
        MarklogicTemplate marklogicTemplate = new MarklogicTemplate(contentSource, mappingMarklogicConverter());
        afterMarklogicTemplateCreation(marklogicTemplate);
        return marklogicTemplate;
    }

    /**
     * Return the base package to scan for mapped {@link Document}s. Will return the package name of the configuration
     * class' (the concrete class, not this one here) by default. So if you have a {@code com.acme.AppConfig} extending
     * {@link AbstractMarklogicConfiguration} the base package will be considered {@code com.acme} unless the method is
     * overriden to implement alternate behaviour.
     *
     * @return the base package to scan for mapped {@link Document} classes or {@literal null} to not enable scanning for
     *         entities.
     */
    protected String getMappingBasePackage() {
        Package mappingBasePackage = getClass().getPackage();
        return mappingBasePackage == null ? null : mappingBasePackage.getName();
    }

    /**
     * Creates a {@link MarklogicMappingContext} equipped with entity classes scanned from the mapping base package.
     *
     * @see #getMappingBasePackage()
     * @return the initialized Marklogic mapping context
     */
    @Bean
    public MarklogicMappingContext marklogicMappingContext() {
        MarklogicMappingContext marklogicMappingContext = new MarklogicMappingContext();
        marklogicMappingContext.setInitialEntitySet(getInitialEntitySet());
        return marklogicMappingContext;
    }

    /**
     * @return a {@link MappingContextIsNewStrategyFactory} wrapped into a {@link CachingIsNewStrategyFactory}.
     */
    @Bean
    public IsNewStrategyFactory isNewStrategyFactory() {
        PersistentEntities persistentEntities = new PersistentEntities(Collections.singletonList(marklogicMappingContext()));
        return new CachingIsNewStrategyFactory(new MappingContextIsNewStrategyFactory(persistentEntities));
    }

    /**
     * Creates a {@link MarklogicMappingConverter} using the configured {@link #marklogicMappingContext()}.
     * @return the prepared marklogic mapping converter
     */
    @Bean
    public MarklogicMappingConverter mappingMarklogicConverter() {
        MarklogicMappingConverter converter = new MarklogicMappingConverter(marklogicMappingContext(), conversionService);
        converter.setConverters(getConverters());
        return converter;
    }

    private Set<Class<?>> getInitialEntitySet() {
        String basePackage = getMappingBasePackage();
        Set<Class<?>> initialEntitySet = new HashSet<>();

        if (StringUtils.hasText(basePackage)) {
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));

            for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
                try {
                    initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
                            AbstractMarklogicConfiguration.class.getClassLoader()));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return initialEntitySet;
    }

    protected List<Object> getConverters() {
        return Collections.emptyList();
    }

}