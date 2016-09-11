package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ApplicationProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.server.core.EtmException;

public class IIBIntegrationServer {

	private ExecutionGroupProxy integrationServer;

	protected IIBIntegrationServer(ExecutionGroupProxy executionGroupProxy) {
		this.integrationServer = executionGroupProxy;
	}

	public String getName() {
		try {
			return this.integrationServer.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBApplication> getApplications() {
		try {
			List<IIBApplication> applications = new ArrayList<>();
			Enumeration<ApplicationProxy> applicationProxies = this.integrationServer.getApplications(null);
			while (applicationProxies.hasMoreElements()) {
				applications.add(new IIBApplication(applicationProxies.nextElement()));
			}
			return applications;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public IIBApplication getApplicationByName(String applicationName) {
		try {
			return new IIBApplication(this.integrationServer.getApplicationByName(applicationName));
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public List<IIBLibrary> getLibraries() {
		try {
			List<IIBLibrary> libraries = new ArrayList<>();
			Enumeration<LibraryProxy> libraryProxies = this.integrationServer.getLibraries(null);
			while (libraryProxies.hasMoreElements()) {
				libraries.add(new IIBLibrary(libraryProxies.nextElement()));
			}
			return libraries;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}
	
	public IIBLibrary getLibraryByName(String libraryName) {
		try {
			return new IIBLibrary(this.integrationServer.getLibraryByName(libraryName));
		} catch (ConfigManagerProxyPropertyNotInitializedException e) { 
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}	
	}
	
	public List<IIBMessageFlow> getMessageFlows() {
		try {
			List<IIBMessageFlow> messageFlows = new ArrayList<>();
			Enumeration<MessageFlowProxy> messageFlowProxies = this.integrationServer.getMessageFlows(null);
			while (messageFlowProxies.hasMoreElements()) {
				messageFlows.add(new IIBMessageFlow(messageFlowProxies.nextElement()));
			}
			return messageFlows;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}		
	}

	public IIBMessageFlow getMessageFlowByName(String flowName) {
		try {
			return new IIBMessageFlow(this.integrationServer.getMessageFlowByName(flowName));
		} catch (ConfigManagerProxyPropertyNotInitializedException e) { 
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}	
	}

}
