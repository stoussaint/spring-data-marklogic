package org.springframework.data.marklogic.config;

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
import org.springframework.data.marklogic.core.MarklogicFactoryBean;
import org.springframework.data.marklogic.core.MarklogicTemplate;
import org.springframework.data.marklogic.core.convert.MappingMarklogicConverter;
import org.springframework.data.marklogic.core.mapping.Document;
import org.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.springframework.data.support.CachingIsNewStrategyFactory;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

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

    protected void beforeMarklogicTemplateCreation(ContentSource contentSource) {};

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
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    public MarklogicMappingContext marklogicMappingContext() throws ClassNotFoundException  {
        MarklogicMappingContext marklogicMappingContext = new MarklogicMappingContext();
        marklogicMappingContext.setInitialEntitySet(getInitialEntitySet());
        return marklogicMappingContext;
    }

    /**
     * Returns a {@link MappingContextIsNewStrategyFactory} wrapped into a {@link CachingIsNewStrategyFactory}.
     * @return
     * @throws ClassNotFoundException
     */
    @Bean
    public IsNewStrategyFactory isNewStrategyFactory() throws ClassNotFoundException {
        PersistentEntities persistentEntities = new PersistentEntities(Arrays.asList(marklogicMappingContext()));
        return new CachingIsNewStrategyFactory(new MappingContextIsNewStrategyFactory(persistentEntities));
    }

    /**
     * Creates a {@link MappingMarklogicConverter} using the configured {@link #marklogicMappingContext()}.
     * @return
     * @throws Exception
     */
    @Bean
    public MappingMarklogicConverter mappingMarklogicConverter() throws Exception {
        MappingMarklogicConverter converter = new MappingMarklogicConverter(marklogicMappingContext(), conversionService);
        converter.setConverters(getConverters());
        return converter;
    }

    protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
        String basePackage = getMappingBasePackage();
        Set<Class<?>> initialEntitySet = new HashSet<>();

        if (StringUtils.hasText(basePackage)) {
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));

            for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
                initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
                        AbstractMarklogicConfiguration.class.getClassLoader()));
            }
        }

        return initialEntitySet;
    }

    protected List<Object> getConverters() {
        return Collections.emptyList();
    }

}