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
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicContentHolder;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.BasicMarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.DocumentFormat;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.impl.AdhocImpl;
import com.marklogic.xcc.impl.ResultItemImpl;
import com.marklogic.xcc.types.impl.XsStringImpl;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.Scanner;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class MarklogicTemplateTest {

    @Mock
    private ContentSource contentSource;

    @Mock
    private Session session;

    @Mock
    private MarklogicConverter marklogicConverter;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ResultSequence resultSequence;

    @Mock
    private MarklogicCollectionUtils marklogicCollectionUtils;

    @Captor
    private
    ArgumentCaptor<Content> contentArgumentCaptor;

    @Captor
    private
    ArgumentCaptor<String> queryArgumentCaptor;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        when(contentSource.newSession()).thenReturn(session);
        MappingContext<BasicMarklogicPersistentEntity<?>, MarklogicPersistentProperty> marklogicMappingContext = new MarklogicMappingContext();
        when(marklogicConverter.getMappingContext()).thenReturn((MappingContext)marklogicMappingContext);
        when(marklogicConverter.getConversionService()).thenReturn(conversionService);
        when(session.submitRequest(any(Request.class))).thenReturn(resultSequence);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullContentSource() {
        new MarklogicTemplate(null);
    }

    @Test
    public void defaultsConverterToMappingMarklogicConverter() {
        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        assertTrue(ReflectionTestUtils.getField(template, "marklogicConverter") instanceof MarklogicMappingConverter);
    }

    @Test
    public void insertionOfSimpleEntityWithoutSpecificOptions() throws Exception {
        final String SAMPLE_CONTENT = "<simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        when(conversionService.convert(any(UUID.class), eq(String.class))).thenReturn("generatedId");

        doAnswer(invocationOnMock -> {
            MarklogicContentHolder holder = invocationOnMock.getArgumentAt(1, MarklogicContentHolder.class);
            holder.setContent(SAMPLE_CONTENT);
            return null;
        }).when(marklogicConverter).write(Mockito.any(SimpleEntity.class), Mockito.any(MarklogicContentHolder.class));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.setMarklogicCollectionUtils(marklogicCollectionUtils);
        template.insert(new SimpleEntity(null, "entity"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/content/simpleentity/generatedId.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(SAMPLE_CONTENT));
    }

    @Test
    public void insertionOfSimpleEntityWithExplicitUri() throws Exception {
        final String SAMPLE_CONTENT = "<simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        doAnswer(invocationOnMock -> {
            MarklogicContentHolder holder = invocationOnMock.getArgumentAt(1, MarklogicContentHolder.class);
            holder.setContent(SAMPLE_CONTENT);
            return null;
        }).when(marklogicConverter).write(Mockito.any(SimpleEntity.class), Mockito.any(MarklogicContentHolder.class));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.setMarklogicCollectionUtils(marklogicCollectionUtils);
        template.insert(new SimpleEntity(null, "entity"), buildCreateOperationOptions("/test/entity/1.xml"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/test/entity/1.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(SAMPLE_CONTENT));
    }

    @Test
    public void saveWithoutIdFallbackToInsert() throws Exception {
        final String SAMPLE_CONTENT = "<simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        when(conversionService.convert(any(UUID.class), eq(String.class))).thenReturn("generatedId");

        doAnswer(invocationOnMock -> {
            MarklogicContentHolder holder = invocationOnMock.getArgumentAt(1, MarklogicContentHolder.class);
            holder.setContent(SAMPLE_CONTENT);
            return null;
        }).when(marklogicConverter).write(Mockito.any(SimpleEntity.class), Mockito.any(MarklogicContentHolder.class));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.setMarklogicCollectionUtils(marklogicCollectionUtils);
        template.save(new SimpleEntity(null, "entity"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/content/simpleentity/generatedId.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(SAMPLE_CONTENT));
    }

    @Test
    public void saveWithSpecificIdFallbackToInsert() throws Exception {
        final String SAMPLE_CONTENT = "<simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        when(session.newAdhocQuery(eq("cts:uris((), (), cts:and-query((cts:element-value-query(fn:QName(\"\", \"id\"), \"1\"))))"))).thenReturn(new AdhocImpl(session, null, new RequestOptions()));

        doAnswer(invocationOnMock -> {
            MarklogicContentHolder holder = invocationOnMock.getArgumentAt(1, MarklogicContentHolder.class);
            holder.setContent(SAMPLE_CONTENT);
            return null;
        }).when(marklogicConverter).write(Mockito.any(SimpleEntity.class), Mockito.any(MarklogicContentHolder.class));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.setMarklogicCollectionUtils(marklogicCollectionUtils);
        template.save(new SimpleEntity("1", "entity"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/content/simpleentity/1.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(SAMPLE_CONTENT));
    }

    @Test
    public void removeEntity() {
        AdhocImpl request = new AdhocImpl(session, null, new RequestOptions());
        when(session.newAdhocQuery(any(String.class))).thenReturn(request);
        when(resultSequence.isEmpty()).thenReturn(false);
        when(resultSequence.hasNext()).thenReturn(true, false, true, false);
        when(resultSequence.next()).thenReturn(new ResultItemImpl(null, 0, null, null), new ResultItemImpl(new XsStringImpl("/test/entity/1.xml"), 0, null, null));
        when(marklogicConverter.read(eq(SimpleEntity.class), any(MarklogicContentHolder.class))).thenReturn(new SimpleEntity("1", "entity"));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.remove("1", SimpleEntity.class);

    }

    @Test(expected = ConverterNotFoundException.class)
    public void rejectsInsertionOfNonAnnotatedEntity() {
        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        template.setMarklogicCollectionUtils(marklogicCollectionUtils);
        template.insert(new NonAnnotatedEntity("1", "entity"), buildCreateOperationOptions("/test/entity/1.xml"));
    }

    @Test
    public void findByQuery() {
        when(session.newAdhocQuery(anyString())).thenReturn(new AdhocImpl(null, null, new RequestOptions()));

        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        Query query = new Query();
        query.setCriteria(new Criteria(new QName("", "name"), "test"));

        template.find(query, SimpleEntity.class);

        verify(session).newAdhocQuery(queryArgumentCaptor.capture());
        assertThat(queryArgumentCaptor.getValue(), is("cts:search(fn:collection(), cts:element-value-query(fn:QName('', 'name'), 'test'), ())"));
    }

    @Test
    public void findById_EnsureExactMatch() {
        when(session.newAdhocQuery(anyString())).thenReturn(new AdhocImpl(null, null, new RequestOptions()));
        MarklogicTemplate template = new MarklogicTemplate(contentSource);

        template.findById("1", SimpleEntity.class);

        verify(session).newAdhocQuery(queryArgumentCaptor.capture());
        assertThat(queryArgumentCaptor.getValue(), is("cts:search(fn:collection(),cts:element-value-query(fn:QName(\"\", \"id\"), \"1\", \"exact\"))"));
    }

    static class SimpleEntity {

        String id;
        String name;

        SimpleEntity() {}

        SimpleEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }
    }

    static class NonAnnotatedEntity {
        String id;
        String name;

        NonAnnotatedEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static String toString(InputStream input) {
        return new Scanner(input).useDelimiter("\\Z").next();
    }

    private MarklogicCreateOperationOptions buildCreateOperationOptions(String uri) {
        return new MarklogicCreateOperationOptions() {
            @Override
            public String uri() {
                return uri;
            }

            @Override
            public String[] extraCollections() {
                return null;
            }

            @Override
            public String defaultCollection() {
                return null;
            }
        };
    }

}