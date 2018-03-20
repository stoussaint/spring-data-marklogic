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
import com._4dconcept.springframework.data.marklogic.repository.Address;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Example;
import org.springframework.expression.spel.SpelEvaluationException;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
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

        Query build = new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "#{entityClass.getSimpleName()}";
            }
        }).build();

        assertThat(build, notNullValue());
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
    @SuppressWarnings("unchecked")
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
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        assertThat(criteriaList, notNullValue());
        assertThat(criteriaList, hasSize(3));
        assertThat(criteriaList.get(0).getCriteriaObject(), is("Me"));
        assertThat(criteriaList.get(1).getCriteriaObject(), is(38));
        assertThat(criteriaList.get(2).getOperator(), is(Criteria.Operator.and));
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
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
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
}