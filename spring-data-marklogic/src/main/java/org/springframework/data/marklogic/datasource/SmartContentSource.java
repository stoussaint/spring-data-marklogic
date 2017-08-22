package org.springframework.data.marklogic.datasource;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.Session;

/**
 * Extension of the {@code com.marklogic.xcc.ContentSource} interface, to be
 * implemented by special ContentSources that return XDBC Sessions
 * in an unwrapped fashion.
 *
 * <p>Classes using this interface can query whether or not the Session
 * should be closed after an operation. Spring's ContentSourceUtils and
 * XdbcTemplate classes automatically perform such a check.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Toussaint Stephane
 * @see ContentSourceUtils#releaseSession
 */
public interface SmartContentSource extends ContentSource {

	/**
	 * Should we close this Session, obtained from this ContentSource?
	 * <p>Code that uses Sessions from a SmartContentSource should always
	 * perform a check via this method before invoking {@code close()}.
	 * @param ses the Session to check
	 * @return whether the given Session should be closed
	 * @see Session#close()
	 */
	boolean shouldClose(Session ses);

}
