/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.banking.psdx.infra;
import io.fireflysoftware.core.banking.psdx.hub.sdk.invoker.ApiClient;
import io.fireflysoftware.core.banking.psdx.sdk.api.ConsentManagementApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ConsentClientFactory {
    private final ApiClient client;
    public ConsentClientFactory(@Value("${firefly.psdx.consent.core-base-url:http://localhost:8080}") String basePath) {
        this.client = new ApiClient();
        this.client.setBasePath(basePath);
    }
    @Bean
    public ConsentManagementApi consentApi() { return new ConsentManagementApi(client); }
}
