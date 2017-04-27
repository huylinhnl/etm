package com.jecstar.etm.server.core.domain.audit.builders;

import com.jecstar.etm.server.core.domain.audit.QueryAuditLog;

public class QueryAuditLogBuilder extends AbstractAuditLogBuilder<QueryAuditLog, QueryAuditLogBuilder>{

	public QueryAuditLogBuilder() {
		super(new QueryAuditLog());
	}

	public QueryAuditLogBuilder setUserQuery(String userQuery) {
		this.audit.userQuery = userQuery;
		return this;
	}

	public QueryAuditLogBuilder setExectuedQuery(String executedQuery) {
		this.audit.executedQuery = executedQuery;
		return this;
	}
	
	public QueryAuditLogBuilder setNumberOfResults(long numberOfResults) {
		this.audit.numberOfResults = numberOfResults;
		return this;
	}

}