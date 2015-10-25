/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JeeLink filter which converts text to message object with help of regular expressions.
 * @author ribbeck
 * @since 1.7.2
 */
public class JeeLinkFilterRegex implements JeeLinkFilter {
	
	final Pattern pattern;
	
	/**
	 * Constructor to initialize the filter with a regular expression.<br>
	 * The expression must match the complete line. 
	 * The first group must capture the raw message which can be passe to {@link JeeLinkMessage#parseSerialString(String)}.
	 * @param regex Regular expression
	 */
	public JeeLinkFilterRegex(String regex) {
		pattern = Pattern.compile(regex);
	}
	
	@Override
	public JeeLinkMessage processLine(String line) throws ParseException {
		
		final Matcher matcher = pattern.matcher(line);
		if (matcher.matches()) {
			final String data = matcher.group(1);
			return JeeLinkMessage.parseSerialString(data);
		}
		return null;
	}

}
