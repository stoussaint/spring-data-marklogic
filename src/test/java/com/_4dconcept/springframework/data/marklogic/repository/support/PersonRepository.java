package com._4dconcept.springframework.data.marklogic.repository.support;

import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.springframework.stereotype.Repository;

/**
 * Test repository based on {@link Person} entity.
 *
 * @author stoussaint
 * @since 2017-11-30
 */
@Repository
interface PersonRepository extends MarklogicRepository<Person, String> {
}
