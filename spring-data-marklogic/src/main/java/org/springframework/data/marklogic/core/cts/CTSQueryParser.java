package org.springframework.data.marklogic.core.cts;

import org.springframework.data.marklogic.core.query.Criteria;
import org.springframework.data.marklogic.core.query.Query;
import org.springframework.data.marklogic.core.query.SortCriteria;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-01
 */
public class CTSQueryParser {

    private Query query;
    private boolean disablePagination;

    public CTSQueryParser(Query query) {
        this.query = query;
    }

    public CTSQueryParser disablePagination() {
        this.disablePagination = true;
        return this;
    }

    public String asCtsQuery() {
        String limitPredicate = "";
        String collection = retrieveTargetCollection();

        if (!disablePagination && query.getLimit() > 0 && query.getSkip() >= 0) {
            limitPredicate = String.format("[%d to %d]", query.getSkip() + 1, query.getLimit());
        }
        return String.format("cts:search(%s, %s, %s)%s", collection, serializeCriteriaList(query.getCriteria()), buildOptions(), limitPredicate);
    }

    private String buildOptions() {
        String sortOptions = serializeSortCriteriaList(query.getSortCriteria());
        return String.format("(%s)", sortOptions);
    }

    private String retrieveTargetCollection() {
        if (query.getCollection() == null) {
            return "fn:collection()";
        }

        return String.format("fn:collection('%s')", query.getCollection());
    }

    private String asCtsQuery(Criteria criteria) {
        Object value = criteria.getValue();
        if (value instanceof List) {
            return handleListCriteria(criteria, value);
        } else {
            return handleSimpleValue(criteria, value);
        }
    }

    @SuppressWarnings("unchecked")
    private String handleListCriteria(Criteria criteria, Object value) {
        List<Criteria> criteriaList = (List<Criteria>)value;
        return String.format("cts:element-query(%s, %s)", serializeQName(criteria.getQname()), serializeCriteriaList(criteriaList));
    }

    private String handleSimpleValue(Criteria criteria, Object value) {
        return String.format("cts:element-value-query(%s, '%s')", serializeQName(criteria.getQname()), value);
    }

    private String serializeCriteriaList(List<Criteria> criteriaList) {
        if (criteriaList != null) {
            String ctsQueries = criteriaList.stream().map(this::asCtsQuery).collect(Collectors.joining(", "));
            return String.format("cts:and-query((%s))", ctsQueries);
        }

        return "()";
    }

    private String serializeQName(QName qname) {
        return String.format("fn:QName('%s', '%s')", qname.getNamespaceURI(), qname.getLocalPart());
    }

    private String serializeSortCriteriaList(List<SortCriteria> sortCriteriaList) {
        if (sortCriteriaList != null) {
            return sortCriteriaList.stream().map(this::asCtsOrder).collect(Collectors.joining(", "));
        }

        return "";
    }

    private String asCtsOrder(SortCriteria sortCriteria) {
        return String.format("cts:index-order(cts:element-reference(%s), ('%s'))", serializeQName(sortCriteria.getQname()), sortCriteria.isDescending() ? "descending" : "ascending");
    }

}
