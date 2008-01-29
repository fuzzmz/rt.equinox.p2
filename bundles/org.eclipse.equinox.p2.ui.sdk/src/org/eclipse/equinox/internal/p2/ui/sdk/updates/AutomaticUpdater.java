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
package org.eclipse.equinox.internal.p2.ui.sdk.updates;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.ProfileElement;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.query.ElementQueryDescriptor;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	Preferences prefs;

	public AutomaticUpdater() {
		prefs = ProvSDKUIActivator.getDefault().getPluginPreferences();
	}

	public void updatesAvailable(final UpdateEvent event) {
		final boolean download = prefs.getBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY);
		final IInstallableUnit[] toUpdate = getUpdatesToShow(event);
		if (toUpdate.length <= 0)
			return;
		try {
			if (download) {
				UpdateEvent eventWithOnlyRoots = new UpdateEvent(event.getProfileId(), toUpdate);
				ElementQueryDescriptor descriptor = ProvSDKUIActivator.getDefault().getQueryProvider().getQueryDescriptor(eventWithOnlyRoots, IQueryProvider.AVAILABLE_UPDATES);
				IInstallableUnit[] replacements = (IInstallableUnit[]) descriptor.queryable.query(descriptor.query, descriptor.collector, null).toArray(IInstallableUnit.class);
				if (replacements.length > 0) {
					ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(event.getProfileId());
					request.removeInstallableUnits(toUpdate);
					request.addInstallableUnits(replacements);
					final ProvisioningPlan plan = ProvisioningUtil.getPlanner().getProvisioningPlan(request, new ProvisioningContext(), null);
					Job job = ProvisioningOperationRunner.schedule(new ProfileModificationOperation(ProvSDKMessages.AutomaticUpdater_AutomaticDownloadOperationName, event.getProfileId(), plan, new DownloadPhaseSet(), false), null);
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent jobEvent) {
							IStatus status = jobEvent.getResult();
							if (status.isOK()) {
								PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
									public void run() {
										new AutomaticUpdatesPopup(toUpdate, event.getProfileId(), true, prefs).open();
									}
								});
							} else if (status.getSeverity() != IStatus.CANCEL) {
								ProvUI.reportStatus(status);
							}
						}
					});
				}
			} else {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						new AutomaticUpdatesPopup(toUpdate, event.getProfileId(), false, prefs).open();
					}
				});
			}

		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
		}

	}

	private IInstallableUnit[] getUpdatesToShow(final UpdateEvent event) {
		// We could simply collect the install roots ourselves, but implementing
		// this in terms of a normal "what's installed" query allows the policy to be defined only
		// in one place.
		IQueryable rootQueryable = new IQueryable() {
			public Collector query(Query query, Collector result, IProgressMonitor monitor) {
				IInstallableUnit[] ius = event.getIUs();
				for (int i = 0; i < ius.length; i++)
					if (query.isMatch(ius[i]))
						result.accept(ius[i]);
				return result;
			}
		};
		ProfileElement element = new ProfileElement(event.getProfileId());
		ElementQueryDescriptor descriptor = ProvSDKUIActivator.getDefault().getQueryProvider().getQueryDescriptor(element, IQueryProvider.INSTALLED_IUS);
		Object[] elements = rootQueryable.query(descriptor.query, descriptor.collector, null).toArray(Object.class);
		IInstallableUnit[] result = new IInstallableUnit[elements.length];
		for (int i = 0; i < result.length; i++)
			result[i] = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		return result;
	}
}
