package org.springframework.data.marklogic.repository;

import org.springframework.data.annotation.Id;

import java.util.UUID;

/**
 * Sample contact domain class.
 * 
 * @author Stéphane Toussaint
 */
public abstract class Contact {

	@Id
	protected String id;

	public String getId() {
		return id;
	}
}
