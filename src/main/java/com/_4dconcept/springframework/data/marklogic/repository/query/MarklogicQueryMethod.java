package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * Marklogic specific implementation of {@link QueryMethod}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicQueryMethod extends QueryMethod {

    private final Method method;
    private final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    /**
     * Creates a new {@link MarklogicQueryMethod} from the given {@link Method}.
     *
     * @param method must not be {@literal null}.
     * @param metadata must not be {@literal null}.
     * @param projectionFactory must not be {@literal null}.
     * @param mappingContext must not be {@literal null}.
     */
    public MarklogicQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory projectionFactory,
                            MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {

        super(method, metadata, projectionFactory);

        Assert.notNull(mappingContext, "MappingContext must not be null!");

        this.method = method;
        this.mappingContext = mappingContext;
    }

}
