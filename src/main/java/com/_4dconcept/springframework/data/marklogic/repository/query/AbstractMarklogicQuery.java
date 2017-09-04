package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Marklogic.
 *
 * @author Stéphane Toussaint
 */
public abstract class AbstractMarklogicQuery implements RepositoryQuery {

    private final MarklogicQueryMethod method;
    private final MarklogicOperations operations;

    /**
     * Creates a new {@link AbstractMarklogicQuery} from the given {@link MarklogicQueryMethod} and {@link MarklogicOperations}.
     *
     * @param method     must not be {@literal null}.
     * @param operations must not be {@literal null}.
     */
    public AbstractMarklogicQuery(MarklogicQueryMethod method, MarklogicOperations operations) {
        Assert.notNull(operations, "MarklogicOperations must not be null!");
        Assert.notNull(method, "MarklogicQueryMethod must not be null!");

        this.method = method;
        this.operations = operations;
    }

    public MarklogicQueryMethod getQueryMethod() {
        return method;
    }

    public Object execute(Object[] parameters) {
        ParameterAccessor accessor = new MarklogicParametersParameterAccessor(method, parameters);
        Query query = createQuery(accessor);

        ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);

        if (isDeleteQuery()) {
//            operations.remove(query);
            return null;
        } else if (method.isCollectionQuery()) {
            return operations.find(query, processor.getReturnedType().getDomainType());
        } else {
            return operations.findOne(query, processor.getReturnedType().getDomainType());
        }

    }

    /**
     * Creates a {@link Query} instance using the given {@link ParameterAccessor}
     *
     * @param accessor must not be {@literal null}.
     * @return
     */
    protected abstract Query createQuery(ParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

}
