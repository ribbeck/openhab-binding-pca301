/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A filter which checks if a valid sketch is installed on the JeeLink device.<br>
 * In case of invalid sketch a {@link InvalidSketchException} is thrown.
 * @author ribbeck
 * @since 1.7.2
 */
public class JeeLinkFilterSketch implements JeeLinkFilter {

	private final static String SKETCH_NAME = "pcaSerial";
	
	final Pattern pattern = Pattern.compile(SKETCH_NAME + "\\.(\\d+)\\.(\\d+)");
	
	final int versionMajor;
	final int versionMinor;
	
	
	/**
	 * Constructor to specify minimum sketch version.
	 * @param major major release of minimum version
	 * @param minor minor release of minimum version
	 */
	public JeeLinkFilterSketch(int major, int minor) {
		versionMajor = major;
		versionMinor = minor;
	}
		
	@Override
	public JeeLinkMessage processLine(String line) throws InvalidSketchException {
		
		if ((line != null) && line.startsWith("[") && line.endsWith("]")) {
			
			final Matcher matcher = pattern.matcher(line);
			if (!matcher.find()) {
				throw new InvalidSketchException("Wrong sketch. Please install " + SKETCH_NAME);
			}
			try {
				
				// parse version
				final int major = Integer.parseInt(matcher.group(1));
				final int minor = Integer.parseInt(matcher.group(2));
				
				// check version
				if (major < versionMajor) {
					throw new InvalidSketchException("Invalid sketch version. Please install at least version " + versionMajor + "." + versionMinor);
				}
				
				if ((major == versionMajor) && (minor < versionMinor)) {
					throw new InvalidSketchException("Invalid sketch version. Please install at least version " + versionMajor + "." + versionMinor);
				}
				
				
			} catch (NumberFormatException e) {
				throw new InvalidSketchException("Invalid sketch signature: " + line, e);
			}
		}
		return null;
	}

	
	/**
	 * Exception which indicates a invalid sketch on JeeLink device-
	 * @author ribbeck
	 * @since 1.7.2 
	 */
	public class InvalidSketchException extends Exception {

		private static final long serialVersionUID = -8169700417319797380L;
		
		
		public InvalidSketchException() {
			super();
		}
		
		public InvalidSketchException(String message) {
			super(message);
		}
		
		public InvalidSketchException(String message, Throwable cause) {
			super(message, cause);
		}

	}
}
