/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.util.Date;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.service.debug.DebugOptions;

/**
 * Manages debug tracing options and provides convenience methods for printing
 * debug statements.
 */
public class Tracing {
	//master p2 debug flag
	public static boolean DEBUG = false;

	//debug constants
	public static boolean DEBUG_PARSING = false;

	static {
		DebugOptions options = (DebugOptions) ServiceHelper.getService(Activator.context, DebugOptions.class.getName());
		if (options != null) {
			DEBUG = options.getBooleanOption(Activator.ID + "/debug", false); //$NON-NLS-1$
			if (DEBUG) {
				DEBUG_PARSING = options.getBooleanOption(Activator.ID + "/persistence/parsing", false); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Prints a debug message on stdout. Callers should first ensure their specific 
	 * debug option is enabled.
	 */
	public static void debug(String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[p2] "); //$NON-NLS-1$
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}
}
