package com.jecstar.etm.server.core.domain.principal.converter.json;

import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;

public class EtmPrincipalTagsJsonImpl implements EtmPrincipalTags {
	
	@Override
	public String getIdTag() {
		return "id";
	}

	@Override
	public String getEmailTag() {
		return "email";
	}
	
	@Override
	public String getFilterQueryTag() {
		return "filter_query";
	}
	
	@Override
	public String getFilterQueryOccurrenceTag() {
		return "filter_query_occurrence";
	}
	
	@Override
	public String getAlwaysShowCorrelatedEventsTag() {
		return "always_show_correlated_events";
	}
	
	@Override
	public String getSearchHistorySizeTag() {
		return "search_history_size";
	}
	
	@Override
	public String getLocaleTag() {
		return "locale";
	}
	
	@Override
	public String getNameTag() {
		return "name";
	}

	@Override
	public String getPasswordHashTag() {
		return "password_hash";
	}
	
	@Override
	public String getChangePasswordOnLogonTag() {
		return "change_password_on_logon";
	}
	
	@Override
	public String getRolesTag() {
		return "roles";
	}
	
	@Override
	public String getGroupsTag() {
		return "groups";
	}

	@Override
	public String getTimeZoneTag() {
		return "time_zone";
	}

	@Override
	public String getSearchHistoryTag() {
		return "search_history";
	}

	@Override
	public String getSearchTemplatesTag() {
		return "search_templates";
	}

	@Override
	public String getLdapBaseTag() {
		return "ldap_base";
	}

}