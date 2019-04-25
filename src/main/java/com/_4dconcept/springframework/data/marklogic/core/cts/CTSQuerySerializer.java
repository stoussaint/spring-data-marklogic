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
package com._4dconcept.springframework.data.marklogic.core.cts;

import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.core.query.SortCriteria;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Serialize a given {@link Query} as cts query expression (String)
 *
 * @author stoussaint
 * @since 2017-08-01
 */
public class CTSQuerySerializer {

    private Query query;
    private boolean disablePagination;

    public CTSQuerySerializer(Query query) {
        this.query = query;
    }

    public CTSQuerySerializer disablePagination() {
        this.disablePagination = true;
        return this;
    }

    public String asCtsQuery() {
        String limitPredicate = "";
        String collection = retrieveTargetCollection();

        if (!disablePagination && query.getLimit() > 0 && query.getSkip() >= 0) {
            limitPredicate = String.format("[%d to %d]", query.getSkip() + 1, query.getLimit());
        }
        return String.format("cts:search(%s, %s, %s)%s", collection, serializeCriteria(query.getCriteria()), buildOptions(), limitPredicate);
    }

    public String asCtsUris() {
        String collectionQuery = retrieveCollectionQuery();

        return String.format("cts:uris((), %s, cts:and-query((%s, %s)))", buildOptions(), collectionQuery, serializeCriteria(query.getCriteria()));
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

    private String retrieveCollectionQuery() {
        if (query.getCollection() == null) {
            return "cts:collection-query(())";
        }

        return String.format("cts:collection-query('%s')", query.getCollection());
    }

    private String handleSimpleValue(Criteria criteria) {
        QName qname = criteria.getQname();
        Object criteriaObject = criteria.getCriteriaObject();

        Assert.notNull(qname, "A criteria QName is expected");
        Assert.notNull(criteriaObject, "A criteria value is expected");

        if (criteriaObject instanceof String) {
            String escapedValue = ((String) criteriaObject).replaceAll("'", "''").replaceAll("&","&amp;");
            return String.format("cts:element-value-query(%s, '%s')", serializeQName(qname), escapedValue);
        } else {
            return String.format("cts:element-value-query(%s, '%s')", serializeQName(qname), criteriaObject);
        }
    }

    private String serializeCriteria(@Nullable Criteria criteria) {
        if (criteria != null) {
            if (criteria.getOperator() == null) {
                return handleSimpleValue(criteria);
            } else if (criteria.getOperator() == Criteria.Operator.NOT) {
                return String.format("cts:not-query(%s)", serializeCriteria((Criteria) criteria.getCriteriaObject()));
            } else if (criteria.getOperator() == Criteria.Operator.COLLECTION) {
                return String.format("cts:collection-query('%s')", criteria.getCriteriaObject());
            } else if (criteria.getOperator() == Criteria.Operator.PROPERTIES) {
                return String.format("cts:properties-fragment-query(%s)", serializeCriteria((Criteria) criteria.getCriteriaObject()));
            } else {
                List<Criteria> criteriaList = retrieveCriteriaList(criteria);
                String ctsQueries = criteriaList.stream().map(this::serializeCriteria).collect(Collectors.joining(", "));

                if (criteria.getOperator() == Criteria.Operator.AND) {
                    return String.format("cts:and-query((%s))", ctsQueries);
                } else if (criteria.getOperator() == Criteria.Operator.OR) {
                    return String.format("cts:or-query((%s))", ctsQueries);
                }
            }
        }

        return "()";
    }

    private List<Criteria> retrieveCriteriaList(Criteria criteria) {
        Object criteriaObject = criteria.getCriteriaObject();

        if (criteriaObject instanceof List) {
            return ((List<?>) criteriaObject).stream().filter(o -> o instanceof Criteria).map(o -> (Criteria) o).collect(toList());
        }

        throw new IllegalArgumentException(String.format("Unexpected criteria type %s", criteria.getClass()));
    }

    private String serializeQName(QName qname) {
        return String.format("fn:QName('%s', '%s')", qname.getNamespaceURI(), qname.getLocalPart());
    }

    private String serializeSortCriteriaList(List<SortCriteria> sortCriteriaList) {
        return sortCriteriaList.stream().map(this::asCtsOrder).collect(Collectors.joining(", "));
    }

    private String asCtsOrder(SortCriteria sortCriteria) {
        return String.format("cts:index-order(cts:element-reference(%s), ('%s'))", serializeQName(sortCriteria.getQname()), sortCriteria.isDescending() ? "descending" : "ascending");
    }

}
