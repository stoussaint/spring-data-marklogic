package com._4dconcept.springframework.data.marklogic.core.query;

import java.util.List;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class Query {

    private int skip;
    private int limit;
    private String collection;

    private Criteria criteria;
    private List<SortCriteria> sortCriteria;

    public Query() {
    }

    public Query(Criteria criteria) {
        this.criteria = criteria;
    }

    /**
     * @return the skip
     */
    public int getSkip() {
        return skip;
    }

    /**
     * @param skip the skip to set
     */
    public void setSkip(int skip) {
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
        return sortCriteria;
    }

    /**
     * @param sortCriteria the sortCriteria to set
     */
    public void setSortCriteria(List<SortCriteria> sortCriteria) {
        this.sortCriteria = sortCriteria;
    }
}
