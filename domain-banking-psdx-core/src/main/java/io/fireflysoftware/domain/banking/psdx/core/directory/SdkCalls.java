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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SdkCalls {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private SdkCalls() {
    }

    static String key() {
        return UUID.randomUUID().toString();
    }

    public static String base(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Live service base URL is not set");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    static boolean missing(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof WebClientResponseException http && http.getStatusCode().value() == 404) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    static <T> Mono<T> emptyIfMissing(Mono<T> call) {
        return call.onErrorResume(SdkCalls::missing, error -> Mono.empty());
    }

    static <T> List<T> convert(List<?> content, Class<T> type) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        List<T> rows = new ArrayList<>();
        for (Object item : content) {
            if (item == null) {
                continue;
            }
            if (type.isInstance(item)) {
                rows.add(type.cast(item));
            } else {
                rows.add(MAPPER.convertValue(item, type));
            }
        }
        return rows;
    }
}
