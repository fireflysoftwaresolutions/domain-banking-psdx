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

package io.fireflysoftware.domain.banking.psdx.core.directory;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Posts the presented password to {@code integration.identity.verifier-url}.
 * core-common-user-mgmt publishes no password, so this check is not an operation of that SDK.
 * A 2xx response accepts the password. Any other outcome rejects it.
 * An installer replaces this class when the verifier protocol is different.
 */
public final class HttpPasswordVerifier implements PasswordVerifier {

    private final WebClient client;

    public HttpPasswordVerifier(String endpoint) {
        this(client(endpoint));
    }

    private static WebClient client(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("integration.identity.verifier-url is not set");
        }
        return WebClient.builder().baseUrl(SdkCalls.base(endpoint)).build();
    }

    HttpPasswordVerifier(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<Boolean> matches(String username, String presentedPassword) {
        if (username == null || username.isBlank() || presentedPassword == null || presentedPassword.isBlank()) {
            return Mono.just(false);
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", presentedPassword);
        return client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
}
