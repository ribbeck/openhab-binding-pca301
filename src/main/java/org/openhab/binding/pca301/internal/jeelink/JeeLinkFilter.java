/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;


/**
 * Interface for classes which can process a JeeLink text line. 
 * @author ribbeck
 * @since 1.7.2
 */
public interface JeeLinkFilter {

	/**
	 * Converts a JeeLink text line into a message object.
	 * @param line text line
	 * @return a message object or null
	 * @throws Exception An unexpected error occured.
	 */
	JeeLinkMessage processLine(String line) throws Exception;
}
 
