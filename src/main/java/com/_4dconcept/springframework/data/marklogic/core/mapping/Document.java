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
package com._4dconcept.springframework.data.marklogic.core.mapping;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marker for entity manageable by Marklogic
 *
 * @author Stéphane Toussaint
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Collection
public @interface Document {

    String uri() default "";

    boolean idInPropertyFragment() default false;

    /**
     * @deprecated This annotation attribute will be removed in future version, replaced by {@link Collection#prefix()}
     * @return the primary collection prefix
     */
    @Deprecated
    @AliasFor(annotation = Collection.class, attribute = "prefix")
    String defaultCollectionPrefix() default "";

    /**
     * @deprecated This annotation attribute will be removed in future version, replaced by {@link Collection#value()}
     * @return the primary collection name
     */
    @Deprecated
    @AliasFor(annotation = Collection.class, attribute = "value")
    String defaultCollection() default "";

}