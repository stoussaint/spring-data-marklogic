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

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

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

    private String handleSimpleValue(Criteria criteria) {
        if (criteria.getCriteriaObject() instanceof String) {
            String escapedValue = criteria.getCriteriaObject().toString().replaceAll("'", "''").replaceAll("&","&amp;");
            return String.format("cts:element-value-query(%s, '%s')", serializeQName(criteria.getQname()), escapedValue);
        } else if (criteria.getCriteriaObject() != null) {
            return String.format("cts:element-value-query(%s, '%s')", serializeQName(criteria.getQname()), criteria.getCriteriaObject());
        }

        return "";
    }

    @SuppressWarnings("unchecked")
    private String serializeCriteria(Criteria criteria) {
        if (criteria != null) {
            if (criteria.getOperator() == null) {
                return handleSimpleValue(criteria);
            } else if (criteria.getOperator() == Criteria.Operator.NOT) {
                return String.format("cts:not-query(%s)", serializeCriteria((Criteria) criteria.getCriteriaObject()));
            } else if (criteria.getOperator() == Criteria.Operator.COLLECTION) {
                return String.format("cts:collection-query('%s')", criteria.getCriteriaObject());
            } else {
                List<Criteria> criteriaList = (List<Criteria>) criteria.getCriteriaObject();
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
