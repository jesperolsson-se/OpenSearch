/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.rest.action.search;

import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.rest.RestRequest;
import org.opensearch.test.rest.FakeRestRequest;
import org.opensearch.test.rest.RestActionTestCase;
import org.junit.Before;

import java.nio.charset.StandardCharsets;

public class RestMultiSearchActionTests extends RestActionTestCase {

    @Before
    public void setUpAction() {
        controller().registerHandler(new RestMultiSearchAction(Settings.EMPTY));
    }

    public void testTypeInPath() {
        String content = "{ \"index\": \"some_index\" } \n {} \n";
        BytesArray bytesContent = new BytesArray(content.getBytes(StandardCharsets.UTF_8));

        RestRequest request = new FakeRestRequest.Builder(xContentRegistry())
            .withMethod(RestRequest.Method.GET)
            .withPath("/some_index/some_type/_msearch")
            .withContent(bytesContent, XContentType.JSON)
            .build();

        // We're not actually testing anything to do with the client, but need to set this so it doesn't fail the test for being unset.
        verifyingClient.setExecuteLocallyVerifier((arg1, arg2) -> null);

        dispatchRequest(request);
        assertWarnings(RestMultiSearchAction.TYPES_DEPRECATION_MESSAGE);
    }

    public void testTypeInBody() {
        String content = "{ \"index\": \"some_index\", \"type\": \"some_type\" } \n {} \n";
        BytesArray bytesContent = new BytesArray(content.getBytes(StandardCharsets.UTF_8));

        RestRequest request = new FakeRestRequest.Builder(xContentRegistry())
            .withMethod(RestRequest.Method.POST)
            .withPath("/some_index/_msearch")
            .withContent(bytesContent, XContentType.JSON)
            .build();

        // We're not actually testing anything to do with the client, but need to set this so it doesn't fail the test for being unset.
        verifyingClient.setExecuteLocallyVerifier((arg1, arg2) -> null);

        dispatchRequest(request);
        assertWarnings(RestMultiSearchAction.TYPES_DEPRECATION_MESSAGE);
    }
}
