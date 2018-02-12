package com.jecstar.etm.launcher.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Elasticsearch {

    public String clusterName = "elasticsearch";

    public List<String> connectAddresses = new ArrayList<>();

    public boolean waitForConnectionOnStartup = false;

    public String username;
    public String password;

    public boolean sslEnabled = false;
    public File sslKeyLocation;
    public File sslCertificateLocation;
    public File sslCertificateAuthoritiesLocation;

    public Elasticsearch() {
        this.connectAddresses.add("127.0.0.1:9300");
    }

}
