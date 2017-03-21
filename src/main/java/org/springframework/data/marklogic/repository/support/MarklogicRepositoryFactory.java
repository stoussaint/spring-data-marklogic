package org.springframework.data.marklogic.repository.support;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarklogicInvokeOperationOptions;
import org.springframework.data.marklogic.core.MarklogicOperations;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.data.marklogic.repository.MarklogicRepository;
import org.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory to create {@link MarklogicRepository} instances.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicRepositoryFactory extends RepositoryFactorySupport {

    private MarklogicOperations marklogicOperations;
    private final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    public MarklogicRepositoryFactory(MarklogicOperations marklogicOperations) {

        Assert.notNull(marklogicOperations);

        this.marklogicOperations = marklogicOperations;
        this.mappingContext = marklogicOperations.getConverter().getMappingContext();
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleMarklogicRepository.class;
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
	 */
    @Override
    protected Object getTargetRepository(RepositoryInformation metadata) {
        MarklogicEntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());
        return getTargetRepositoryViaReflection(metadata, entityInformation, marklogicOperations);
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {
        return new MarklogicQueryLookupStrategy(marklogicOperations, evaluationContextProvider, mappingContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, ID extends Serializable> MarklogicEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        MarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(domainClass);
        return new MappingMarklogicEntityInformation<>((MarklogicPersistentEntity<T>) entity);
    }

    /**
     * {@link QueryLookupStrategy} to create {@link RepositoryQuery} instances.
     *
     * @author Oliver Gierke
     * @author Thomas Darimont
     */
    private static class MarklogicQueryLookupStrategy implements QueryLookupStrategy {

        private final MarklogicOperations operations;
        private final EvaluationContextProvider evaluationContextProvider;
        MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

        public MarklogicQueryLookupStrategy(MarklogicOperations operations, EvaluationContextProvider evaluationContextProvider,
                                            MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {

            this.operations = operations;
            this.evaluationContextProvider = evaluationContextProvider;
            this.mappingContext = mappingContext;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                            NamedQueries namedQueries) {

            QueryMethod queryMethod = new QueryMethod(method, metadata, factory);
            String namedQueryName = queryMethod.getNamedQueryName();

            if (namedQueries.hasQuery(namedQueryName)) {
                String moduleQueryUri = namedQueries.getQuery(namedQueryName);
                return new ModuleInvokeDelegateQuery(moduleQueryUri, queryMethod, operations);
            }

            return null;
        }
    }

    private static class ModuleInvokeDelegateQuery implements RepositoryQuery {

        private String moduleQueryUri;
        private QueryMethod queryMethod;
        private MarklogicOperations operations;

        ModuleInvokeDelegateQuery(String moduleQueryUri, QueryMethod queryMethod, MarklogicOperations operations) {
            this.moduleQueryUri = moduleQueryUri;
            this.queryMethod = queryMethod;
            this.operations = operations;
        }

        @Override
        public Object execute(Object[] parameters) {
            HashMap<Object, Object> params = new HashMap<>();
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    String paramName = queryMethod.getParameters().getParameter(i).getName();
                    params.put(paramName, parameters[i]);
                }
            }

            MarklogicInvokeOperationOptions invokeOperationOptions = new MarklogicInvokeOperationOptions() {
                @Override
                public Map<Object, Object> params() {
                    return params;
                }
            };

            if (queryMethod.isCollectionQuery()) {
                return operations.invokeModuleAsList(moduleQueryUri, queryMethod.getReturnedObjectType(), invokeOperationOptions);
            } else {
                return operations.invokeModule(moduleQueryUri, queryMethod.getReturnedObjectType(), invokeOperationOptions);
            }

        }

        @Override
        public QueryMethod getQueryMethod() {
            return queryMethod;
        }
    }

}