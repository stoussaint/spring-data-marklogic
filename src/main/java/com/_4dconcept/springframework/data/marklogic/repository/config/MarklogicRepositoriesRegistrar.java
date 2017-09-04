package com._4dconcept.springframework.data.marklogic.repository.config;

import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * {@link ImportBeanDefinitionRegistrar} to setup Marklogic repositories via {@link EnableMarklogicRepositories}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableMarklogicRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new MarklogicRepositoryConfigurationExtension();
    }

}