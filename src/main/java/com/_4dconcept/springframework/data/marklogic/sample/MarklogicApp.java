package com._4dconcept.springframework.data.marklogic.sample;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com.marklogic.xcc.ContentSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MarklogicApp {

    private static final Logger log = LoggerFactory.getLogger(MarklogicApp.class);

    public static void main(String[] args) throws Exception {
        MarklogicMappingConverter marklogicMappingConverter = new MarklogicMappingConverter(new MarklogicMappingContext());
        marklogicMappingConverter.afterPropertiesSet();

        MarklogicOperations marklogicOps = new MarklogicTemplate(
                ContentSourceFactory.newContentSource("localhost", 8888, "admin", "admin"),
                marklogicMappingConverter
        );

        Person p = new Person("Joe", 34);

        // Insert is used to initially store the object into the database.
        marklogicOps.insert(p);
        log.info("Insert: " + p);

        // Find
        p = marklogicOps.findById(p.getId(), Person.class);
        log.info("Found: " + p);

        // Update
        p.setAge(35);
        marklogicOps.save(p);
        p = marklogicOps.findOne(null, Person.class);
        log.info("Updated: " + p);

        // Delete
        marklogicOps.remove(p);

        // Check that deletion worked
        List<Person> people =  marklogicOps.findAll(Person.class);
        log.info("Number of people = : " + people.size());

    }
}