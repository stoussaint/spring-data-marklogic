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
package com._4dconcept.springframework.data.marklogic.core.query;

import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Composition of {@link Criteria} that represent a Marklogic Query
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Query {

    private long skip;
    private int limit;
    private @Nullable String collection;
    private @Nullable Criteria criteria;
    private List<SortCriteria> sortCriteria;

    public Query() {
    }

    public Query(Criteria criteria) {
        this.criteria = criteria;
    }

    /**
     * @return the skip
     */
    public long getSkip() {
        return skip;
    }

    /**
     * @param skip the skip to set
     */
    public void setSkip(long skip) {
        this.skip = skip;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return the collection
     */
    @Nullable
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * @return the criteria
     */
    @Nullable
    public Criteria getCriteria() {
        return criteria;
    }

    /**
     * @param criteria the criteria to set
     */
    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    /**
     * @return the sortCriteria
     */
    public List<SortCriteria> getSortCriteria() {
        return sortCriteria == null ? Collections.emptyList() : sortCriteria;
    }

    /**
     * @param sortCriteria the sortCriteria to set
     */
    public void setSortCriteria(List<SortCriteria> sortCriteria) {
        this.sortCriteria = sortCriteria;
    }
}
