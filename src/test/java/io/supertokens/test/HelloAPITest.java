/*
 *    Copyright (c) 2023, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 */

package io.supertokens.test;

import com.google.gson.JsonObject;
import io.supertokens.ProcessState;
import io.supertokens.featureflag.EE_FEATURES;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.multitenancy.Multitenancy;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.multitenancy.*;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.test.httpRequest.HttpRequestForTesting;
import io.supertokens.test.httpRequest.HttpResponseException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.*;

public class HelloAPITest {
    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void testHelloAPIWithBasePath1() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        Utils.setValueInConfig("base_path", "/base");
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                "http://localhost:3567/base", null, 1000, 1000,
                null, Utils.getCdiVersionStringLatestForTests(), "");
        assertEquals("Hello", res);

        res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                "http://localhost:3567/base/hello", null, 1000, 1000,
                null, Utils.getCdiVersionStringLatestForTests(), "");
        assertEquals("Hello", res);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testHelloAPIWithBasePath2() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        Utils.setValueInConfig("base_path", "/hello");
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                "http://localhost:3567/hello", null, 1000, 1000,
                null, Utils.getCdiVersionStringLatestForTests(), "");
        assertEquals("Hello", res);

        res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                "http://localhost:3567/hello/hello", null, 1000, 1000,
                null, Utils.getCdiVersionStringLatestForTests(), "");
        assertEquals("Hello", res);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testHelloAPIWithBasePath3() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        Utils.setValueInConfig("base_path", "/hello");
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", null),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, null, "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        String[] HELLO_ROUTES = new String[]{
                "http://localhost:3567/hello", // baseUrl + /
                "http://localhost:3567/hello/", // baseUrl + /
                "http://localhost:3567/hello/hello", // baseUrl + /hello
                "http://localhost:3567/hello/hello/", // baseUrl + (hello tenant) + / : works because the hello api doesn't check if that tenant exists
                "http://localhost:3567/hello/appid-hello/hello", // baseUrl + app + /hello
                "http://localhost:3567/hello/appid-hello/hello/", // baseUrl + app + /hello
        };

        for (String helloUrl: HELLO_ROUTES) {
            System.out.println(helloUrl);
            String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    helloUrl, null, 1000, 1000,
                    null, Utils.getCdiVersionStringLatestForTests(), "");
            assertEquals("Hello", res);
            Thread.sleep(250); // Avoid rate limiting
        }

        String[] NOT_FOUND_ROUTES = new String[]{
                "http://localhost:3567/hello/appid-hello", // baseUrl + app + /
                "http://localhost:3567/hello/appid-hello/", // baseUrl + app + /
                "http://localhost:3567/hello/appid-hello/test", // baseUrl + app + tenant + /
        };

        // Not found
        for (String notFoundUrl : NOT_FOUND_ROUTES) {
            System.out.println(notFoundUrl);
            try {
                String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                        notFoundUrl, null, 1000, 1000,
                        null, Utils.getCdiVersionStringLatestForTests(), "");
                fail();
            } catch (HttpResponseException e) {
                assertEquals(404, e.statusCode);
            }
        }

        String[] BAD_PERMISSION_ROUTES = new String[]{
                "http://localhost:3567/hello/appid-hello/test/hello", // baseUrl + app + tenant + /hello
                "http://localhost:3567/hello/appid-hello/test/", // baseUrl + app + tenant + /
        };

        // Bad permission
        for (String badPermUrl : BAD_PERMISSION_ROUTES) {
            try {
                String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                        badPermUrl, null, 1000, 1000,
                        null, Utils.getCdiVersionStringLatestForTests(), "");
                fail();
            } catch (HttpResponseException e) {
                assertEquals(403, e.statusCode);
            }
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testWithBasePathThatHelloAPIDoesNotRequireAPIKeys() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        Utils.setValueInConfig("api_keys", "asdfasdfasdf123412341234");
        Utils.setValueInConfig("base_path", "/hello");

        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", null),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, null, "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        String[] HELLO_ROUTES = new String[]{
                "http://localhost:3567/hello", // baseUrl + /
                "http://localhost:3567/hello/", // baseUrl + /
                "http://localhost:3567/hello/hello", // baseUrl + /hello
                "http://localhost:3567/hello/hello/", // baseUrl + (hello tenant) + / : works because the hello api doesn't check if that tenant exists
                "http://localhost:3567/hello/appid-hello/hello", // baseUrl + app + /hello
                "http://localhost:3567/hello/appid-hello/hello/", // baseUrl + app + /hello
        };

        for (String helloUrl: HELLO_ROUTES) {
            System.out.println(helloUrl);
            String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    helloUrl, null, 1000, 1000,
                    null, Utils.getCdiVersionStringLatestForTests(), "");
            assertEquals("Hello", res);
            Thread.sleep(250); // avoid rate limiting
        }

        String[] NOT_FOUND_ROUTES = new String[]{
                "http://localhost:3567/abcd",
                "http://localhost:3567",
                "http://localhost:3567/hello/appid-hello", // baseUrl + app + /
                "http://localhost:3567/hello/appid-hello/", // baseUrl + app + /
                "http://localhost:3567/hello/appid-hello/test", // baseUrl + app + tenant + /
        };

        // Not found
        for (String notFoundUrl : NOT_FOUND_ROUTES) {
            try {
                String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                        notFoundUrl, null, 1000, 1000,
                        null, Utils.getCdiVersionStringLatestForTests(), "");
                fail();
            } catch (HttpResponseException e) {
                assertEquals(404, e.statusCode);
            }
        }

        String[] BAD_PERMISSION_ROUTES = new String[]{
                "http://localhost:3567/hello/appid-hello/test/hello", // baseUrl + app + tenant + /hello
                "http://localhost:3567/hello/appid-hello/test/", // baseUrl + app + tenant + /
        };

        // Bad permission
        for (String badPermUrl : BAD_PERMISSION_ROUTES) {
            try {
                String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                        badPermUrl, null, 1000, 1000,
                        null, Utils.getCdiVersionStringLatestForTests(), "");
                fail();
            } catch (HttpResponseException e) {
                assertEquals(403, e.statusCode);
            }
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testThatHelloAPIDoesNotRequireAPIKeys() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        Utils.setValueInConfig("api_keys", "asdfasdfasdf123412341234");

        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", null),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, "hello", "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        Multitenancy.addNewOrUpdateAppOrTenant(process.getProcess(), new TenantConfig(
                new TenantIdentifier(null, null, "test"),
                new EmailPasswordConfig(true),
                new ThirdPartyConfig(true, null),
                new PasswordlessConfig(true),
                new JsonObject()
        ), false);

        String[] HELLO_ROUTES = new String[]{
                "http://localhost:3567", // /
                "http://localhost:3567/", // /
                "http://localhost:3567/hello", // /hello
                "http://localhost:3567/hello/", // (hello tenant) + / : works because the hello api doesn't check if that tenant exists
                "http://localhost:3567/appid-hello/hello", // app + /hello
                "http://localhost:3567/appid-hello/hello/", // app + /hello
        };

        for (String helloUrl: HELLO_ROUTES) {
            System.out.println(helloUrl);
            String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                    helloUrl, null, 1000, 1000,
                    null, Utils.getCdiVersionStringLatestForTests(), "");
            assertEquals("Hello", res);
            Thread.sleep(250); // avoid rate limiting
        }

        String[] NOT_FOUND_ROUTES = new String[]{
                "http://localhost:3567/abcd",
                "http://localhost:3567/appid-hello", // app + /
                "http://localhost:3567/appid-hello/", // app + /
                "http://localhost:3567/appid-hello/test", // app + tenant + /
        };

        // Not found
        for (String notFoundUrl : NOT_FOUND_ROUTES) {
            try {
                String res = HttpRequestForTesting.sendGETRequest(process.getProcess(), "",
                        notFoundUrl, null, 1000, 1000,
                        null, Utils.getCdiVersionStringLatestForTests(), "");
                fail();
            } catch (HttpResponseException e) {
                assertEquals(404, e.statusCode);
            }
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }
}
