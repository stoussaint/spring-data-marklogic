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
package com._4dconcept.springframework.data.marklogic.repository.support;

import com._4dconcept.springframework.data.marklogic.core.EntityInformationOperationOptions;
import com._4dconcept.springframework.data.marklogic.core.MarklogicOperationOptions;
import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.core.query.QueryBuilder;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Repository base implementation for Marklogic.
 *
 * @author Stéphane Toussaint
 */
@Repository
@Transactional(readOnly = true)
public class SimpleMarklogicRepository<T, ID> implements MarklogicRepository<T, ID> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MarklogicRepository.class);

    protected final MarklogicOperations marklogicOperations;
    protected final MarklogicEntityInformation<T, ID> entityInformation;

    /**
     * Creates a new {@link SimpleMarklogicRepository} for the given {@link MarklogicEntityInformation} and {@link MarklogicTemplate}.
     *
     * @param metadata must not be {@literal null}.
     * @param marklogicOperations must not be {@literal null}.
     */
    public SimpleMarklogicRepository(MarklogicEntityInformation<T, ID> metadata, MarklogicOperations marklogicOperations) {

        Assert.notNull(marklogicOperations, "marklogicOperations must not be null");
        Assert.notNull(metadata, "marklogic entity information must not be null");

        this.entityInformation = metadata;
        this.marklogicOperations = marklogicOperations;
    }

    // READ ONLY Operations

    @Override
    public long count() {
        Query query = newQueryBuilderInstance().options(new MarklogicOperationOptions() {
            @Override
            public Class entityClass() {
                return entityInformation.getJavaType();
            }
        }).build();
        return marklogicOperations.count(query);
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        Query query = newQueryBuilderInstance().alike(example).build();
        return marklogicOperations.count(query);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        Query query = newQueryBuilderInstance().alike(example).build();
        return marklogicOperations.count(query) > 0;
    }

    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "The given id must not be null");
        return Optional.ofNullable(marklogicOperations.findById(id, entityInformation.getJavaType(), new EntityInformationOperationOptions(entityInformation)));
    }

    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        final List<S> results = findAll(example);
        if (CollectionUtils.isEmpty(results)) {
            return Optional.empty();
        }

        if (results.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(1, results.size());
        }

        return Optional.of(results.get(0));
    }

    @Override
    public List<T> findAll() {
        return marklogicOperations.findAll(entityInformation.getJavaType(), new EntityInformationOperationOptions(entityInformation));
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        return StreamSupport.stream(ids.spliterator(), false)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<T> findAll(Sort sort) {
        Query query = newQueryBuilderInstance().options(new MarklogicOperationOptions() {
            @Override
            public Class entityClass() {
                return entityInformation.getJavaType();
            }
        }).with(sort).build();
        return marklogicOperations.find(query, entityInformation.getJavaType());
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        Query query = newQueryBuilderInstance().options(new MarklogicOperationOptions() {
            @Override
            public Class entityClass() {
                return entityInformation.getJavaType();
            }
        }).with(pageable).build();

        long count = marklogicOperations.count(query);

        if (count == 0) {
            return new PageImpl<>(Collections.emptyList());
        }

        return new PageImpl<>(marklogicOperations.find(query, entityInformation.getJavaType()), pageable, count);
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example) {
        Query query = newQueryBuilderInstance().alike(example).build();
        return marklogicOperations.find(query, example.getProbeType());
    }

    @Override
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        Query query = newQueryBuilderInstance().alike(example).with(sort).build();
        return marklogicOperations.find(query, example.getProbeType());
    }

    @Override
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        Query query = newQueryBuilderInstance().alike(example).with(pageable).build();

        long count = marklogicOperations.count(query);

        if (count == 0) {
            return new PageImpl<>(Collections.<S>emptyList());
        }

        return new PageImpl<>(marklogicOperations.find(query, example.getProbeType()), pageable, count);
    }

    // WRITE Operations
    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "entity must not be null");

        if (entityInformation.isNew(entity)) {
            marklogicOperations.insert(entity, new EntityInformationOperationOptions(entityInformation));
        } else {
            marklogicOperations.save(entity, new EntityInformationOperationOptions(entityInformation));
        }

        return entity;
    }

    @Override
    @Transactional
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "entities must not be null");
        return StreamSupport.stream(entities.spliterator(), false).map(this::save).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        marklogicOperations.remove(id, entityInformation.getJavaType(), new EntityInformationOperationOptions(entityInformation));
    }

    @Override
    @Transactional
    public void delete(T entity) {
        marklogicOperations.remove(entity);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    @Transactional
    public void deleteAll() {
        marklogicOperations.removeAll(entityInformation.getJavaType(), new EntityInformationOperationOptions(entityInformation));
    }

    private QueryBuilder newQueryBuilderInstance() {
        return new QueryBuilder(marklogicOperations);
    }

}