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

package io.fireflysoftware.domain.banking.psdx.core.consent;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentRequestDTO;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class SandboxConsentAdapter implements ConsentPort {
    private final ConcurrentHashMap<UUID, String> rows = new ConcurrentHashMap<>();
    @Override public Mono<ConsentView> read(UUID id) {
        if (id != null && rows.containsKey(id)) return Mono.just(new ConsentView(id, rows.get(id)));
        UUID resolved = id == null ? UUID.fromString("00000000-0000-4000-8000-000000000001") : id;
        return Mono.just(new ConsentView(resolved, "RECEIVED"));
    }
    @Override public Mono<StoredConsent> load(UUID id) {
        String status = id == null ? null : rows.get(id);
        if (status == null) return Mono.error(new IllegalStateException("Consent " + id + " was not found"));
        return Mono.just(new StoredConsent(id, status));
    }
    @Override public Mono<StoredConsent> create(PSDConsentRequestDTO draft) {
        if (draft == null) return Mono.error(new IllegalArgumentException("Consent is required"));
        UUID id = UUID.randomUUID();
        rows.put(id, "RECEIVED");
        return Mono.just(new StoredConsent(id, "RECEIVED"));
    }
    @Override public Mono<StoredConsent> updateStatus(UUID id, String status) {
        if (id == null || !rows.containsKey(id)) return Mono.error(new IllegalStateException("Consent " + id + " was not found"));
        if (status == null || status.isBlank()) return Mono.error(new IllegalArgumentException("Status is required"));
        rows.put(id, status);
        return Mono.just(new StoredConsent(id, status));
    }
    @Override public Mono<Void> remove(UUID id) {
        if (id != null) rows.remove(id);
        return Mono.empty();
    }
}
