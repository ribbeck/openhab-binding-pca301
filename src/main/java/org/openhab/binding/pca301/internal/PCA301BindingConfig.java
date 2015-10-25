/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.binding.BindingConfig;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * This is a helper class holding binding specific configuration details
 * 
 * @author ribbeck
 * @since 1.7.2
 */
class PCA301BindingConfig implements BindingConfig {
	
	private static final String KEY_ADDRESS		= "address";
	private static final String KEY_PROPERTY	= "property";
	
	private static final String[] MANDATORY_KEYS = {KEY_ADDRESS, KEY_PROPERTY};
	
	
	/** Property of a PCA301 device. */
	public enum Property {
		UNKNOWN,
		CONSUMPTION,
		POWER,
		RESET,
		STATE;
		
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	};
	
	private int address;
	private Property property;
	
	
	private PCA301BindingConfig(int address, Property property) {
		this.address = address;
		this.property = property;
	}

	public int getAddress() {
		return address;
	}

	public String getPropertyName() {
		
		if (property == Property.UNKNOWN) {
			return null;
		}
		return property.toString();
	}
	
	/**
	 * Parses the specified binding configuration and creates an object from it.<br>
	 * The configuration format: key=value,key=value
	 * @param bindingConfig configuration in text format
	 * @return binding configuration as object
	 * @throws BindingConfigParseException configuration is not in a valid format
	 */
	public static PCA301BindingConfig parse(String bindingConfig) throws BindingConfigParseException {
		
		final Map<String, String> parameter = new HashMap<String, String>(4);
		
		final String[] params = bindingConfig.split(",");
		for (String param : params) {
			String[] fields = param.split("=");
			if (fields.length != 2) {
				throw new BindingConfigParseException("Wrong parameter format: " + param);
			}
			parameter.put(fields[0].trim(), fields[1].trim());
		}
		
		// check if mandatory parameter are given
		for (String key : MANDATORY_KEYS) {
			if (!parameter.containsKey(key)) {
				throw new BindingConfigParseException("Parameter " + key + " is missing");
			}
		}
		
		// get address
		int address = 0;
		try {
			address = Integer.decode(parameter.get(KEY_ADDRESS));
			
		} catch (NumberFormatException e) {
			throw new BindingConfigParseException("Invalid Address: " + e.getMessage());
		}
		
		// get property
		Property property = Property.UNKNOWN;
		try {
			property = Property.valueOf(parameter.get(KEY_PROPERTY).toUpperCase());
			
		} catch (IllegalArgumentException e) {
			throw new BindingConfigParseException("Invalid property: " + parameter.get(KEY_PROPERTY));
		}
		
		return new PCA301BindingConfig(address, property);
	}
	
}
