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

import feign.Feign;
import feign.FeignException;
import feign.Response;
import feign.Request;
import org.apache.fineract.cn.api.annotation.ThrowsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Myrle Krantz
 */
@RunWith(Parameterized.class)
public class AnnotatedErrorDecoderTest {

  private final TestCase testCase;

  public AnnotatedErrorDecoderTest(final TestCase testCase) {
    this.testCase = testCase;
  }

  @Parameterized.Parameters
  public static Collection testCases() throws NoSuchMethodException {
    final Collection<TestCase> ret = new ArrayList<>();

    final String TEST_URL = "http://igle.pop.org/app/v1/";
    final Request request = Request.create("GET", TEST_URL, Collections.emptyMap(), new byte[]{}, Charset.defaultCharset());
  
    final Response emptyInternalServerErrorResponse = Response.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyBadRequestResponse = Response.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyBadRequestResponseWithNoBody = Response.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyNotFoundRequestResponse = Response.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyConflictResponse = Response.builder()
            .status(HttpStatus.CONFLICT.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyIAmATeapotResponse = Response.builder()
            .status(HttpStatus.I_AM_A_TEAPOT.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyUnauthorizedResponse = Response.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final Response emptyForbiddenResponse = Response.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .body("blah", Charset.defaultCharset())
            .headers(Collections.emptyMap())
            .request(request)
            .build();

    final String madeUpMethodKey = "x";

    final String annotationlessMethodKey =
        Feign.configKey(AnnotationlessInterface.class, AnnotationlessInterface.class.getMethod("method"));

    final String oneAnnotatedMethodKey =
        Feign.configKey(OneMethodInterface.class, OneMethodInterface.class.getMethod("method"));

    final String onceAnnotatedMethodKey =
        Feign.configKey(OneMethodOneAnnotationInterface.class, OneMethodOneAnnotationInterface.class.getMethod("method"));

    final String onceAnnotatedWithStringExceptionMethodKey =
        Feign.configKey(OneMethodOneAnnotationStringParameteredExceptionInterface.class, OneMethodOneAnnotationStringParameteredExceptionInterface.class.getMethod("method"));

    ret.add(new TestCase("Methodless interface")
        .clazz(MethodlessInterface.class)
        .methodKey(madeUpMethodKey)
        .response(emptyConflictResponse)
        .expectedResult(FeignException.errorStatus(madeUpMethodKey, emptyConflictResponse)));

    ret.add(new TestCase("Annotationless interface")
        .clazz(AnnotationlessInterface.class)
        .methodKey(annotationlessMethodKey)
        .response(emptyConflictResponse)
        .expectedResult(FeignException.errorStatus(annotationlessMethodKey, emptyConflictResponse)));

    ret.add(new TestCase("Interface with one method mapped to parameterless exception")
        .clazz(OneMethodInterface.class)
        .methodKey(oneAnnotatedMethodKey)
        .response(emptyBadRequestResponse)
        .expectedResult(new ParameterlessException()));

    ret.add(new TestCase("Interface with one method mapped to parametered exception")
        .clazz(OneMethodInterface.class)
        .methodKey(oneAnnotatedMethodKey)
        .response(emptyConflictResponse)
        .expectedResult(new ParameteredException(emptyConflictResponse)));

    ret.add(new TestCase("Interface with one method mapped to an exception which can't be constructed by reflection")
        .clazz(OneMethodInterface.class)
        .methodKey(oneAnnotatedMethodKey)
        .response(emptyIAmATeapotResponse)
        .expectedResult(FeignException.errorStatus(oneAnnotatedMethodKey, emptyIAmATeapotResponse)));

    ret.add(new TestCase("Interface with one method, not mapped to the response code returned")
        .clazz(OneMethodInterface.class)
        .methodKey(oneAnnotatedMethodKey)
        .response(emptyUnauthorizedResponse)
        .expectedResult(FeignException.errorStatus(oneAnnotatedMethodKey, emptyUnauthorizedResponse)));

    ret.add(new TestCase("Interface with one method that has one annotation")
        .clazz(OneMethodOneAnnotationInterface.class)
        .methodKey(onceAnnotatedMethodKey)
        .response(emptyBadRequestResponse)
        .expectedResult(new ParameterlessException()));

    ret.add(new TestCase("Interface with one method that has one annotation containing an exception which accepts a string parameter.")
        .clazz(OneMethodOneAnnotationStringParameteredExceptionInterface.class)
        .methodKey(onceAnnotatedWithStringExceptionMethodKey)
        .response(emptyBadRequestResponse)
        .expectedResult(new StringParameteredException("blah")));

    ret.add(new TestCase("Bad request on an interface in which bad request isn't mapped.")
        .clazz(AnnotationlessInterface.class)
        .methodKey(annotationlessMethodKey)
        .response(emptyBadRequestResponse)
        .expectedResult(new IllegalArgumentException("blah")));

    ret.add(new TestCase("Bad request with no body on an interface in which bad request isn't mapped.")
        .clazz(AnnotationlessInterface.class)
        .methodKey(annotationlessMethodKey)
        .response(emptyBadRequestResponseWithNoBody)
        .expectedResult(new IllegalArgumentException((String)null)));


    ret.add(new TestCase("Not found request on an interface in which not found request isn't mapped.")
        .clazz(AnnotationlessInterface.class)
        .methodKey(annotationlessMethodKey)
        .response(emptyNotFoundRequestResponse)
        .expectedResult(new NotFoundException("blah")));

    ret.add(new TestCase("Request with invalid token.")
        .clazz(OneMethodOneAnnotationInterface.class)
        .methodKey(onceAnnotatedMethodKey)
        .response(emptyForbiddenResponse)
        .expectedResult(new InvalidTokenException("blah")));

    ret.add(new TestCase("Internal Server Error on an interface in which internal server error isn't mapped.")
        .clazz(AnnotationlessInterface.class)
        .methodKey(annotationlessMethodKey)
        .response(emptyInternalServerErrorResponse)
        .expectedResult(new InternalServerError("blah")));

    return ret;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Test
  public void checkMapping() {

    final AnnotatedErrorDecoder testSubject = new AnnotatedErrorDecoder(
        LoggerFactory.getLogger(AnnotatedErrorDecoderTest.class.getName()), testCase.getClazz());

    final Exception result = testSubject.decode(testCase.getMethodKey(), testCase.getResponse());

    Assert.assertEquals("Test case \"" + testCase.getName() + "\" failed.",
        testCase.expectedResult().getClass(), result.getClass());

    Assert.assertEquals("Test case \"" + testCase.getName() + "\" failed.",
        testCase.expectedResult().getMessage(), result.getMessage());
  }

  private interface MethodlessInterface {

  }

  private interface AnnotationlessInterface {

    @SuppressWarnings("unused")
    void method();
  }

  private interface OneMethodInterface {

    @SuppressWarnings("unused")
    @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = ParameterlessException.class)
    @ThrowsException(status = HttpStatus.CONFLICT, exception = ParameteredException.class)
    @ThrowsException(status = HttpStatus.I_AM_A_TEAPOT, exception = WrongParameteredException.class)
    void method();
  }


  private interface OneMethodOneAnnotationInterface {

    @SuppressWarnings("unused")
    @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = ParameterlessException.class)
    void method();
  }


  private interface OneMethodOneAnnotationStringParameteredExceptionInterface {

    @SuppressWarnings("unused")
    @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = StringParameteredException.class)
    void method();
  }

  private static class TestCase {

    private final String name;
    private Class clazz;
    private String methodKey;
    private Response response;
    private Exception expectedResult;

    private TestCase(final String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }

    TestCase clazz(final Class newVal) {
      clazz = newVal;
      return this;
    }

    TestCase methodKey(final String newVal) {
      methodKey = newVal;
      return this;
    }

    TestCase response(final Response newVal) {
      response = newVal;
      return this;
    }

    TestCase expectedResult(final Exception newVal) {
      expectedResult = newVal;
      return this;
    }

    Class getClazz() {
      return clazz;
    }

    String getMethodKey() {
      return methodKey;
    }

    Response getResponse() {
      return response;
    }

    Exception expectedResult() {
      return expectedResult;
    }

    String getName() {
      return name;
    }
  }

  private static class ParameterlessException extends RuntimeException {

    @SuppressWarnings("WeakerAccess")
    public ParameterlessException() {
      super("I am a parameterless exception.  Aren't I cool.");
    }
  }

  private static class ParameteredException extends RuntimeException {

    @SuppressWarnings("WeakerAccess")
    public ParameteredException(final Response response) {
      super("I am a parametered exception with a response of " + response.toString());
    }
  }

  private static class StringParameteredException extends RuntimeException {

    @SuppressWarnings("WeakerAccess")
    public StringParameteredException(final String response) {
      super(response);
    }
  }

  private static class WrongParameteredException extends RuntimeException {

    public WrongParameteredException(final Integer message) {
      super(message.toString());
    }
  }
}
