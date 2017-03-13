/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.core.api.util;

import feign.Feign;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.mifos.core.api.annotation.ThrowsException;
import io.mifos.core.api.annotation.ThrowsExceptions;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
class AnnotatedErrorDecoder implements ErrorDecoder {

  private final Class feignClientClass;
  private final Logger logger;

  AnnotatedErrorDecoder(final Logger logger, final Class feignClientClass) {
    this.logger = logger;
    this.feignClientClass = feignClientClass;
  }

  @Override
  public Exception decode(
      final String methodKey,
      final Response response) {
    final Optional<Optional<Optional<Exception>>> ret =
        Arrays.stream(feignClientClass.getMethods())
            .filter(method -> Feign.configKey(feignClientClass, method).equals(methodKey))
            .map(method -> {
              final Optional<ThrowsException> annotation = getMatchingAnnotation(response, method);
              return annotation.map(a -> constructException(response, a));
            })
            .findAny();

    return unwrapEmbeddedOptional(ret, getAlternative(methodKey, response));
  }

  private RuntimeException getAlternative(final String methodKey, final Response response) {
    if (response.status() == HttpStatus.BAD_REQUEST.value()) {
      return new IllegalArgumentException(response.reason());
    } else if (response.status() == HttpStatus.FORBIDDEN.value()) {
      return new InvalidTokenException(response.reason());
    } else if (response.status() == HttpStatus.NOT_FOUND.value()) {
      return new NotFoundException();
    } else if (response.status() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      return new InternalServerError(response.reason());
    } else {
      return FeignException.errorStatus(methodKey, response);
    }
  }

  private Optional<ThrowsException> getMatchingAnnotation(
      final Response response,
      final Method method) {

    final ThrowsExceptions throwsExceptionsAnnotation =
        method.getAnnotation(ThrowsExceptions.class);
    if (throwsExceptionsAnnotation == null) {
      final ThrowsException throwsExceptionAnnotation =
          method.getAnnotation(ThrowsException.class);
      if ((throwsExceptionAnnotation != null) &&
          statusMatches(response, throwsExceptionAnnotation)) {
        return Optional.of(throwsExceptionAnnotation);
      }
    } else {
      return Arrays.stream(throwsExceptionsAnnotation.value())
          .filter(throwsExceptionAnnotation -> statusMatches(response,
              throwsExceptionAnnotation))
          .findAny();
    }

    return Optional.empty();
  }

  private boolean statusMatches(final Response response,
                                final ThrowsException throwsExceptionAnnotation) {
    return throwsExceptionAnnotation.status().value() == response.status();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private <T> T unwrapEmbeddedOptional(
      final Optional<Optional<Optional<T>>> embeddedOptional, T alternative) {
    return embeddedOptional.orElse(Optional.empty()).orElse(Optional.empty()).orElse(alternative);
  }

  private Optional<Exception> constructException(
      final Response response,
      final ThrowsException throwsExceptionAnnotations) {
    try {
      try {
        final Constructor<? extends RuntimeException> oneArgumentConstructor =
            throwsExceptionAnnotations.exception().getConstructor(Response.class);

        return Optional.of(oneArgumentConstructor.newInstance(response));
      } catch (final NoSuchMethodException e) {

        final Constructor<? extends RuntimeException> noArgumentConstructor =
            throwsExceptionAnnotations.exception().getConstructor();

        return Optional.of(noArgumentConstructor.newInstance());
      }
    } catch (final InvocationTargetException
        | IllegalAccessException
        | InstantiationException
        | NoSuchMethodException e) {
      logger.error("Instantiating exception {}, in for status {} failed with an exception",
          throwsExceptionAnnotations.exception(), throwsExceptionAnnotations.status(), e);

      return Optional.empty();
    }
  }
}
