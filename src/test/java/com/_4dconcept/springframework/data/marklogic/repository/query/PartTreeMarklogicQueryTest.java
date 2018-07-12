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
package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.convert.MappingMarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.lang.Nullable;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PartTreeMarklogicQuery}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class PartTreeMarklogicQueryTest {

    public @Rule
    ExpectedException exception = ExpectedException.none();

    @Mock
    private MarklogicOperations marklogicOperationsMock;

    private MarklogicMappingContext mappingContext;

    @Before
    public void setUp() {
        mappingContext = new MarklogicMappingContext();
        MarklogicConverter converter = new MappingMarklogicConverter(mappingContext);

        when(marklogicOperationsMock.getConverter()).thenReturn(converter);
    }

    @Test
    public void buildQueryUseExpectedCollection() {
        assertThat(deriveQueryFromMethod("findByLastname", "foo").getCollection(), is("Person"));
        assertThat(deriveQueryFromMethod("findBySkills", "foo").getCollection(), is("Person"));
    }

    @Test
    public void singleFieldShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastname", "foo");

        assertCriteria(
                query.getCriteria(),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );
    }

    @Test
    public void multipleFieldsShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastnameAndFirstname", "foo", "bar");

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());
        assertCriteria(
                criteriaList.get(0),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "firstname")),
                is("bar")
        );
    }

    @Test
    public void deepFieldsShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByAddressCountry", "France");
        assertCriteria(query.getCriteria(),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "country")),
                is("France")
        );
    }

    @Test
    public void differentFieldsLevelShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastnameAndAddressCountry", "foo", "France");

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "country")),
                is("France")
        );
    }

    @Test
    public void collectionFieldsShouldBeConsideredWithASingleParameter() {
        Query query = deriveQueryFromMethod("findBySkills", "foo");

        assertCriteria(
                query.getCriteria(),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );
    }

    @Test
    public void collectionFieldsShouldBeConsideredAsAndCriteria() {
        List<String> skills = new ArrayList<>();
        skills.add("foo");
        skills.add("bar");

        Query query = deriveQueryFromMethod("findBySkills", skills);

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("bar")
        );
    }

    @Test
    public void collectionFieldsWithContainingKeyWordShouldBeConsideredAsOrCriteria() {
        List<String> skills = new ArrayList<>();
        skills.add("foo");
        skills.add("bar");

        Query query = deriveQueryFromMethod("findBySkillsContaining", skills);

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.OR));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("bar")
        );
    }

    @Test
    public void booleanFieldShouldBeConsideredTrue() {
        Query query = deriveQueryFromMethod("findByActiveIsTrue");
        assertCriteria(
                query.getCriteria(),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "active")),
                is(true)
        );
    }

    @Test
    public void booleanFieldShouldBeConsideredFalse() {
        Query query = deriveQueryFromMethod("findByActiveIsFalse");
        assertCriteria(
                query.getCriteria(),
                nullValue(),
                is(new QName("http://spring.data.marklogic/test/contact", "active")),
                is(false)
        );
    }

    @Test
    public void deriveQueryFromMethod_WithCollectionAnnotation_ShouldAppendCollectionConstraints() {
        Query query = deriveQueryFromMethod("findByExtraCollections", "foo");

        assertThat(query.getCollection(), is("Person"));
        assertCriteria(
                query.getCriteria(),
                is(Criteria.Operator.COLLECTION),
                nullValue(),
                is("foo")
        );
    }

    @Test
    public void deriveQueryFromMethod_WithCollectionAnnotationAndNotInExpression_ShouldAppendNegativeCollectionConstraints() {
        Query query = deriveQueryFromMethod("findByExtraCollectionsIsNot", "foo");

        assertThat(query.getCollection(), is("Person"));
        assertCriteria(
                query.getCriteria(),
                is(Criteria.Operator.NOT),
                nullValue(),
                allOf(
                        hasProperty("operator", is(Criteria.Operator.COLLECTION)),
                        hasProperty("criteriaObject", is("foo"))
                )
        );
    }

    private void assertCriteria(@Nullable Criteria criteria, Matcher<Object> operatorMatcher, Matcher<Object> nameMatcher, Matcher<Object> valueMatcher) {
        assertThat(criteria, notNullValue());
        assertThat(criteria.getOperator(), operatorMatcher);
        assertThat(criteria.getQname(), nameMatcher);
        assertThat(criteria.getCriteriaObject(), valueMatcher);
    }

    private Query deriveQueryFromMethod(String method, Object... args) {

        Class<?>[] types = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }

        PartTreeMarklogicQuery partTreeQuery = createQueryForMethod(method, types);

        ParametersParameterAccessor accessor = new ParametersParameterAccessor(partTreeQuery.getQueryMethod().getParameters(), args);
        return partTreeQuery.createQuery(accessor);
    }

    private PartTreeMarklogicQuery createQueryForMethod(String methodName, Class<?>... paramTypes) {

        try {

            Method method = Repo.class.getMethod(methodName, paramTypes);
            ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
            MarklogicQueryMethod queryMethod = new MarklogicQueryMethod(method, new DefaultRepositoryMetadata(Repo.class), factory);

            return new PartTreeMarklogicQuery(queryMethod, marklogicOperationsMock);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Criteria> extractListCriteria(Criteria criteria) {
        return (List<Criteria>) criteria.getCriteriaObject();
    }

    interface Repo extends MarklogicRepository<Person, Long> {

        Person findByLastname(String lastname);

        Person findByLastnameAndFirstname(String lastname, String firstname);

        Person findByLastnameAndAddressCountry(String lastname, String country);

        Person findByAddressCountry(String country);

        List<Person> findBySkills(String skill);

        List<Person> findBySkills(ArrayList<String> skills);

        List<Person> findBySkillsContaining(ArrayList<String> skills);

        List<Person> findByExtraCollections(String extraCollections);

        List<Person> findByExtraCollectionsIsNot(String extraCollections);

        Person findByActiveIsTrue();

        Person findByActiveIsFalse();

    }
}