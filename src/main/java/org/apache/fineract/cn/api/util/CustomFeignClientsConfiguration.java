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

import com.netflix.hystrix.strategy.HystrixPlugins;
import feign.Feign;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import org.apache.fineract.cn.api.config.ApiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Myrle Krantz
 */
//@EnableApiFactory (for logger)
@SuppressWarnings({"unused"})
@ConditionalOnProperty(value = "hystrix.wrappers.enabled", matchIfMissing = true)
public class CustomFeignClientsConfiguration extends FeignClientsConfiguration {

    public static final Logger logger = LoggerFactory.getLogger(CustomFeignClientsConfiguration.class.getName());

    private static class AnnotatedErrorDecoderFeignBuilder extends Feign.Builder {
        private final Logger logger;

        AnnotatedErrorDecoderFeignBuilder(final Logger logger) {
            this.logger = logger;
        }

        public <T> T target(Target<T> target) {
            this.errorDecoder(new AnnotatedErrorDecoder(logger, target.type()));
            return build().newInstance(target);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantedTargetInterceptor tenantedTargetInterceptor() {
        return new TenantedTargetInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenedTargetInterceptor tokenedTargetInterceptor() {
        return new TokenedTargetInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder() {
        return new GsonDecoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Encoder feignEncoder() {
        return new GsonEncoder();
    }

    @Bean(name = ApiConfiguration.LOGGER_NAME)
    public Logger logger() {
        return LoggerFactory.getLogger(ApiConfiguration.LOGGER_NAME);
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public Feign.Builder feignBuilder(@Qualifier(ApiConfiguration.LOGGER_NAME) final Logger logger) {
        return new AnnotatedErrorDecoderFeignBuilder(logger);
    }

    @PostConstruct
    public void configureHystrixConcurrencyStrategy() {
        List<HystrixCallableWrapper> wrappers = new ArrayList<>();
        wrappers.add(new ThreadLocalAwareCallableWrapper());
        HystrixPlugins instance = HystrixPlugins.getInstance();
        try {
        instance.registerConcurrencyStrategy(
                new HystrixContextAwareConcurrencyStrategy(wrappers));
        } catch(IllegalStateException e) {
            logger.warn("Another concurrency strategy is already registered in hystrix!");
        }
    }
}
