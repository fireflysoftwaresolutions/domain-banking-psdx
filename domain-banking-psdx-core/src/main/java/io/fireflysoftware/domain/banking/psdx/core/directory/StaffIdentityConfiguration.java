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

import io.fireflysoftware.core.users.sdk.api.UserAccountsApi;
import io.fireflysoftware.core.users.sdk.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Live PSD2 staff lookup. Selected when {@code integration.identity.enabled=true}.
 * The default leaves this client unwired, so the process opens no socket.
 * The PSD2 core keeps its sandbox users and does not call core-common-user-mgmt.
 */
@Configuration
@ConditionalOnProperty(name = "integration.identity.enabled", havingValue = "true")
public class StaffIdentityConfiguration {

    @Bean
    public SdkStaffIdentityAdapter staffIdentity(
            @Value("${integration.identity.base-url}") String baseUrl,
            @Value("${integration.identity.verifier-url:}") String verifierUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(SdkCalls.base(baseUrl));
        PasswordVerifier verifier = verifierUrl == null || verifierUrl.isBlank()
                ? null
                : new HttpPasswordVerifier(verifierUrl);
        return new SdkStaffIdentityAdapter(new UserAccountsApi(client), verifier);
    }
}
