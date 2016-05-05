package com.jecstar.etm.launcher.configuration;

public class Configuration {

	public String clusterName = "Enterprise Telemetry Monitor";
	public String instanceName = "Node_1";
	
	public String bindingAddress = "127.0.0.1";
	public int bindingPortOffset = 0;
	
	public Elasticsearch elasticsearch = new Elasticsearch();
	public Http http = new Http();
	public Logging logging = new Logging();

	public boolean guiEnabled = true;
	public boolean restProcessorEnabled = true;
	
	public boolean isHttpServerNecessary() {
		return this.restProcessorEnabled || this.guiEnabled;
	}
	
	public int getHttpPort() {
		if (this.http.httpPort > 0) {
			return this.http.httpPort + this.bindingPortOffset; 
		}
		return this.http.httpPort;
	}
	
	public int getHttpsPort() {
		if (this.http.httpsPort > 0) {
			return this.http.httpsPort + this.bindingPortOffset; 
		}
		return this.http.httpsPort;
	}
}
