/*
 * Copyright 2025 Firefly Software Foundation
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

package io.fireflysoftware.domain.banking.psdx;

import io.fireflysoftware.core.banking.psdx.sdk.api.ConsentManagementApi;
import io.fireflysoftware.core.banking.psdx.hub.sdk.invoker.ApiClient;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentDTO;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Live adapter. Calls core-banking-psdx-sdk. Selected with firefly.psdx.consent.adapter=core. */
public final class SdkAdapter implements CorePort {
    private final ConsentManagementApi api;

    public SdkAdapter(String basePath) {
        ApiClient client = new ApiClient();
        client.setBasePath(basePath);
        this.api = new ConsentManagementApi(client);
    }

    SdkAdapter(ConsentManagementApi api) {
        this.api = api;
    }

    @Override
    public Mono<ConsentView> read(UUID id) {
        return api.getConsent(id, null).map(dto -> new ConsentView(dto.getId(), dto.getConsentStatus()));
    }
}
