package org.springframework.data.marklogic.repository.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.marklogic.core.MarklogicOperations;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CDI extension to export Marklogic repositories.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryExtension extends CdiRepositoryExtensionSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarklogicRepositoryExtension.class);

    private final Map<Set<Annotation>, Bean<MarklogicOperations>> marklogicOperations = new HashMap<>();

    public MarklogicRepositoryExtension() {
        LOGGER.info("Activating CDI extension for Spring Data Marklogic repositories.");
    }

    @SuppressWarnings("unchecked")
    <X> void processBean(@Observes ProcessBean<X> processBean) {
        Bean<X> bean = processBean.getBean();

        for (Type type : bean.getTypes()) {
            if (type instanceof Class<?> && MarklogicOperations.class.isAssignableFrom((Class<?>) type)) {
                LOGGER.debug("Discovered {} with qualifiers {}.", MarklogicOperations.class.getName(), bean.getQualifiers());
            }

            marklogicOperations.put(new HashSet<>(bean.getQualifiers()), (Bean<MarklogicOperations>) bean);
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        for (Map.Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {
            Class<?> repositoryType = entry.getKey();
            Set<Annotation> qualifiers = entry.getValue();

            // Create the bean representing the repository.
            CdiRepositoryBean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);

            LOGGER.info("Registering bean for {} with qualifiers {}.", repositoryType.getName(), qualifiers);

            registerBean(repositoryBean);
            afterBeanDiscovery.addBean(repositoryBean);
        }
    }

    private <T> CdiRepositoryBean<T> createRepositoryBean(Class<T> repositoryType, Set<Annotation> qualifiers, BeanManager beanManager) {
        Bean<MarklogicOperations> marklogicOperations = this.marklogicOperations.get(qualifiers);

        if (marklogicOperations == null) {
            throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s.",
                    MarklogicOperations.class.getName(), qualifiers));
        }

        return new MarklogicRepositoryBean<>(marklogicOperations, qualifiers, repositoryType, beanManager, getCustomImplementationDetector());
    }


}
