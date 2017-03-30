package org.springframework.data.marklogic.core;

import org.springframework.data.marklogic.core.convert.MarklogicConverter;

import java.util.List;

/**
 * Interface that specifies a basic set of Marklogic operations. It offers more specifics operations than the underlying
 * {@link com.marklogic.xcc.ContentSource}. Implemented by {@link MarklogicTemplate}.
 * Not often used but a useful option for extensibility and testability.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicOperations {

    /**
     * Insert the given object.
     * <p/>
     * Content will be converted if not one of supported type.
     * Uri will be computed as well as creation options (such as defaultCollection)
     * <p/>
     * Insert is used to initially store the object into the database. To update an existing object use the save method.
     *
     * @param objectToSave the object to store
     */
    void insert(Object objectToSave);

    /**
     * Insert the given object at the specified uri with specified creation options.
     * <p/>
     * Content will be converted if not one of supported type.
     * <p/>
     * Insert is used to initially store the object into the database. To update an existing object use the save method.
     *
     * @param objectToSave the object to store
     * @param options content creation options
     */
    void insert(Object objectToSave, MarklogicCreateOperationOptions options);

    /**
     * Save the given object.
     * <p/>
     * Content will be converted if not one of supported type.
     * <p/>
     * The save method will retrieve uri location to store the content to.
     *
     * @param objectToSave the object to store
     */
    void save(Object objectToSave);

    /**
     * Save the given object.
     * <p/>
     * Content will be converted if not one of supported type.
     * <p/>
     * The save method will retrieve uri location to store the content to.
     *
     * @param objectToSave the object to store
     * @param options content creation options
     */
    void save(Object objectToSave, MarklogicCreateOperationOptions options);

    /**
     * Remove the given Entity
     * @param entity the entity to remove
     */
    void remove(Object entity);

    /**
     * Remove entity with corresponding identifier
     * @param id the identifier
     * @param entityClass the type of the document to remove
     * @param <T>
     */
    <T> void remove(Object id, Class<T> entityClass);

    /**
     * Remove entity with corresponding identifier
     * @param id the identifier
     * @param entityClass the type of the document to remove
     * @param options content deletion options
     * @param <T>
     */
    <T> void remove(Object id, Class<T> entityClass, MarklogicOperationOptions options);

    /**
     * Remove every entities of the given type.
     * @param entityClass the type of the documents to be removed
     * @param <T>
     */
    <T> void removeAll(Class<T> entityClass);

    /**
     * Remove every entities of the given type
     * @param options content deletion options
     * @param <T>
     */
    <T> void removeAll(Class<T> entityClass, MarklogicOperationOptions options);

    /**
     * Returns the document with the given id for the specified entity type.
     * @param id the id of the document to retrieve
     * @param entityClass the type of the document to retrieve
     * @param <T>
     * @return
     */
    <T> T findById(Object id, Class<T> entityClass);

    /**
     * Returns the document with the given id for the specified entity type within explicit collection.
     * @param id the id of the document to retrieve
     * @param entityClass the type of the document to retrieve
     * @param options the explicit collection the document will be queried
     * @param <T>
     * @return
     */
    <T> T findById(Object id, Class<T> entityClass, MarklogicOperationOptions options);


    // TODO Provide an object structure for a so called 'Query' : Backport from QuadroContent ?
    /**
     * Returns content matching the given query
     * @param query the query that specifies criteria used to find contents
     * @param entityClass the entity class the content will be converted to
     * @param <T>
     * @return
     */
    <T> List<T> find(Object query, Class<T> entityClass);

    /**
     * Returns content matching the given query
     *
     * @param query the query that specifies criteria used to find contents
     * @param entityClass the entity class the content will be converted to
     * @param options search options
     * @param <T>
     * @return
     */
    <T> List<T> find(Object query, Class<T> entityClass, MarklogicOperationOptions options);

    /**
     * Returns content matching the given query
     * @param query the query that specifies criteria used to find contents
     * @param entityClass the entity class the content will be converted to
     * @param <T>
     * @return
     */
    <T> T findOne(Object query, Class<T> entityClass);

    /**
     * Returns content matching the given query
     *
     * @param query the query that specifies criteria used to find contents
     * @param entityClass the entity class the content will be converted to
     * @param options search options
     * @param <T>
     * @return
     */
    <T> T findOne(Object query, Class<T> entityClass, MarklogicOperationOptions options);

    <T> List<T> findAll(Class<T> entityClass);

    <T> List<T> findAll(Class<T> entityClass, MarklogicOperationOptions options);

    <T> List<T> invokeModuleAsList(String moduleName, Class<T> resultClass, MarklogicInvokeOperationOptions options);

    <T> T invokeModule(String moduleName, Class<T> resultClass, MarklogicInvokeOperationOptions options);

    void invokeModule(String moduleName, MarklogicInvokeOperationOptions options);

    <T> List<T> invokeAdhocQueryAsList(String query, Class<T> resultClass, MarklogicInvokeOperationOptions options);

    <T> T invokeAdhocQuery(String query, Class<T> resultClass, MarklogicInvokeOperationOptions options);

    void invokeAdhocQuery(String query, MarklogicInvokeOperationOptions options);

    <T> String resolveDefaultCollection(T entity, MarklogicOperationOptions options);

    <T> Object resolveContentIdentifier(T entity);

    /**
     * Returns the underlying {@link MarklogicConverter}.
     *
     * @return
     */
    MarklogicConverter getConverter();
}
