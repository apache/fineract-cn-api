/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.api.util;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Myrle Krantz
 */
public class CookieInterceptingClientTest {
  private final static String TEST_URL = "http://igle.pop.org/app/v1/";
  private final Request request = Request.create("GET", TEST_URL, Collections.emptyMap(), new byte[]{}, Charset.defaultCharset());

  @Test
  public void cookiesPlacedInJarThenAttachedToRequest() throws IOException, URISyntaxException {
    final CookieInterceptingClient testSubject = new CookieInterceptingClient(TEST_URL);

    final CookieInterceptingClient spiedTestSubject = Mockito.spy(testSubject);

    final Map<String, Collection<String>> cookieHeaders = new HashMap<>();
    cookieHeaders.put("Set-Cookie", Collections.singleton("x=y;Path=/app/v1"));
    final Response dummyResponse =  Response.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .reason("blah")
            .headers(cookieHeaders)
            .request(this.request)
            .build();

    Mockito.doReturn(dummyResponse).when(spiedTestSubject).superExecute(Mockito.anyObject(), Mockito.anyObject());

    spiedTestSubject.execute(this.request, new Request.Options());

    final Map<String, List<String>> ret = testSubject.cookieManager.get(new URI(TEST_URL), Collections.emptyMap());
    Assert.assertEquals(ret.get("Cookie"), Collections.singletonList("x=y"));

    //request
    final RequestTemplate dummyRequestTemplate = new RequestTemplate();
    dummyRequestTemplate.append("/request");
    testSubject.getCookieInterceptor().apply(dummyRequestTemplate);
    Assert.assertEquals(dummyRequestTemplate.headers().get("Cookie"), Collections.singletonList("x=y"));
  }

  @Test(expected = IllegalStateException.class)
  public void unexpectedCookieManagerFailure() throws IOException {
    final CookieManager cookieManagerMock = Mockito.mock(CookieManager.class);
    //noinspection unchecked
    Mockito.when(cookieManagerMock.get(Mockito.anyObject(), Mockito.anyObject())).thenThrow(IOException.class);

    final CookieInterceptingClient testSubject = new CookieInterceptingClient(TEST_URL, cookieManagerMock);

    final RequestTemplate dummyRequestTemplate = new RequestTemplate();
    dummyRequestTemplate.append("/request");

    testSubject.getCookieInterceptor().apply(dummyRequestTemplate);
  }

  @Test()
  public void setCookieBetweenRemoteCalls() {
    final CookieInterceptingClient testSubject = new CookieInterceptingClient(TEST_URL);
    testSubject.putCookie("/blah", "token", "Bearerbear");
    //request
    final RequestTemplate dummyRequestTemplate = new RequestTemplate();
    dummyRequestTemplate.append("/request");
    testSubject.getCookieInterceptor().apply(dummyRequestTemplate);
    Assert.assertEquals(Collections.singletonList("token=Bearerbear"), dummyRequestTemplate.headers().get("Cookie"));
  }
}