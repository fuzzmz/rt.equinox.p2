/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.spi.p2.metadata.repository.IMetadataRepositoryFactory;

public class ExtensionLocationMetadataRepositoryFactory implements IMetadataRepositoryFactory {

	public IMetadataRepository create(URL location, String name, String type) throws ProvisionException {
		return null;
	}

	public IMetadataRepository load(URL location, IProgressMonitor monitor) throws ProvisionException {
		return new ExtensionLocationMetadataRepository(location, monitor);
	}

	public IStatus validate(URL location, IProgressMonitor monitor) {
		try {
			ExtensionLocationMetadataRepository.validate(location, monitor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
