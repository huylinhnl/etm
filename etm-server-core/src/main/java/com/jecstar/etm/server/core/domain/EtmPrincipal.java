package com.jecstar.etm.server.core.domain;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class EtmPrincipal implements Principal {
	
	public static final int DEFAULT_HISTORY_SIZE = 5;
	
	public enum PrincipalRole { 
		ADMIN("admin"), SEARCHER("searcher"), CONTROLLER("controller"), PROCESSOR("processor");
		
		private final String roleName;
		
		private PrincipalRole(String roleName) {
			this.roleName = roleName;
		}
		
		public String getRoleName() {
			return this.roleName;
		}
	}
	
	private final String id;
	private String passwordHash;
	
	private String name = null;
	private Locale locale = Locale.getDefault();
	private Set<PrincipalRole> roles = new HashSet<PrincipalRole>();
	private TimeZone timeZone = TimeZone.getDefault();
	private String filterQuery = null;
	private int historySize = DEFAULT_HISTORY_SIZE;

	
	// State properties. DO NOT CHANGE!!
	public boolean forceReload = false;
	
	public EtmPrincipal(String id) {
		this.id = id;
	}
	
	public EtmPrincipal(String id, String passwordHash) {
		this(id);
		setPasswordHash(passwordHash);
	}

	@Override
	public String getName() {
		return this.name == null ? this.id : this.name;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public void setLocale(Locale locale) {
		if (locale == null) {
			return;
		}
		this.locale = locale;
	}
	
	public TimeZone getTimeZone() {
		return this.timeZone;
	}
	
	public void setTimeZone(TimeZone timeZone) {
		if (timeZone == null) {
			return;
		}
		this.timeZone = timeZone;
	}
	
	public Set<PrincipalRole> getRoles() {
		return Collections.unmodifiableSet(this.roles);
	}
	
	public void addRole(PrincipalRole role) {
		if (role != null && !this.roles.contains(role)) {
			this.roles.add(role);
		}
	}
	
	public void addRoles(Collection<PrincipalRole> roles) {
		if (roles == null) {
			return;
		}
		for (PrincipalRole role : roles) {
			addRole(role);
		}
	}
	
	public String getFilterQuery() {
		return this.filterQuery;
	}
	
	public void setFilterQuery(String filterQuery) {
		this.filterQuery = filterQuery;
	}
	
	public int getHistorySize() {
		return this.historySize;
	}
	
	public void setHistorySize(int historySize) {
		if (historySize < 0) {
			this.historySize =  0;
		}
		this.historySize = historySize;
	}

}
