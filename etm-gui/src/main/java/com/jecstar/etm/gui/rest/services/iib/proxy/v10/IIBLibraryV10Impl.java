package com.jecstar.etm.gui.rest.services.iib.proxy.v10;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.SubFlowProxy;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

public class IIBLibraryV10Impl implements IIBLibrary {

	private LibraryProxy library;

	protected IIBLibraryV10Impl(LibraryProxy libraryProxy) {
		this.library = libraryProxy;
	}
	
	public String getName() {
		try {
			return this.library.getName();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBMessageFlow> getMessageFlows() {
		try {
			List<IIBMessageFlow> messageFlows = new ArrayList<>();
			Enumeration<MessageFlowProxy> messageFlowProxies = this.library.getMessageFlows(null);
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
			MessageFlowProxy messageFlowProxy = this.library.getMessageFlowByName(flowName);
			if (messageFlowProxy == null) {
				return null;
			}
			return new IIBMessageFlow(messageFlowProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBSubFlow> getSubFlows() {
		try {
			List<IIBSubFlow> subFlows = new ArrayList<>();
			Enumeration<SubFlowProxy> subFlowProxies = this.library.getSubFlows(null);
			while (subFlowProxies.hasMoreElements()) {
				subFlows.add(new IIBSubFlowV10Impl(subFlowProxies.nextElement()));
			}
			return subFlows;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}				
	}
	
	public IIBSubFlow getSubFlowByName(String subFlowName) {
		try {
			SubFlowProxy subFlowProxy = this.library.getSubFlowByName(subFlowName);
			if (subFlowProxy == null) {
				return null;
			}
			return new IIBSubFlowV10Impl(subFlowProxy);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public String getVersion() {
		try {
			return this.library.getVersion();
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
}