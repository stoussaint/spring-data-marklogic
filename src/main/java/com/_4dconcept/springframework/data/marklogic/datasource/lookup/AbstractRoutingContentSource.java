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
package com._4dconcept.springframework.data.marklogic.datasource.lookup;

import com._4dconcept.springframework.data.marklogic.datasource.AbstractContentSource;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract {@link ContentSource} implementation that routes {@link #newSession()}
 * calls to one of various target ContentSources based on a lookup key. The latter is usually
 * (but not necessarily) determined through some thread-bound transaction context.
 *
 * @author Stéphane Toussaint
 * @author Juergen Hoeller
 *
 * @see #setTargetContentSources
 * @see #setDefaultTargetContentSource
 * @see #determineCurrentLookupKey()
 */
public abstract class AbstractRoutingContentSource extends AbstractContentSource implements InitializingBean {

    private Map<Object, Object> targetContentSources;

    private Object defaultTargetContentSource;

    private boolean lenientFallback = true;

    private Map<Object, ContentSource> resolvedContentSources;

    private ContentSource resolvedDefaultContentSource;


    /**
     * Specify the map of target ContentSources, with the lookup key as key.
     * The mapped value can either be a corresponding {@link ContentSource}
     * instance or a data source name String (to be resolved via a
     * <p>The key can be of arbitrary type; this class implements the
     * generic lookup process only. The concrete key representation will
     * be handled by {@link #resolveSpecifiedLookupKey(Object)} and
     * {@link #determineCurrentLookupKey()}.
     * @param targetContentSources the map of target ContentSources
     */
    public void setTargetContentSources(Map<Object, Object> targetContentSources) {
        this.targetContentSources = targetContentSources;
    }

    /**
     * Specify the default target ContentSource, if any.
     * <p>The mapped value can either be a corresponding {@link ContentSource}
     * instance or a data source name String (to be resolved via a
     * <p>This ContentSource will be used as target if none of the keyed
     * {@link #setTargetContentSources targetContentSources} match the
     * {@link #determineCurrentLookupKey()} current lookup key.
     * @param defaultTargetContentSource the default target ContentSource
     */
    public void setDefaultTargetContentSource(Object defaultTargetContentSource) {
        this.defaultTargetContentSource = defaultTargetContentSource;
    }

    /**
     * Specify whether to apply a lenient fallback to the default ContentSource
     * if no specific ContentSource could be found for the current lookup key.
     * <p>Default is "true", accepting lookup keys without a corresponding entry
     * in the target ContentSource map - simply falling back to the default ContentSource
     * in that case.
     * <p>Switch this flag to "false" if you would prefer the fallback to only apply
     * if the lookup key was {@code null}. Lookup keys without a ContentSource
     * entry will then lead to an IllegalStateException.
     * @see #setTargetContentSources
     * @see #setDefaultTargetContentSource
     * @see #determineCurrentLookupKey()
     *
     * @param lenientFallback whether to apply a lenient fallback to the default ContentSource
     */
    public void setLenientFallback(boolean lenientFallback) {
        this.lenientFallback = lenientFallback;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.targetContentSources == null) {
            throw new IllegalArgumentException("Property 'targetContentSources' is required");
        }
        this.resolvedContentSources = new HashMap<>(this.targetContentSources.size());
        for (Map.Entry<Object, Object> entry : this.targetContentSources.entrySet()) {
            Object lookupKey = resolveSpecifiedLookupKey(entry.getKey());
            ContentSource contentSource = resolveSpecifiedContentSource(entry.getValue());
            this.resolvedContentSources.put(lookupKey, contentSource);
        }
        if (this.defaultTargetContentSource != null) {
            this.resolvedDefaultContentSource = resolveSpecifiedContentSource(this.defaultTargetContentSource);
        }
    }

    /**
     * Resolve the given lookup key object, as specified in the
     * {@link #setTargetContentSources targetContentSources} map, into
     * the actual lookup key to be used for matching with the
     * {@link #determineCurrentLookupKey() current lookup key}.
     * <p>The default implementation simply returns the given key as-is.
     * @param lookupKey the lookup key object as specified by the user
     * @return the lookup key as needed for matching
     */
    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        return lookupKey;
    }

    /**
     * Resolve the specified data source object into a ContentSource instance.
     * @param contentSource the data source value object as specified in the
     * {@link #setTargetContentSources targetContentSources} map
     * @return the resolved ContentSource (never {@code null})
     * @throws IllegalArgumentException in case of an unsupported value type
     */
    private ContentSource resolveSpecifiedContentSource(Object contentSource) throws IllegalArgumentException {
        if (contentSource instanceof ContentSource) {
            return (ContentSource) contentSource;
        } else {
            throw new IllegalArgumentException(
                    "Illegal data source value - only [ContentSource] supported: " + contentSource);
        }
    }


    @Override
    public Session newSession() {
        return determineTargetContentSource().newSession();
    }

    @Override
    public Session newSession(String s) {
        return determineTargetContentSource().newSession(s);
    }

    @Override
    public Session newSession(String s, String s1, String s2) {
        return determineTargetContentSource().newSession(s, s1, s2);
    }

    @Override
    public Session newSession(String username, String password) {
        return determineTargetContentSource().newSession(username, password);
    }

    /**
     * Retrieve the current target ContentSource. Determines the
     * {@link #determineCurrentLookupKey() current lookup key}, performs
     * a lookup in the {@link #setTargetContentSources targetContentSources} map,
     * falls back to the specified
     * {@link #setDefaultTargetContentSource default target ContentSource} if necessary.
     * @see #determineCurrentLookupKey()
     * @return the current target ContentSource
     */
    private ContentSource determineTargetContentSource() {
        Assert.notNull(this.resolvedContentSources, "ContentSource router not initialized");
        Object lookupKey = determineCurrentLookupKey();
        ContentSource contentSource = this.resolvedContentSources.get(lookupKey);
        if (contentSource == null && (this.lenientFallback || lookupKey == null)) {
            contentSource = this.resolvedDefaultContentSource;
        }
        if (contentSource == null) {
            throw new IllegalStateException("Cannot determine target ContentSource for lookup key [" + lookupKey + "]");
        }
        return contentSource;
    }

    /**
     * Determine the current lookup key. This will typically be
     * implemented to check a thread-bound transaction context.
     * <p>Allows for arbitrary keys. The returned key needs
     * to match the stored lookup key type, as resolved by the
     * {@link #resolveSpecifiedLookupKey} method.
     * @return the current lookup key
     */
    @Nullable
    protected abstract Object determineCurrentLookupKey();

}
