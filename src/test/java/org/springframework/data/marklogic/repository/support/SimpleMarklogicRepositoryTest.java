package org.springframework.data.marklogic.repository.support;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.exceptions.XccConfigException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Example;
import org.springframework.data.marklogic.core.MarklogicFactoryBean;
import org.springframework.data.marklogic.core.MarklogicTemplate;
import org.springframework.data.marklogic.core.convert.MarklogicConverter;
import org.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import org.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.springframework.data.marklogic.datasource.ContentSourceTransactionManager;
import org.springframework.data.marklogic.repository.Person;
import org.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link SimpleMarklogicRepository}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SimpleMarklogicRepositoryTest {

    @Autowired
    private SimpleMarklogicRepository<Person, String> repository;

    Person steph;
    Person sahbi;

    List<Person> all;

    @Before
    public void setUp() {
        repository.deleteAll();

        steph = new Person(null,"Stéphane", "Toussaint", 38);
        sahbi = new Person(null, "Sahbi", "Ktifa", 28);

        all = repository.save(Arrays.asList(steph, sahbi));
    }

    @Test
    public void findAllFromCustomCollectionName() {
        List<Person> result = repository.findAll();
        assertThat(result, hasSize(all.size()));
    }

    @Test
    public void insertPersonWithNoId() {
        Person person = new Person(null,"James", "Bond", 38);
        repository.save(person);
        assertThat(person.getId(), notNullValue());
    }

    @Test
    public void findPersonById() {
        Person person = repository.findOne(steph.getId());
        assertThat(person, notNullValue());
        assertThat(person.getId(), is(steph.getId()));
        assertThat(person.getFirstname(), is("Stéphane"));
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
    }

    private static class CustomizedPersonInformation implements MarklogicEntityInformation<Person, String> {

        @Override
        public boolean isNew(Person entity) {
            return entity.getId() == null;
        }

        @Override
        public String getId(Person entity) {
            return entity.getId();
        }

        @Override
        public Class<String> getIdType() {
            return String.class;
        }

        @Override
        public Class<Person> getJavaType() {
            return Person.class;
        }

        @Override
        public String getUri() {
            return "/person/#{id}.xml";
        }

        @Override
        public String getDefaultCollection() {
            return "Person";
        }

        @Override
        public boolean idInPropertyFragment() {
            return false;
        }
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
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        public MarklogicFactoryBean marklogicContentSource() {
            MarklogicFactoryBean marklogicFactoryBean = new MarklogicFactoryBean();
            marklogicFactoryBean.setUri(URI.create("xdbc://admin:admin@localhost:8888"));
            return marklogicFactoryBean;
        }

        @Bean
        public MarklogicTemplate marklogicTemplate(ContentSource contentSource, MarklogicConverter marklogicConverter) {
            MarklogicTemplate marklogicTemplate = new MarklogicTemplate(contentSource, marklogicConverter);
            return marklogicTemplate;
        }

        @Bean
        public MarklogicConverter marklogicConverter() {
            MarklogicMappingConverter marklogicConverter = new MarklogicMappingConverter(new MarklogicMappingContext());
            marklogicConverter.setConverters(Arrays.asList(personConverter()));
            return marklogicConverter;
        }

        @Bean
        public PersonConverter personConverter() {
            return new PersonConverter();
        }

        @Bean
        public ContentSourceTransactionManager transactionManager(ContentSource contentSource) throws XccConfigException {
            return new ContentSourceTransactionManager(contentSource);
        }

        @Bean
        public SimpleMarklogicRepository<Person, String> simpleMarklogicRepository(MarklogicTemplate marklogicTemplate) {
            return new SimpleMarklogicRepository<>(new CustomizedPersonInformation(), marklogicTemplate);
        }

    }

}