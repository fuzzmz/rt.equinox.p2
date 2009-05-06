/*******************************************************************************
 * Copyright (c) 2009, Cloudsmith Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.testserver;

import java.net.URI;
import javax.servlet.ServletException;
import org.eclipse.equinox.p2.testserver.servlets.BasicResourceDelivery;
import org.eclipse.equinox.p2.testserver.servlets.ChopAndDelay;
import org.eclipse.equinox.p2.testserver.servlets.ContentLengthLier;
import org.eclipse.equinox.p2.testserver.servlets.FileMolester;
import org.eclipse.equinox.p2.testserver.servlets.LastModifiedLier;
import org.eclipse.equinox.p2.testserver.servlets.StatusCodeResponse;
import org.eclipse.equinox.p2.testserver.servlets.TimeOut;
import org.eclipse.equinox.p2.testserver.servlets.Truncator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {
	private static BundleContext context;
	private ServiceTracker httpTracker;
	private SecureContext secureHttpContext;
	private static Activator instance;
	private HttpService httpService;
	private AlwaysFailContext alwaysFail;
	private FlipFlopFailContext flipFlop;

	private static final String SITE = "http://download.eclipse.org/eclipse/updates/3.4"; //$NON-NLS-1$

	public void start(BundleContext aContext) throws Exception {
		context = aContext;

		httpTracker = new ServiceTracker(context, HttpService.class.getName(), this);
		httpTracker.open();
		instance = this;
	}

	public void stop(BundleContext aContext) throws Exception {
		httpTracker.close();
		context = null;
	}

	public Object addingService(ServiceReference reference) {
		httpService = (HttpService) context.getService(reference);
		secureHttpContext = new SecureContext(httpService.createDefaultHttpContext());
		alwaysFail = new AlwaysFailContext(httpService.createDefaultHttpContext());
		flipFlop = new FlipFlopFailContext(httpService.createDefaultHttpContext());

		try {
			httpService.registerResources("/public", "/webfiles", null); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/private", "/webfiles", secureHttpContext); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/never", "/webfiles", alwaysFail); //$NON-NLS-1$ //$NON-NLS-2$
			httpService.registerResources("/flipflop", "/webfiles", flipFlop); //$NON-NLS-1$ //$NON-NLS-2$

			httpService.registerServlet("/status", new StatusCodeResponse(), null, null); //$NON-NLS-1$
			httpService.registerServlet("/timeout", new TimeOut(), null, null); //$NON-NLS-1$

			httpService.registerServlet("/truncated", new Truncator("/truncated", URI.create("/webfiles"), 50), null, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			httpService.registerServlet("/molested", new FileMolester("/molested", URI.create("/webfiles"), 40), null, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			// 8 bytes at a time, delay from 0 to 100 ms, in steps of 5
			httpService.registerServlet("/decelerate", new ChopAndDelay("/decelerate", URI.create("/webfiles"), 3, new LinearChange(0, 5, 100, 0)), null, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

			httpService.registerServlet("/proxy/truncated", new Truncator("/proxy/truncated", URI.create(SITE), 50), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/private", new BasicResourceDelivery("/proxy/private", URI.create(SITE)), null, secureHttpContext); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/never", new BasicResourceDelivery("/proxy/private", URI.create(SITE)), null, alwaysFail); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/flipFlop", new BasicResourceDelivery("/proxy/private", URI.create(SITE)), null, flipFlop); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/molested", new FileMolester("/proxy/molested", URI.create(SITE), 40), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/decelerate", new ChopAndDelay("/proxy/decelerate", URI.create(SITE), 3, new LinearChange(0, 5, 100, 0)), null, null); //$NON-NLS-1$//$NON-NLS-2$

			// lie about modified time
			httpService.registerServlet("/proxy/modified/zero", new LastModifiedLier("/proxy/modified/zero", URI.create(SITE), LastModifiedLier.TYPE_ZERO), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/modified/old", new LastModifiedLier("/proxy/modified/old", URI.create(SITE), LastModifiedLier.TYPE_OLD), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/modified/now", new LastModifiedLier("/proxy/modified/now", URI.create(SITE), LastModifiedLier.TYPE_NOW), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/modified/future", new LastModifiedLier("/proxy/modified/future", URI.create(SITE), LastModifiedLier.TYPE_FUTURE), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/modified/bad", new LastModifiedLier("/proxy/modified/bad", URI.create(SITE), LastModifiedLier.TYPE_BAD), null, null); //$NON-NLS-1$//$NON-NLS-2$

			// lie about length
			httpService.registerServlet("/proxy/length/zero", new ContentLengthLier("/proxy/length/zero", URI.create(SITE), 0), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/length/less", new ContentLengthLier("/proxy/length/less", URI.create(SITE), 90), null, null); //$NON-NLS-1$//$NON-NLS-2$
			httpService.registerServlet("/proxy/length/more", new ContentLengthLier("/proxy/length/more", URI.create(SITE), 200), null, null); //$NON-NLS-1$//$NON-NLS-2$

		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return httpService;
	}

	public static Activator getInstance() {
		return instance;
	}

	public HttpService getHttp() {
		return httpService;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// do nothing 
	}

	public void removedService(ServiceReference reference, Object service) {
		httpService = (HttpService) service;
		httpService.unregister("/public"); //$NON-NLS-1$
		httpService.unregister("/private"); //$NON-NLS-1$
	}

}