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

import feign.RequestTemplate;
import org.apache.fineract.cn.api.context.AutoUserContext;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Myrle Krantz
 */
public class TokenedTargetInterceptorTest {

  @Test
  public void test() {
    final TokenedTargetInterceptor testSubject = new TokenedTargetInterceptor();

    final RequestTemplate requestTemplate = new RequestTemplate();

    try (final AutoUserContext ignored = new AutoUserContext("x", "y")) {
      testSubject.apply(requestTemplate);
    }

    Assert.assertTrue(requestTemplate.headers().get(ApiConstants.USER_HEADER).contains("x"));
    Assert.assertTrue(requestTemplate.headers().get(ApiConstants.AUTHORIZATION_HEADER).contains("y"));
  }
}