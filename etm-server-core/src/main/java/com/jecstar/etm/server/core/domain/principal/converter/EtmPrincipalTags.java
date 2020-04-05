/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.principal.converter;

public interface EtmPrincipalTags {

    String getIdTag();

    String getEmailTag();

    String getApiKeyTag();

    String getSecondaryApiKeyTag();

    String getFilterQueryTag();

    String getFilterQueryOccurrenceTag();

    String getAlwaysShowCorrelatedEventsTag();

    String getSearchHistorySizeTag();

    String getLocaleTag();

    String getNameTag();

    String getDisplayNameTag();

    String getPasswordHashTag();

    String getChangePasswordOnLogonTag();

    String getRolesTag();

    String getGroupsTag();

    String getTimeZoneTag();

    String getLdapBaseTag();

    String getDashboardsTag();

    String getGraphsTag();

    String getSearchHistoryTag();

    String getSearchTemplatesTag();

    String getSignalsTag();

    String getDashboardDatasourcesTag();

    String getSignalDatasourcesTag();

    String getNotifiersTag();
}
