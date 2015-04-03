package com.jecstar.etm.gui.rest.repository;

import javax.annotation.ManagedBean;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Session;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.gui.rest.repository.cassandra.EndpointRepositoryCassandraImpl;
import com.jecstar.etm.gui.rest.repository.cassandra.QueryRepositoryCassandraImpl;
import com.jecstar.etm.gui.rest.repository.cassandra.StatisticsRepositoryCassandraImpl;
import com.jecstar.etm.jee.configurator.core.GuiConfiguration;

@ManagedBean
@Singleton
public class RepositoryProducer {

	@Inject
	@GuiConfiguration
	private Session session;
	
	@GuiConfiguration
	@Inject
	private EtmConfiguration configuration;
	
	private StatisticsRepository statisticsRepository;
	private QueryRepository queryRepository;
	private EndpointRepository endpointRepository;

	@Produces
	public StatisticsRepository getStatisticsRepository() {
		synchronized (this) {
	        if (this.statisticsRepository == null) {
	        	this.statisticsRepository = new StatisticsRepositoryCassandraImpl(this.session);
	        }
        }
		return this.statisticsRepository;
	}
	
	@Produces
	public QueryRepository getQueryRepository() {
		synchronized (this) {
	        if (this.queryRepository == null) {
	        	this.queryRepository = new QueryRepositoryCassandraImpl(this.session, this.configuration);
	        }
        }
		return this.queryRepository;
	}
	
	@Produces
	public EndpointRepository getEndpointRepository() {
		synchronized (this) {
	        if (this.endpointRepository == null) {
	        	this.endpointRepository = new EndpointRepositoryCassandraImpl(this.session);
	        }
        }
		return this.endpointRepository;
	}
	
}
