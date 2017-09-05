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
package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.Session;
import org.springframework.core.NamedThreadLocal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An adapter for a target XDBC {@link com.marklogic.xcc.ContentSource}, applying the specified
 * user credentials to every standard {@code newSession()} call, implicitly
 * invoking {@code newSession(username, password)} on the target.
 * All other methods simply delegate to the corresponding methods of the
 * target ContentSource.
 *
 * <p>Can be used to proxy a target JNDI ContentSource that does not have user
 * credentials configured. Client code can work with this ContentSource as usual,
 * using the standard {@code newSession()} call.
 *
 * <p>In the following example, client code can simply transparently work with
 * the preconfigured "myContentSource", implicitly accessing "myTargetContentSource"
 * with the specified user credentials.
 *
 * <pre class="code">
 * &lt;bean id="myTargetContentSource" class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/xdbc/mycs"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myContentSource" class="com._4dconcept.contentfactory.extra.datasource.UserCredentialsContentSourceAdapter"&gt;
 *   &lt;property name="targetContentSource" ref="myTargetContentSource"/&gt;
 *   &lt;property name="username" value="myusername"/&gt;
 *   &lt;property name="password" value="mypassword"/&gt;
 * &lt;/bean></pre>
 *
 * <p>If the "username" is empty, this proxy will simply delegate to the
 * standard {@code newSession()} method of the target ContentSource.
 * This can be used to keep a UserCredentialsContentSourceAdapter bean definition
 * just for the <i>option</i> of implicitly passing in user credentials if
 * the particular target ContentSource requires it.
 *
 * @author Stéphane Toussaint
 * @author Juergen Hoeller
 *
 * @see #newSession
 */
public class UserCredentialsContentSourceAdapter extends DelegatingContentSource {

    private final ThreadLocal<XdbcUserCredentials> threadBoundCredentials = new NamedThreadLocal<>("Current XDBC user credentials");
    private String username;
    private String password;

    /**
     * Set the default username that this adapter should use for retrieving Sessions.
     * <p>Default is no specific user. Note that an explicitly specified username
     * will always override any username/password specified at the ContentSource level.
     * @see #setPassword
     * @see #setCredentialsForCurrentThread(String, String)
     * @see #newSession(String, String)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set the default user's password that this adapter should use for retrieving Sessions.
     * <p>Default is no specific password. Note that an explicitly specified username
     * will always override any username/password specified at the ContentSource level.
     * @see #setUsername
     * @see #setCredentialsForCurrentThread(String, String)
     * @see #newSession(String, String)
     */
    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * Set user credententials for this proxy and the current thread.
     * The given username and password will be applied to all subsequent
     * {@code newSession()} calls on this ContentSource proxy.
     * <p>This will override any statically specified user credentials,
     * that is, values of the "username" and "password" bean properties.
     * @param username the username to apply
     * @param password the password to apply
     * @see #removeCredentialsFromCurrentThread
     */
    public void setCredentialsForCurrentThread(String username, String password) {
        this.threadBoundCredentials.set(new XdbcUserCredentials(username, password));
    }

    /**
     * Remove any user credentials for this proxy from the current thread.
     * Statically specified user credentials apply again afterwards.
     * @see #setCredentialsForCurrentThread
     */
    public void removeCredentialsFromCurrentThread() {
        this.threadBoundCredentials.remove();
    }


    /**
     * Determine whether there are currently thread-bound credentials,
     * using them if available, falling back to the statically specified
     * username and password (i.e. values of the bean properties) else.
     * <p>Delegates to {@link #doGetSession(String, String)} with the
     * determined credentials as parameters.
     */
    @Override
    public Session newSession() {
        XdbcUserCredentials threadCredentials = this.threadBoundCredentials.get();
        if (threadCredentials != null) {
            return doGetSession(threadCredentials.username, threadCredentials.password);
        } else {
            return doGetSession(this.username, this.password);
        }
    }

    /**
     * Simply delegates to {@link #doGetSession(String, String)},
     * keeping the given user credentials as-is.
     */
    @Override
    public Session newSession(String username, String password) {
        return doGetSession(username, password);
    }

    /**
     * This implementation delegates to the {@code newSession(username, password)}
     * method of the target ContentSource, passing in the specified user credentials.
     * If the specified username is empty, it will simply delegate to the standard
     * {@code newSession()} method of the target ContentSource.
     * @param username the username to use
     * @param password the password to use
     * @return the Session
     * @see com.marklogic.xcc.ContentSource#newSession(String, String)
     * @see com.marklogic.xcc.ContentSource#newSession()
     */
    protected Session doGetSession(String username, String password) {
        Assert.state(getTargetContentSource() != null, "'targetContentSource' is required");
        if (StringUtils.hasLength(username)) {
            return getTargetContentSource().newSession(username, password);
        } else {
            return getTargetContentSource().newSession();
        }
    }


    /**
     * Inner class used as ThreadLocal value.
     */
    private static class XdbcUserCredentials {

        public final String username;

        public final String password;

        private XdbcUserCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            return "XdbcUserCredentials[username='" + this.username + "',password='" + this.password + "']";
        }
    }

}
