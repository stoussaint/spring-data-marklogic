package org.springframework.data.marklogic.core.cts;

import org.junit.Test;
import org.springframework.data.marklogic.core.query.Criteria;
import org.springframework.data.marklogic.core.query.Query;
import org.springframework.data.marklogic.core.query.SortCriteria;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-01
 */
public class CTSQueryParserTest {

    @Test
    public void parseEmptyQuery() throws Exception {
        String ctsQuery = new CTSQueryParser(new Query()).asCtsQuery();

        assertThat(ctsQuery, is("cts:search(fn:collection(), (), ())"));
    }

    @Test
    public void parsePopulatedQuery() throws Exception {
        Query query = new Query();
        query.setCriteria(Arrays.asList(
                new Criteria(new QName("name"), "Me"),
                new Criteria(new QName("address"), Arrays.asList(
                        new Criteria(new QName("town"), "Paris")
                ))));
        String ctsQuery = new CTSQueryParser(query).asCtsQuery();

        assertThat(ctsQuery, is("cts:search(fn:collection(), cts:and-query((cts:element-value-query(fn:QName('', 'name'), 'Me'), cts:element-query(fn:QName('', 'address'), cts:and-query((cts:element-value-query(fn:QName('', 'town'), 'Paris')))))), ())"));
    }

    @Test
    public void parseQueryWithPagination() throws Exception {
        Query query = new Query();
        query.setCollection("Collection1");
        query.setLimit(10);
        query.setSkip(0);
        String ctsQuery = new CTSQueryParser(query).asCtsQuery();

        assertThat(ctsQuery, is("cts:search(fn:collection('Collection1'), (), ())[1 to 10]"));
    }

    @Test
    public void parseQueryWithSortOrders() throws Exception {
        Query query = new Query();
        query.setCollection("Collection1");
        query.setSortCriteria(Arrays.asList(
                new SortCriteria(new QName("", "age"), true),
                new SortCriteria(new QName("", "lastname"))
        ));
        String ctsQuery = new CTSQueryParser(query).asCtsQuery();

        assertThat(ctsQuery, is("cts:search(fn:collection('Collection1'), (), (cts:index-order(cts:element-reference(fn:QName('', 'age')), ('descending')), cts:index-order(cts:element-reference(fn:QName('', 'lastname')), ('ascending'))))"));
    }
}