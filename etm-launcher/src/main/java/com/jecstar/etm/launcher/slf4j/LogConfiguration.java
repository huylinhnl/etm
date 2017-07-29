package com.jecstar.etm.launcher.slf4j;

import java.net.InetAddress;
import java.util.TreeMap;

public class LogConfiguration {

	public static String rootLogLevel = "INFO";
	public static final TreeMap<String, String> loggers = new TreeMap<>();
	private static final String applicationName = "Enterprise Telemetry Monitor";
	private static final String applicationVersion = System.getProperty("app.version");
	public static String applicationInstance = null;
	private static final String principalName = System.getProperty("user.name");
	public static InetAddress hostAddress = null;
	
	private String getRootLogLevel() {
		return rootLogLevel;
	}

	private TreeMap<String, String> getLoggers() {
		return loggers;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public String getApplicationInstance() {
		return applicationInstance;
	}

	public String getPrincipalName() {
		return principalName;
	}

	public InetAddress getHostAddress() {
		return hostAddress;
	}
	
	String getLogLevel(String loggerName) {
		if (loggerName == null) {
			return getRootLogLevel();
		}
		String specificLevel = getLoggers().get(loggerName);
		if (specificLevel != null) {
			return specificLevel;
		}
		int ix = loggerName.lastIndexOf("$");
		if (ix != -1) {
			return getLogLevel(loggerName.substring(0, ix));
		}
		ix = loggerName.lastIndexOf(".");
		if (ix != -1) {
			return getLogLevel(loggerName.substring(0, ix));
		}
		return getRootLogLevel();
	}
	
	// TODO create a method to update the loglevel of a logger on the fly.
}
