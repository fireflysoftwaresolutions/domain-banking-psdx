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
import io.fireflysoftware.core.banking.psdx.sdk.api.ConsentManagementApi;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentRequestDTO;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentStatusDTO;
import reactor.core.publisher.Mono;
import java.util.UUID;
public final class SdkConsentAdapter implements ConsentPort {
    private final ConsentManagementApi api;
    public SdkConsentAdapter(ConsentManagementApi api) { this.api = api; }
    @Override public Mono<ConsentView> read(UUID id) {
        return api.getConsent(id, null).map(dto -> new ConsentView(dto.getId(), dto.getConsentStatus()));
    }
    @Override public Mono<StoredConsent> load(UUID id) {
        return api.getConsent(id, null).map(dto -> new StoredConsent(dto.getId() == null ? id : dto.getId(), dto.getConsentStatus()));
    }
    @Override public Mono<StoredConsent> create(PSDConsentRequestDTO draft) {
        return api.createConsent(draft, key()).map(dto -> new StoredConsent(dto.getId(), dto.getConsentStatus()));
    }
    @Override public Mono<StoredConsent> updateStatus(UUID id, String status) {
        PSDConsentStatusDTO body = new PSDConsentStatusDTO().consentStatus(status);
        return api.updateConsentStatus(id, body, key()).map(dto -> new StoredConsent(dto.getId() == null ? id : dto.getId(), dto.getConsentStatus()));
    }
    @Override public Mono<Void> remove(UUID id) {
        return api.revokeConsent(id, key()).then();
    }
    private static String key() { return UUID.randomUUID().toString(); }
}
