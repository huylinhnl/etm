package com.jecstar.etm.slf4j;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DefaultConfiguration implements Configuration {

	private final List<String> endpointUrls = new ArrayList<>();
	private final TreeMap<String, String> loggers = new TreeMap<>();
	
	public DefaultConfiguration() {
	}
	
	@Override
	public List<String> getEndpointUrls() {
		return this.endpointUrls;
	}

	@Override
	public long getPushInterval() {
		return 5000;
	}

	@Override
	public int getMaxRequestsPerBatch() {
		return 1000;
	}

	@Override
	public int getNumberOfWorkers() {
		return 1;
	}

	@Override
	public String getRootLogLevel() {
		return "INFO";
	}

	@Override
	public TreeMap<String, String> getLoggers() {
		return this.loggers;
	}

	@Override
	public String getApplicationName() {
		return null;
	}

	@Override
	public String getApplicationVersion() {
		return null;
	}

	@Override
	public String getApplicationInstance() {
		return null;
	}

	@Override
	public String getPrincipalName() {
		return null;
	}

	@Override
	public InetAddress getHostAddress() {
		return null;
	}

}
