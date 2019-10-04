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

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.SecurityOptions;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.XccConfigException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Convenient factory for configuring Marklogic ContentSource.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicFactoryBean extends AbstractFactoryBean<ContentSource> implements PersistenceExceptionTranslator {

    private static final PersistenceExceptionTranslator DEFAULT_EXCEPTION_TRANSLATOR = new MarklogicExceptionTranslator();

    private URI uri;
    private PersistenceExceptionTranslator exceptionTranslator = DEFAULT_EXCEPTION_TRANSLATOR;

    /**
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public Class<?> getObjectType() {
        return ContentSource.class;
    }

    @Override
    public ContentSource createInstance() throws Exception {
        if (uri.getScheme().equals("xccs")) {
            return createSecuredInstance();
        }

        return ContentSourceFactory.newContentSource(uri);
    }

    /**
     * Construct an instance that may be used to insert content.
     *
     * @throws XccConfigException
     *             Thrown if a {@link Session} cannot be created. This usually indicates that the
     *             host/port or user credentials are incorrect.
     */
    private ContentSource createSecuredInstance() throws Exception {
        TrustManager[] trustManagers;


        // Trust anyone.
        trustManagers = new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {
                // nothing to do
            }

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                    throws CertificateException {
                // nothing to do
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };

        String clientJKS = null;

        String passphrase = "";

        KeyManager[] keyManagers;

        keyManagers = null;

        // Get an SSLContext that supports the desired protocol; SSLv3 or TLSv1.
        SSLContext sslContext = SSLContext.getInstance("TLSv1");

        // Initialize the SSL context with key and trust managers.
        sslContext.init(keyManagers, trustManagers, null);

        // Create a security options object for use by the secure content source.
        SecurityOptions securityOptions = new SecurityOptions(sslContext);

        // Limit acceptable protocols; SSLv3 and/or TLSv1 (optional)
        securityOptions.setEnabledProtocols(new String[] { "TLSv1" });

        // Limit acceptable cipher suites. (optional)
        // See ciphers man page or TLS 1.0 / SSL 3.0 specifications.
        securityOptions.setEnabledCipherSuites(new String[] { "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA" });

        return ContentSourceFactory.newContentSource(uri, securityOptions);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }


}
