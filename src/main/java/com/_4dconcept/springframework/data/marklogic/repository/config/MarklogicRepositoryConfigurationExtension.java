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

import com._4dconcept.springframework.data.marklogic.config.BeanNames;
import com._4dconcept.springframework.data.marklogic.core.mapping.Document;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.support.MarklogicRepositoryFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.config.*;
import org.w3c.dom.Element;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

/**
 * {@link RepositoryConfigurationExtension} for Marklogic.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    private static final String MARKLOGIC_TEMPLATE_REF = "marklogic-template-ref";

    private boolean fallbackMappingContextCreated = false;

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    protected String getModulePrefix() {
        return "marklogic";
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    public String getRepositoryFactoryClassName() {
        return MarklogicRepositoryFactoryBean.class.getName();
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(Document.class);
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(MarklogicRepository.class);
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        if (fallbackMappingContextCreated) {
            builder.addPropertyReference("mappingContext", BeanNames.MAPPING_CONTEXT_BEAN_NAME);
        }
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

        Element element = config.getElement();
        ParsingUtils.setPropertyReference(builder, element, MARKLOGIC_TEMPLATE_REF, "marklogicOperations");
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
        AnnotationAttributes attributes = config.getAttributes();

        builder.addPropertyReference("marklogicOperations", attributes.getString("marklogicTemplateRef"));
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {
        super.registerBeansForRoot(registry, configurationSource);

        if (!registry.containsBeanDefinition(BeanNames.MAPPING_CONTEXT_BEAN_NAME)) {
            RootBeanDefinition definition = new RootBeanDefinition(MarklogicMappingContext.class);
            definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            definition.setSource(configurationSource.getSource());

            registry.registerBeanDefinition(BeanNames.MAPPING_CONTEXT_BEAN_NAME, definition);
        }
    }
}
