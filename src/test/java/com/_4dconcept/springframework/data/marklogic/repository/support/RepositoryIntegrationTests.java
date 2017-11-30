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

import com._4dconcept.springframework.data.marklogic.config.AbstractMarklogicConfiguration;
import com._4dconcept.springframework.data.marklogic.repository.Address;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import com._4dconcept.springframework.data.marklogic.repository.config.EnableMarklogicRepositories;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the repository layer.
 *
 * @author Stéphane Toussaint
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RepositoryIntegrationTests {

    @Autowired
    private PersonRepository repository;

    private Person steph;
    private Person sahbi;
    private Person another;

    private List<Person> all;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        repository.deleteAll();

        steph = new Person(null,"Stéphane", "Toussaint", 38, "France");
        sahbi = new Person(null, "Sahbi", "Ktifa", 28, "France");
        another = new Person(null, "Another", "One", 28, "England");

        all = repository.save(Arrays.asList(steph, sahbi, another));
    }

    @Test
    public void countAllInRepository() throws Exception {
        assertThat(repository.count(), is(3L));
    }

    @Test
    public void countBySample() throws Exception {
        Person samplePerson = new Person();
        samplePerson.setLastname("Toussaint");
        assertThat(repository.count(Example.of(samplePerson)), is(1L));
    }

    @Test
    public void checkPersonExists() throws Exception {
        assertThat(repository.exists(steph.getId()), is(true));
    }

    @Test
    public void checkPersonExistsBySample() throws Exception {
        Person samplePerson = new Person();
        samplePerson.setLastname("Toussaint");
        assertThat(repository.exists(Example.of(samplePerson)), is(true));
    }

    @Test
    public void findPersonById() {
        Person person = repository.findOne(steph.getId());
        assertThat(person, notNullValue());
        assertThat(person.getId(), is(steph.getId()));
        assertThat(person.getFirstname(), is("Stéphane"));
    }

    @Test
    public void findByIdWithMethodQuery() {
        Person person = repository.findById(steph.getId());
        assertThat(person, notNullValue());
        assertThat(person.getId(), is(steph.getId()));
        assertThat(person.getFirstname(), is("Stéphane"));
    }

    @Test
    public void findOnePersonByExample() {
        Person person = new Person();
        person.setLastname("Toussaint");
        final Person result = repository.findOne(Example.of(person));
        assertThat(result, notNullValue());
        assertThat(result.getId(), is(steph.getId()));
        assertThat(result.getFirstname(), is("Stéphane"));
    }

    @Test
    public void throwExceptionWhenFindOneReturnMultiplePersons() {
        thrown.expect(IncorrectResultSizeDataAccessException.class);
        thrown.expectMessage("Incorrect result size: expected 1, actual 2");

        Person person = new Person();
        Address address = new Address();
        address.setCountry("France");
        person.setAddress(address);

        repository.findOne(Example.of(person));
    }

    @Test
    public void findAllInRepository() {
        List<Person> result = repository.findAll();
        assertThat(result, hasSize(all.size()));
    }

    @Test
    public void findAllById() {
        List<Person> result = repository.findAll(Arrays.asList(sahbi.getId(), steph.getId()));
        assertThat(result, hasSize(2));
        assertThat(result.stream().map(Person::getLastname).collect(Collectors.toList()), containsInAnyOrder("Toussaint", "Ktifa"));
    }

    @Test
    public void findAllInRepositorySortedOrder() {
        checkOrder(new Sort("lastname"), new String[] {"Ktifa", "One", "Toussaint"});
        checkOrder(new Sort(Sort.Direction.DESC, "lastname"), new String[] {"Toussaint", "One", "Ktifa"});
        checkOrder(new Sort(Sort.Direction.DESC, "age", "lastname"), new String[] {"Toussaint", "One", "Ktifa"});
        checkOrder(new Sort(Sort.Direction.DESC, "age").and(new Sort("lastname")), new String[] {"Toussaint", "Ktifa", "One"});
    }

    @Test
    public void findAllWithPagination() {
        Page<Person> pageResult = repository.findAll(new PageRequest(0, 2));
        assertThat(pageResult.getTotalElements(), is(3L));
        assertThat(pageResult.getTotalPages(), is(2));
        assertThat(pageResult.getContent(), hasSize(2));
    }

    @Test
    public void insertPersonWithNoId() {
        Person person = new Person(null,"James", "Bond", 38, "France");
        repository.save(person);
        assertThat(person.getId(), notNullValue());
    }

    @Test
    public void updatePerson() {
        Person person = repository.findOne(sahbi.getId());
        person.setAge(425);
        person.setFirstname("Duncan");
        person.setLastname("MacLeod");
        repository.save(person);
        assertThat(person.getId(), is(sahbi.getId()));

        assertThat(repository.findOne(sahbi.getId()).getAge(), is(425));
    }

    @Test
    public void deletePerson() {
        repository.delete(steph);

        assertThat(repository.findOne(steph.getId()), nullValue());

        List<Person> result = repository.findAll();
        assertThat(result, hasSize(all.size() - 1));
    }

    @Test
    public void existsPerson() {
        assertThat(repository.exists(steph.getId()), is(true));
        assertThat(repository.exists("unknown"), is(false));
    }

    @Test
    public void findPersonByExample() {
        Person person = new Person();
        person.setLastname("Toussaint");
        Iterable<Person> result = repository.findAll(Example.of(person));
        assertThat(result, notNullValue());

        Person byAgeSample = new Person();
        byAgeSample.setAge(28);
        assertThat(repository.findAll(Example.of(byAgeSample)), hasSize(2));
    }

    private void checkOrder(Sort sort, String[] lastnames) {
        List<Person> result = repository.findAll(sort);
        assertThat(result.get(0).getLastname(), is(lastnames[0]));
        assertThat(result.get(1).getLastname(), is(lastnames[1]));
        assertThat(result.get(2).getLastname(), is(lastnames[2]));
    }

    static class PersonConverter implements Converter<Person, Serializable> {
        @Override
        public Serializable convert(Person source) {
            try {
                Marshaller marshaller = JAXBContext.newInstance(Person.class).createMarshaller();
                StringWriter writer = new StringWriter();
                marshaller.marshal(source, writer);
                return writer.toString() + "<!-- Converted by PersonConverter -->"; // Add this comment to show the content converted by this converter and not the generic one.
            } catch (JAXBException jaxbe) {
                throw new RuntimeException(jaxbe);
            }
        }
    }

    @Configuration
    @EnableMarklogicRepositories
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @PropertySource(value = "integration-test.properties", ignoreResourceNotFound = true)
    static class TestConfig extends AbstractMarklogicConfiguration {

        @Value("${marklogic.uri}")
        private String marklogicUri;

        @Override
        public URI getMarklogicUri() {
            return URI.create(marklogicUri);
        }

        @Override
        protected List<Object> getConverters() {
            return Collections.singletonList(new PersonConverter());
        }

    }

}