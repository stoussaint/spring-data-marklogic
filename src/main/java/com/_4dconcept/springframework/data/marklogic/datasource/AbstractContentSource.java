package com._4dconcept.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.spi.ConnectionProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.logging.Logger;

/**
 * Abstract base class for Spring's {@link ContentSource}
 * implementations, taking care of the padding.
 *
 * @author Stéphane Toussaint
 * @author Juergen Hoeller
 * @since 07.05.2003
 */
public abstract class AbstractContentSource implements ContentSource {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * DefaultLogger methods are not supported.
	 */
	@Override
	public Logger getDefaultLogger() {
		throw new UnsupportedOperationException("getDefaultLogger");
	}

	/**
	 * DefaultLogger methods are not supported.
	 */
	@Override
	public void setDefaultLogger(Logger log) {
		throw new UnsupportedOperationException("setDefaultLogger");
	}

	@Override
	public ConnectionProvider getConnectionProvider() {
		throw new UnsupportedOperationException("getConnectionProvider");
	}

	@Override
	public boolean isAuthenticationPreemptive() {
		throw new UnsupportedOperationException("isAuthenticationPreemptive");
	}

	@Override
	public void setAuthenticationPreemptive(boolean b) {
		throw new UnsupportedOperationException("setAuthenticationPreemptive");
	}
}
