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
public interface ConsentPort {
    Mono<ConsentView> read(UUID id);
    Mono<StoredConsent> load(UUID id);
    Mono<StoredConsent> create(PSDConsentRequestDTO draft);
    Mono<StoredConsent> updateStatus(UUID id, String status);
    Mono<Void> remove(UUID id);
}
