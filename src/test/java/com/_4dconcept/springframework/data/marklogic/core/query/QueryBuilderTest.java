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

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperationOptions;
import com._4dconcept.springframework.data.marklogic.core.mapping.Collection;
import com._4dconcept.springframework.data.marklogic.core.mapping.Document;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicIdentifier;
import com._4dconcept.springframework.data.marklogic.repository.Address;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Example;
import org.springframework.expression.spel.SpelEvaluationException;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author stoussaint
 * @since 2017-08-01
 */
public class QueryBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void buildEmptyQuery() {
        Query query = new QueryBuilder().build();

        assertThat(query, notNullValue());
        assertThat(query.getCriteria(), nullValue());
        assertThat(query.getSortCriteria(), empty());
        assertThat(query.getSkip(), is(0L));
        assertThat(query.getLimit(), is(0));
    }

    @Test
    public void name() {
        Query query = new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public Class entityClass() {
                return Person.class;
            }
        }).build();

        assertThat(query.getCollection(), is("Person"));
    }

    @Test
    public void buildEmptyQueryWithUnresolvableCollection_throwsException() {
        expectedException.expect(SpelEvaluationException.class);
        expectedException.expectMessage("Attempted to call method getSimpleName() on null context object");

        new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "#{entityClass.getSimpleName()}";
            }
        }).build();
    }

    @Test
    public void buildEmptyQueryWithSpecificCollection() {
        Query query = new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "#{entityClass.getSimpleName()}";
            }

            @Override
            public Class entityClass() {
                return Person.class;
            }
        }).build();

        assertThat(query.getCollection(), is("Person"));
    }

    @Test
    public void buildQueryFromEmptyExample() {
        Person person = new Person();
        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query, notNullValue());
        assertThat(query.getCriteria(), nullValue());
    }

    @Test
    public void buildQueryFromExampleWithExtraCollections() {
        Person person = new Person();
        person.setExtraCollections(Arrays.asList("collection1", "collection2"));
        person.setType("neighbour");
        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query.getCollection(), is("Person"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();

        assertThat(criteriaList, hasSize(2));
        assertThat(criteriaList.get(0).getCriteriaObject(), is("neighbour"));
        assertThat(criteriaList.get(1).getOperator(), is(Criteria.Operator.OR));

        assertThat(criteriaList.get(1).getCriteriaObject(), instanceOf(List.class));
        @SuppressWarnings("unchecked")
        List<Criteria> subCriteriaList = (List<Criteria>) criteriaList.get(1).getCriteriaObject();

        assertThat(subCriteriaList, hasSize(2));
        assertThat(subCriteriaList.get(0).getOperator(), is(Criteria.Operator.COLLECTION));
        assertThat(subCriteriaList.get(0).getCriteriaObject(), is("extra:collection1"));
        assertThat(subCriteriaList.get(1).getOperator(), is(Criteria.Operator.COLLECTION));
        assertThat(subCriteriaList.get(1).getCriteriaObject(), is("extra:collection2"));
    }

    @Test
    public void buildQueryFromFilledExample() {
        Person person = new Person();
        person.setFirstname("Me");
        person.setAge(38);

        Address address = new Address();
        address.setTown("Paris");
        address.setCountry("France");
        person.setAddress(address);

        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("Person"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();

        assertThat(criteriaList, hasSize(3));
        assertThat(criteriaList.get(0).getCriteriaObject(), is("Me"));
        assertThat(criteriaList.get(1).getCriteriaObject(), is(38));
        assertThat(criteriaList.get(2).getOperator(), is(Criteria.Operator.AND));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void considerCollectionValuesAsOrQuery() {
        Person person = new Person();
        person.setSkills(Arrays.asList("java", "xml"));
        person.setAge(38);

        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("Person"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        assertThat(criteriaList, hasSize(2));

    }

    @Test
    public void buildQueryWithExplicitCollection() {
        Person person = new Person();

        Query query = new QueryBuilder().alike(Example.of(person)).options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "contact";
            }
        }).build();

        assertThat(query.getCollection(), is("contact"));
    }

    @Test
    public void buildQuery_ForSimpleEntityIdentifierBy() {
        Query query = new QueryBuilder().ofType(SimpleIdentifiedEntity.class).identifiedBy(new MarklogicIdentifier() {
            @Override
            public QName qname() {
                return new QName("test", "id");
            }

            @Override
            public Object value() {
                return "1";
            }
        }).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("SimpleIdentifiedEntity"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), nullValue());
        assertThat(query.getCriteria().getQname(), notNullValue());
        assertThat(query.getCriteria().getQname().getNamespaceURI(), is("test"));
        assertThat(query.getCriteria().getQname().getLocalPart(), is("id"));
        assertThat(query.getCriteria().getCriteriaObject(), is("1"));
        assertThat(query.getCriteria().getOptions(), contains("exact"));
    }

    @Test
    public void buildQuery_ForCompositeEntityIdentifierBy() {
        Query query = new QueryBuilder().ofType(CompositeIdentifiedEntity.class).identifiedBy(new MarklogicIdentifier() {
            @Override
            public QName qname() {
                return new QName("test", "id");
            }

            @Override
            public Object value() {
                return new CompositeId("aaa", "111");
            }
        }).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("CompositeIdentifiedEntity"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> subCriteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();

        assertThat(subCriteriaList, hasSize(2));

        Criteria criteria1 = subCriteriaList.get(0);
        assertThat(criteria1.getQname(), notNullValue());
        assertThat(criteria1.getQname().getLocalPart(), is("idPart1"));
        assertThat(criteria1.getCriteriaObject(), is("aaa"));

        Criteria criteria2 = subCriteriaList.get(1);
        assertThat(criteria2.getQname(), notNullValue());
        assertThat(criteria2.getQname().getLocalPart(), is("idPart2"));
        assertThat(criteria2.getCriteriaObject(), is("111"));
    }

    @Test
    public void buildQuery_ForSimpleEntityIdentifierByInProperties() {
        Query query = new QueryBuilder().ofType(SimpleIdentifiedEntity.class).identifiedBy(new MarklogicIdentifier() {
            @Override
            public QName qname() {
                return new QName("test", "id");
            }

            @Override
            public Object value() {
                return "1";
            }
        }).options(new MarklogicOperationOptions() {
            @Override
            public boolean idInPropertyFragment() {
                return true;
            }
        }).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("SimpleIdentifiedEntity"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.PROPERTIES));

        Criteria criteriaObject = (Criteria) query.getCriteria().getCriteriaObject();
        assertThat(criteriaObject, notNullValue());
        assertThat(criteriaObject.getQname(), notNullValue());
        assertThat(criteriaObject.getQname().getNamespaceURI(), is("test"));
        assertThat(criteriaObject.getQname().getLocalPart(), is("id"));
        assertThat(criteriaObject.getCriteriaObject(), is("1"));
    }

    @Test
    public void buildQuery_ForCompositeEntityIdentifierByInProperties() {
        Query query = new QueryBuilder().ofType(CompositeIdentifiedEntity.class).identifiedBy(new MarklogicIdentifier() {
            @Override
            public QName qname() {
                return new QName("test", "id");
            }

            @Override
            public Object value() {
                return new CompositeId("aaa", "111");
            }
        }).options(new MarklogicOperationOptions() {
            @Override
            public boolean idInPropertyFragment() {
                return true;
            }
        }).build();

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("CompositeIdentifiedEntity"));
        assertThat(query.getCriteria(), notNullValue());

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.PROPERTIES));
        Criteria criteriaObject = (Criteria) query.getCriteria().getCriteriaObject();

        assertThat(criteriaObject, notNullValue());
        assertThat(criteriaObject.getOperator(), is(Criteria.Operator.AND));
        assertThat(criteriaObject.getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> subCriteriaList = (List<Criteria>) criteriaObject.getCriteriaObject();

        assertThat(subCriteriaList, hasSize(2));

        Criteria criteria1 = subCriteriaList.get(0);
        assertThat(criteria1.getQname(), notNullValue());
        assertThat(criteria1.getQname().getLocalPart(), is("idPart1"));
        assertThat(criteria1.getCriteriaObject(), is("aaa"));

        Criteria criteria2 = subCriteriaList.get(1);
        assertThat(criteria2.getQname(), notNullValue());
        assertThat(criteria2.getQname().getLocalPart(), is("idPart2"));
        assertThat(criteria2.getCriteriaObject(), is("111"));
    }

    @Document
    @Collection("#{entityClass.getSimpleName()}")
    private static class SimpleIdentifiedEntity {
        private String id;

        public SimpleIdentifiedEntity(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @Document
    @Collection("#{entityClass.getSimpleName()}")
    private static class CompositeIdentifiedEntity {
        private CompositeId id;

        public CompositeIdentifiedEntity(CompositeId id) {
            this.id = id;
        }

        public CompositeId getId() {
            return id;
        }
    }

    private static class CompositeId {
        private String idPart1;
        private String idPart2;

        public CompositeId(String idPart1, String idPart2) {
            this.idPart1 = idPart1;
            this.idPart2 = idPart2;
        }

        public String getIdPart1() {
            return idPart1;
        }

        public String getIdPart2() {
            return idPart2;
        }
    }

}