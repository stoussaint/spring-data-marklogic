package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * {@link RepositoryQuery} implementation for Marklogic.
 *
 * @author Stephane Toussaint
 */
public class PartTreeMarklogicQuery extends AbstractMarklogicQuery {

    private final PartTree tree;
    private final MappingContext<?, MarklogicPersistentProperty> context;

    public PartTreeMarklogicQuery(MarklogicQueryMethod method, MarklogicOperations marklogicOperations) {
        super(method, marklogicOperations);

        ResultProcessor processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(), processor.getReturnedType().getDomainType());
        this.context = marklogicOperations.getConverter().getMappingContext();
    }

    @Override
    protected Query createQuery(ParameterAccessor accessor) {
        MarklogicQueryCreator creator = new MarklogicQueryCreator(tree, accessor, context);
        Query query = creator.createQuery();

        if (tree.isLimiting()) {
            query.setLimit(tree.getMaxResults());
        }

        return query;
    }

    @Override
    protected boolean isDeleteQuery() {
        return tree.isDelete();
    }
}
