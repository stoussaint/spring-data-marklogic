package org.springframework.data.marklogic.repository.cdi;

import org.springframework.data.marklogic.core.MarklogicOperations;
import org.springframework.data.marklogic.repository.support.MarklogicRepositoryFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.util.Assert;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * {@link CdiRepositoryBean} to create Marklogic repository instances.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryBean<T> extends CdiRepositoryBean<T> {

    private final Bean<MarklogicOperations> operations;

    /**
     * Creates a new {@link MarklogicRepositoryBean}.
     *
     * @param operations must not be {@literal null}.
     * @param qualifiers must not be {@literal null}.
     * @param repositoryType must not be {@literal null}.
     * @param beanManager must not be {@literal null}.
     * @param detector detector for the custom {@link org.springframework.data.repository.Repository} implementations
     *          {@link CustomRepositoryImplementationDetector}, can be {@literal null}.
     */
    public MarklogicRepositoryBean(Bean<MarklogicOperations> operations, Set<Annotation> qualifiers, Class<T> repositoryType,
        BeanManager beanManager, CustomRepositoryImplementationDetector detector) {
        super(qualifiers, repositoryType, beanManager, detector);

        Assert.notNull(operations);
        this.operations = operations;
    }

    @Override
    protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType, Object customImplementation) {

        MarklogicOperations marklogicOperations = getDependencyInstance(operations, MarklogicOperations.class);
        MarklogicRepositoryFactory factory = new MarklogicRepositoryFactory(marklogicOperations);

        return factory.getRepository(repositoryType, customImplementation);
    }
}
