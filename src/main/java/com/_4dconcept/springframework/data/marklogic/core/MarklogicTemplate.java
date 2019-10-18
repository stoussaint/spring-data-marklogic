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
package com._4dconcept.springframework.data.marklogic.core;

import com._4dconcept.springframework.data.marklogic.MarklogicCollectionUtils;
import com._4dconcept.springframework.data.marklogic.MarklogicSupportedType;
import com._4dconcept.springframework.data.marklogic.MarklogicTypeUtils;
import com._4dconcept.springframework.data.marklogic.MarklogicUtils;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicContentHolder;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicWriter;
import com._4dconcept.springframework.data.marklogic.core.cts.CTSQuerySerializer;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicIdentifier;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicSimpleTypes;
import com._4dconcept.springframework.data.marklogic.core.mapping.event.AfterRetrieveEvent;
import com._4dconcept.springframework.data.marklogic.core.mapping.event.AfterSaveEvent;
import com._4dconcept.springframework.data.marklogic.core.mapping.event.BeforeConvertEvent;
import com._4dconcept.springframework.data.marklogic.core.mapping.event.BeforeSaveEvent;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.core.query.QueryBuilder;
import com._4dconcept.springframework.data.marklogic.datasource.ContentSourceUtils;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmValue;
import com.marklogic.xcc.types.XdmVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Primary implementation of {@link MarklogicOperations}.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicTemplate implements MarklogicOperations, ApplicationEventPublisherAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarklogicTemplate.class);

    private final ContentSource contentSource;
    private final MarklogicConverter marklogicConverter;
    private final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    private MarklogicCollectionUtils marklogicCollectionUtils = new MarklogicCollectionUtils() {};

    private static final String SUBMISSION_ERROR_MSG = "Unable to submit request";

    private ApplicationEventPublisher eventPublisher;

    public MarklogicTemplate(ContentSource contentSource) {
        this(contentSource, null);
    }

    public MarklogicTemplate(ContentSource contentSource, MarklogicConverter marklogicConverter) {
        Assert.notNull(contentSource, "contentSource must not be null");

        this.contentSource = contentSource;
        this.marklogicConverter = marklogicConverter == null ? getDefaultMarklogicConverter() : marklogicConverter;
        this.mappingContext = this.marklogicConverter.getMappingContext();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    @Override
    public long count(Query query) {
        String ctsQuery = new CTSQuerySerializer(query).disablePagination().asCtsQuery();
        String countQuery = String.format("xdmp:estimate(%s)", ctsQuery);
        return invokeAdhocQuery(countQuery, Long.TYPE, new MarklogicInvokeOperationOptions() {
            @Override
            public boolean useCacheResult() {
                return false;
            }
        });
    }

    @Override
    public void insert(Object objectToSave) {
        final String uri = mappingContext.getPersistentEntity(objectToSave.getClass()).getUri();
        final String defaultCollection = mappingContext.getPersistentEntity(objectToSave.getClass()).getDefaultCollection();

        insert(objectToSave, new MarklogicCreateOperationOptions() {
            @Override
            public String uri() {
                return uri;
            }

            @Override
            public String[] extraCollections() {
                return new String[0];
            }

            @Override
            public String defaultCollection() {
                return defaultCollection;
            }
        });
    }

    @Override
    public void insert(Object objectToSave, MarklogicCreateOperationOptions options) {
        assertAutoGenerableIdIfNotSet(objectToSave);

        generateIdIfNecessary(objectToSave);

        doInsert(objectToSave, options, this.marklogicConverter);
    }

    @Override
    public void save(Object objectToSave) {
        if (isUnidentifiedObject(objectToSave)) {
            LOGGER.debug("Save operation issued with unidentified object. Fallback to insert operation.");
            insert(objectToSave);
        } else {
            final String uri = retrieveUri(objectToSave);
            final String defaultCollection = mappingContext.getPersistentEntity(objectToSave.getClass()).getDefaultCollection();

            save(objectToSave, new MarklogicCreateOperationOptions() {
                @Override
                public String uri() {
                    return uri;
                }

                @Override
                public String[] extraCollections() {
                    return new String[0];
                }

                @Override
                public String defaultCollection() {
                    return defaultCollection;
                }
            });
        }
    }

    @Override
    public void save(Object objectToSave, MarklogicCreateOperationOptions options) {
        if (isUnidentifiedObject(objectToSave)) {
            LOGGER.debug("Save operation issued with unidentified object. Fallback to insert operation.");
            insert(objectToSave, options);
        } else {
            final String uri = retrieveUri(objectToSave);
            doInsert(objectToSave, new MarklogicCreateOperationOptions() {
                @Override
                public String uri() {
                    return uri;
                }

                @Override
                public String[] extraCollections() {
                    return options.extraCollections();
                }

                @Override
                public String defaultCollection() {
                    return options.defaultCollection();
                }

                @Override
                public boolean idInPropertyFragment() {
                    return options.idInPropertyFragment();
                }

                @Override
                public Class entityClass() {
                    return options.entityClass();
                }
            }, marklogicConverter);
        }
    }

    @Override
    public void remove(Object entity) {
        String uri = retrieveUri(entity);
        if (uri != null) {
            LOGGER.debug("Remove '{}' from '{}'", entity, uri);
            // Double the delimiter of string literal to allow its inclusion in said string literal
            String query = "xdmp:document-delete('" + uri.replace("'", "''") + "')";
            invokeAdhocQuery(query, new MarklogicInvokeOperationOptions() {

                @Override
                public boolean useCacheResult() {
                    return false;
                }
            });
        }
    }

    @Override
    public <T> void remove(Object id, Class<T> entityClass) {
        T entity = findById(id, entityClass);
        remove(entity);
    }

    @Override
    public <T> void remove(Object id, Class<T> entityClass, MarklogicOperationOptions options) {
        T entity = findById(id, entityClass, options);
        remove(entity);
    }

    @Override
    public <T> void removeAll(Class<T> entityClass) {
        String collection = determineCollectionName(entityClass);
        removeAll(entityClass, new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return collection;
            }
        });
    }

    @Override
    public <T> void removeAll(Class<T> entityClass, MarklogicOperationOptions options) {
        Assert.notNull(options.defaultCollection(), "A collection should be provided for removeAll operation!");

        String collection = MarklogicUtils.expandsExpression(options.defaultCollection(), entityClass);
        doRemoveAll(collection);
    }

    @Override
    public <T> T findById(Object id, Class<T> entityClass) {
        return findById(id, entityClass, new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return determineCollectionName(entityClass);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T findById(Object id, Class<T> entityClass, MarklogicOperationOptions options) {
        final Class<T> targetEntityClass = options.entityClass() == null ? entityClass : (Class<T>) options.entityClass();
        MarklogicIdentifier identifier = resolveMarklogicIdentifier(id, targetEntityClass);

        final String targetCollection = retrieveTargetCollection(MarklogicUtils.expandsExpression(options.defaultCollection(), targetEntityClass, null, () -> id));

        final boolean isIdInPropertyFragment = options.idInPropertyFragment();

        LOGGER.debug("Retrieve object stored in '{}' default collection with '{}' as identifier", targetCollection, id);

        StringBuilder sb = new StringBuilder("cts:search(" + targetCollection + ",");
        if (isIdInPropertyFragment) {
            sb.append("cts:properties-fragment-query(");
        }
        sb
            .append("cts:element-value-query(fn:QName(\"")
            .append(identifier.qname().getNamespaceURI())
            .append("\", \"")
            .append(identifier.qname().getLocalPart())
            .append("\"), \"")
            .append(identifier.value())
            .append("\", \"exact\")");
        if (isIdInPropertyFragment) {
            sb.append(")");
        }
        sb.append(")");

        LOGGER.trace("{}", sb);

        return invokeAdhocQuery(sb.toString(), entityClass, new MarklogicInvokeOperationOptions() {
            @Override
            public Map<Object, Object> params() {
                Map<Object, Object> params = new HashMap<>();
                params.put("id", id);
                return params;
            }
        });
    }

    @Override
    public <T> List<T> find(Query query, Class<T> entityClass) {
        return find(query, entityClass, new MarklogicOperationOptions() {});
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> find(Query query, Class<T> entityClass, MarklogicOperationOptions options) {
        return invokeAdhocQueryAsList(new CTSQuerySerializer(query).asCtsQuery(), entityClass, new MarklogicInvokeOperationOptions() {
            @Override
            public boolean useCacheResult() {
                return false;
            }
        });
    }

    @Override
    public <T> T findOne(Query query, Class<T> entityClass) {
        return findOne(query, entityClass, new MarklogicOperationOptions() {});
    }

    @Override
    public <T> T findOne(Query query, Class<T> entityClass, MarklogicOperationOptions options) {
        List<T> resultList = find(query, entityClass, options);

        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        } else if (resultList.size() == 1) {
            return resultList.get(0);
        } else {
            throw new DataRetrievalFailureException("Only one result expected. You should probably call find instead");
        }
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass) {
        return find(new QueryBuilder(mappingContext).ofType(entityClass).build(), entityClass, new MarklogicOperationOptions() {});
    }

    @Override
    public <T> List<T> findAll(Class<T> entityClass, MarklogicOperationOptions options) {
        return find(new QueryBuilder(mappingContext).ofType(entityClass).build(), entityClass, options);
    }

    @Override
    public <T> List<T> invokeModuleAsList(String moduleName, Class<T> resultClass, MarklogicInvokeOperationOptions options) {
        return returnInSession(session -> {
            try {
                ResultSequence resultSequence = session.submitRequest(buildModuleRequest(moduleName, options, session));
                return prepareResultList(resultSequence, resultClass, options);
            } catch (RequestException re) {
                throw new DataRetrievalFailureException(SUBMISSION_ERROR_MSG, re);
            }
        });
    }

    @Override
    public <T> T invokeModule(String moduleName, Class<T> resultClass, MarklogicInvokeOperationOptions options) {
        List<T> resultList = invokeModuleAsList(moduleName, resultClass, options);

        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        } else if (resultList.size() == 1) {
            return resultList.get(0);
        } else {
            throw new DataRetrievalFailureException("Only one result expected. You should probably call invokeModuleAsList instead");
        }
    }

    @Override
    public void invokeModule(String moduleName, MarklogicInvokeOperationOptions options) {
        doInSession(session -> {
            try {
                ResultSequence resultSequence = session.submitRequest(buildModuleRequest(moduleName, options, session));
                if (! resultSequence.isClosed()) {
                    resultSequence.close();
                }
            } catch (RequestException re) {
                throw new DataRetrievalFailureException(SUBMISSION_ERROR_MSG, re);
            }
        });
    }

    @Override
    public <T> List<T> invokeAdhocQueryAsList(String query, Class<T> resultClass, MarklogicInvokeOperationOptions options) {
        return returnInSession(session -> {
            try {
                ResultSequence resultSequence = session.submitRequest(buildAdhocRequest(query, options, session));
                return prepareResultList(resultSequence, resultClass, options);
            } catch (RequestException re) {
                throw new DataRetrievalFailureException(SUBMISSION_ERROR_MSG, re);
            }
        });
    }

    @Override
    public <T> T invokeAdhocQuery(String query, Class<T> resultClass, MarklogicInvokeOperationOptions options) {
        List<T> resultList = invokeAdhocQueryAsList(query, resultClass, options);

        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        } else if (resultList.size() == 1) {
            return resultList.get(0);
        } else {
            throw new DataRetrievalFailureException("Only one result expected. You should probably call invokeAdhocQueryAsList instead");
        }
    }

    @Override
    public void invokeAdhocQuery(String query, MarklogicInvokeOperationOptions options) {
        doInSession(session -> {
            try {
                ResultSequence resultSequence = session.submitRequest(buildAdhocRequest(query, options, session));
                if (! resultSequence.isClosed()) {
                    resultSequence.close();
                }
            } catch (RequestException re) {
                throw new DataRetrievalFailureException(SUBMISSION_ERROR_MSG, re);
            }
        });
    }

    @Override
    public <T> String resolveDefaultCollection(T entity, MarklogicOperationOptions options) {
        MarklogicPersistentEntity persistentEntity = mappingContext.getPersistentEntity(entity.getClass());
        String defaultCollection = options.defaultCollection() == null ? persistentEntity.getDefaultCollection() : options.defaultCollection();
        return MarklogicUtils.expandsExpression(defaultCollection, entity.getClass(), entity, () ->  MarklogicUtils.retrieveIdentifier(entity, mappingContext));
    }

    @Override
    public <T> Object resolveContentIdentifier(T entity) {
        return MarklogicUtils.retrieveIdentifier(entity, mappingContext);
    }

    @Override
    public MarklogicConverter getConverter() {
        return this.marklogicConverter;
    }

    /*
        #############################
        ## PRIVATE IMPLEMENTATIONS ##
        #############################
        */
    private void doRemoveAll(String collection) {
        LOGGER.debug("Remove all entities stored in '{}' default collection", collection);

        String query = "xdmp:collection-delete(\"" + collection + "\")";

        doInSession(session -> {
            Request request = session.newAdhocQuery(query);
            try {
                ResultSequence resultSequence = session.submitRequest(request);
                if (! resultSequence.isClosed()) {
                    resultSequence.close();
                }
            } catch (RequestException re) {
                throw new DataRetrievalFailureException("Unable to query uri", re);
            }
        });
    }

    private <T> void doInsert(T objectToSave, MarklogicCreateOperationOptions options, MarklogicWriter<T> writer) {
        String uri =  MarklogicUtils.expandsExpression(options.uri(), objectToSave.getClass(), objectToSave, () -> MarklogicUtils.retrieveIdentifier(objectToSave, mappingContext));
        String collection = MarklogicUtils.expandsExpression(options.defaultCollection(), objectToSave.getClass(), objectToSave, () -> MarklogicUtils.retrieveIdentifier(objectToSave, mappingContext));

        LOGGER.debug("Insert entity '{}' at '{}' within '{}' default collection", objectToSave, uri, collection);

        maybeEmitEvent(new BeforeConvertEvent<>(objectToSave, uri));

        Content content = toContentObject(uri, objectToSave, collection, writer);

        maybeEmitEvent(new BeforeSaveEvent<>(objectToSave, content, uri));

        doInsertContent(content);
        doPostInsert(uri, objectToSave);

        maybeEmitEvent(new AfterSaveEvent<>(objectToSave, content, uri));
    }

    private void maybeEmitEvent(ApplicationEvent event) {
        if (null != eventPublisher) {
            eventPublisher.publishEvent(event);
        }
    }

    /**
     * Populates the id property of the saved object, if it's not set already.
     *
     * @param objectToSave The object currently saved
     */
    private void generateIdIfNecessary(Object objectToSave) {

        MarklogicPersistentProperty property = MarklogicUtils.getIdPropertyFor(objectToSave.getClass(), mappingContext);

        if (property == null) {
            return;
        }

        MarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(objectToSave.getClass());
        PersistentPropertyAccessor accessor = entity.getPropertyAccessor(objectToSave);

        if (accessor.getProperty(property) != null) {
            return;
        }

        ConversionService conversionService = marklogicConverter.getConversionService();
        new ConvertingPropertyAccessor(accessor, conversionService).setProperty(property, UUID.randomUUID());
    }

    private boolean isUnidentifiedObject(Object objectToSave) {
        MarklogicPersistentProperty property = MarklogicUtils.getIdPropertyFor(objectToSave.getClass(), mappingContext);

        if (property == null) {
            return true;
        }

        MarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(objectToSave.getClass());
        return entity.getPropertyAccessor(objectToSave).getProperty(property) == null;
    }

    private String retrieveUri(Object objectToSave) {
        MarklogicPersistentEntity persistentEntity = mappingContext.getPersistentEntity(objectToSave.getClass());

        final MarklogicIdentifier identifier = resolveMarklogicIdentifier(objectToSave);
        final String collection = MarklogicUtils.expandsExpression(persistentEntity.getDefaultCollection(), objectToSave.getClass(), null, () -> MarklogicUtils.retrieveIdentifier(objectToSave, mappingContext));

        String collectionConstraints = retrieveConstraintCollection(collection);

        final boolean isIdInPropertyFragment = persistentEntity.idInPropertyFragment();

        LOGGER.debug("Looking to uri for object stored in '{}' default collection with '{}' as identifier", collection, identifier.qname());

        StringBuilder sb = new StringBuilder("cts:uris((), (), cts:and-query((");
        if (collectionConstraints != null) {
            sb.append(collectionConstraints).append(",");
        }

        if (isIdInPropertyFragment) {
            sb.append("cts:properties-fragment-query(");
        }
        sb.append("cts:element-value-query(fn:QName(\"")
                .append(identifier.qname().getNamespaceURI())
                .append("\", \"")
                .append(identifier.qname().getLocalPart())
                .append("\"), \"")
                .append(identifier.value())
                .append("\")");
        if (isIdInPropertyFragment) {
            sb.append(")");
        }
        sb.append(")))");

        LOGGER.trace("{}", sb);

        List<String> uris = invokeAdhocQueryAsList(sb.toString(), String.class, new MarklogicInvokeOperationOptions() {});

        if (! CollectionUtils.isEmpty(uris)) {
            return uris.get(0);
        } else {
            return persistentEntity.getUri();
        }
    }

    private <T> T returnInSession(Function<Session, T> sessionTask) {
        Session session = ContentSourceUtils.getSession(contentSource);
        try {
            return sessionTask.apply(session);
        } finally {
            ContentSourceUtils.releaseSession(session, contentSource);
        }
    }

    private void doInSession(Consumer<Session> sessionTask) {
        Session session = ContentSourceUtils.getSession(contentSource);
        try {
            sessionTask.accept(session);
        } finally {
            ContentSourceUtils.releaseSession(session, contentSource);
        }
    }

    private <T> Content toContentObject(String uri, T entity, String collection, MarklogicWriter<T> writer) {
        Content content;
        boolean supportedClass = MarklogicTypeUtils.isSupportedType(entity.getClass());

        ArrayList<String> collections = new ArrayList<>();
        collections.add(collection);
        collections.addAll(extractCollections(entity));

        Object contentToSave;

        if (! supportedClass) {
            MarklogicContentHolder holder = new MarklogicContentHolder();
            writer.write(entity, holder);
            contentToSave = holder.getContent();
        } else {
            contentToSave = entity;
        }

        try {
            content = MarklogicSupportedType
                    .fromClass(contentToSave.getClass())
                    .orElseThrow(() -> new MappingException("Unexpected content type " + entity.getClass()))
                    .createContentObject(uri, contentToSave);
        } catch (IOException ioe) {
            throw new MappingException("Unable to convert entity into a Marklogic Content", ioe);
        }


        if (collection != null) {
            content.getCreateOptions().setCollections(collections.toArray(new String[0]));
        }

        return content;
    }

    private <T> List<String> extractCollections(T entity) {
        return marklogicCollectionUtils.extractCollections(entity, mappingContext);
    }

    private void doInsertContent(Content content) {
        doInSession(session -> {
            try {
                session.insertContent(content);
            } catch (RequestException re) {
                throw new DataAccessResourceFailureException("Unable to execute request", re);
            }
        });
    }

    private void assertAutoGenerableIdIfNotSet(Object entity) {

        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entity.getClass());
        MarklogicPersistentProperty idProperty = persistentEntity == null ? null : persistentEntity.getIdProperty();

        if (idProperty == null) {
            return;
        }

        Object idValue = persistentEntity.getPropertyAccessor(entity).getProperty(idProperty);

        if (idValue == null && !MarklogicSimpleTypes.AUTOGENERATED_ID_TYPES.contains(idProperty.getType())) {
            throw new InvalidDataAccessApiUsageException(
                    String.format("Cannot auto-generate id of type %s for entity of type %s!", idProperty.getType().getName(),
                            entity.getClass().getName()));
        }
    }

    private String determineCollectionName(Class<?> entityClass) {
        if (entityClass == null) {
            throw new InvalidDataAccessApiUsageException("No class parameter provided, entity can't be determined!");
        }

        MarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(entityClass);
        if (entity == null) {
            throw new InvalidDataAccessApiUsageException("No Persistent Entity information found for the class " + entityClass.getName());
        }

        return entity.getDefaultCollection();
    }

    private static MarklogicConverter getDefaultMarklogicConverter() {
        MarklogicMappingConverter marklogicMappingConverter = new MarklogicMappingConverter(new MarklogicMappingContext());
        marklogicMappingConverter.afterPropertiesSet();
        return marklogicMappingConverter;
    }

    private Request buildAdhocRequest(String query, MarklogicInvokeOperationOptions options, Session session) {
        Assert.notNull(query, "A query must be provided!");

        Request request = session.newAdhocQuery(query);
        if (options != null) {
            request.getOptions().setCacheResult(options.useCacheResult());
            addVariablesToRequest(options.params(), request);
        }
        return request;
    }

    private Request buildModuleRequest(String moduleName, MarklogicInvokeOperationOptions options, Session session) {
        Assert.notNull(moduleName, "A module name must be provided!");

        Request request = session.newModuleInvoke(moduleName);
        if (options != null) {
            addVariablesToRequest(options.params(), request);
        }
        return request;
    }

    private void addVariablesToRequest(Map<Object, Object> params, Request request) {
        if (params != null) {
            for (Map.Entry<Object, Object> entry : params.entrySet()) {
                request.setVariable(buildVariable(entry.getKey(), entry.getValue()));
            }
        }
    }

    private XdmVariable buildVariable(Object key, Object value) {
        XName xname = isFullQualifiedName(key) ? buildVariableName((QName) key) : new XName((String) key);
        return ValueFactory.newVariable(xname, buildVariableValue(value));
    }

    private XName buildVariableName(QName name) {
        return new XName(name.getNamespaceURI(), name.getLocalPart());
    }

    private boolean isFullQualifiedName(Object name) {
        return name instanceof QName;
    }

    private XdmValue buildVariableValue(Object value) {
        if (value == null) {
            return ValueFactory.newXSString("");
        }

        return marklogicConverter.getConversionService().convert(value, XdmValue.class);
    }

    private <T> List<T> prepareResultList(ResultSequence resultSequence, Class<T> returnType, MarklogicInvokeOperationOptions options) {
        List<T> resultList = null;
        try {
            if (resultSequence != null && !resultSequence.isEmpty() && returnType != null) {
                resultList = new ArrayList<>();
                while (resultSequence.hasNext()) {
                    resultList.add(prepareResultItem(resultSequence.next(), returnType, options));
                }
            }
        } finally {
            if (resultSequence != null && !resultSequence.isClosed()) {
                resultSequence.close();
            }
        }
        return resultList;
    }

    private <T> T prepareResultItem(ResultItem resultItem, Class<T> returnType, MarklogicInvokeOperationOptions options) {
        MarklogicContentHolder holder = new MarklogicContentHolder();
        holder.setContent(resultItem);

        T item = marklogicConverter.read(returnType, holder);
        if (item != null) {
            AfterRetrieveEvent<T> event = new AfterRetrieveEvent<>(item, resultItem.getDocumentURI());
            if (options != null) {
                event.setParams(options.params());
            }
            maybeEmitEvent(event);
        }
        return item;
    }

    private String retrieveTargetCollection(String defaultCollection) {
        if (defaultCollection == null) {
            return "fn:collection()";
        }

        return "fn:collection('" + defaultCollection + "')";
    }

    private String retrieveConstraintCollection(String defaultCollection) {
        if (defaultCollection == null) {
            return null;
        }

        return "cts:collection-query('" + defaultCollection + "')";
    }

    private MarklogicIdentifier resolveMarklogicIdentifier(Object object) {
        MarklogicPersistentProperty idProperty = MarklogicUtils.getIdPropertyFor(object.getClass(), mappingContext);

        if (idProperty == null)
            throw new  InvalidDataAccessApiUsageException("Unable to retrieve expected identifier property !");

        return resolveMarklogicIdentifier(MarklogicUtils.retrieveIdentifier(object, mappingContext), idProperty);
    }

    private <T> MarklogicIdentifier resolveMarklogicIdentifier(Object id, Class<T> entityClass) {
        MarklogicPersistentProperty idProperty = MarklogicUtils.getIdPropertyFor(entityClass, mappingContext);

        if (idProperty == null)
            throw new  InvalidDataAccessApiUsageException("Unable to retrieve expected identifier property !");

        return resolveMarklogicIdentifier(id, idProperty);
    }

    private MarklogicIdentifier resolveMarklogicIdentifier(Object id, MarklogicPersistentProperty idProperty) {
        if (MarklogicTypeUtils.isSimpleType(idProperty.getType())) {
            return new MarklogicIdentifier() {
                @Override
                public QName qname() {
                    return idProperty.getQName();
                }

                @Override
                public String value() {
                    return id.toString();
                }
            };
        }

        ConversionService conversionService = marklogicConverter.getConversionService();
        if (conversionService.canConvert(idProperty.getType(), MarklogicIdentifier.class)) {
            return conversionService.convert(id, MarklogicIdentifier.class);
        }

        throw new MappingException("Unexpected identifier type " + idProperty.getClass());
    }

    private <T> void doPostInsert(String uri, T objectToSave) {
        MarklogicPersistentEntity persistentEntity = mappingContext.getPersistentEntity(objectToSave.getClass());
        if (persistentEntity.idInPropertyFragment()) {
            MarklogicIdentifier identifier = resolveMarklogicIdentifier(objectToSave);
            invokeAdhocQuery(String.format(
                    "declare namespace _id=\"%s\";\n" +
                    "xdmp:document-set-property(\"%s\", element _id:%s {\"%s\"})",
                    identifier.qname().getNamespaceURI(),
                    uri,
                    identifier.qname().getLocalPart(),
                    identifier.value()
            ), new MarklogicInvokeOperationOptions() {});
        }
    }

    void setMarklogicCollectionUtils(MarklogicCollectionUtils marklogicCollectionUtils) {
        this.marklogicCollectionUtils = marklogicCollectionUtils;
    }
}