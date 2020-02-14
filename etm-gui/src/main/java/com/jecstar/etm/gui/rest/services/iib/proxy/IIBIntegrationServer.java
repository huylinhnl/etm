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

package com.jecstar.etm.gui.rest.services.iib.proxy;

import java.util.List;

public interface IIBIntegrationServer {

    String getName();

    List<IIBApplication> getApplications();

    IIBApplication getApplicationByName(String applicationName);

    List<IIBLibrary> getSharedLibraries();

    IIBLibrary getSharedLibraryByName(String libraryName);

    List<IIBMessageFlow> getMessageFlows();

    IIBMessageFlow getMessageFlowByName(String flowName);

}
