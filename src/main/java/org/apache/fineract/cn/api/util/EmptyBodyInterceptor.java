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

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.charset.Charset;

/**
 * Sets the content length of a request to zero if the request is of type POST or PUT, and contains
 * no request body.
 */
public class EmptyBodyInterceptor implements RequestInterceptor {

  public EmptyBodyInterceptor() {
    super();
  }

  @Override
  public void apply(final RequestTemplate template) {
    if ((template.method().equalsIgnoreCase(RequestMethod.POST.name())
        || template.method().equalsIgnoreCase(RequestMethod.PUT.name()))
        && template.body() == null) {
      template.body(new byte[0], Charset.defaultCharset());
    }
  }
}
