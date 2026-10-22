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
import io.fireflysoftware.core.users.sdk.model.PaginationResponse;
import io.fireflysoftware.core.users.sdk.model.UserAccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SdkStaffIdentityAdapterTest {

    @Test
    void sandboxIsTheDefaultAndDoesNotOpenASocket() {
        new ApplicationContextRunner()
                .withUserConfiguration(StaffIdentityConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SdkStaffIdentityAdapter.class);
                    assertThat(context).doesNotHaveBean(UserAccountsApi.class);
                });
    }

    @Test
    void livePropertySelectsTheUserAccountSdk() {
        new ApplicationContextRunner()
                .withUserConfiguration(StaffIdentityConfiguration.class)
                .withPropertyValues(
                        "integration.identity.enabled=true",
                        "integration.identity.base-url=http://127.0.0.1:9")
                .run(context -> assertThat(context).hasSingleBean(SdkStaffIdentityAdapter.class));
    }

    @Test
    void liveLookupDoesNotAuthenticateWithoutAVerifier() {
        UserAccountDTO account = account();
        UserAccountsApi api = accountsReturning(account);
        var adapter = new SdkStaffIdentityAdapter(api, null);

        assertThat(adapter.authenticate("ada@example.com", "secret").block(Duration.ofSeconds(2))).isNull();
        verify(api).filterUserAccounts(any(), any());
        StaffPrincipal found = adapter.findByUsername("ada@example.com").block(Duration.ofSeconds(2));
        assertThat(found.username()).isEqualTo("ada@example.com");
        assertThat(found.id()).isEqualTo(account.getId());
        assertThat(found.toString()).doesNotContain("secret");
    }

    @Test
    void liveAdapterAuthenticatesOnlyThroughTheConfiguredVerifier() {
        UserAccountDTO account = account();
        UserAccountsApi api = accountsReturning(account);
        PasswordVerifier verifier = (username, password) -> Mono.just("secret".equals(password));
        var adapter = new SdkStaffIdentityAdapter(api, verifier);

        StaffPrincipal accepted = adapter.authenticate("ada@example.com", "secret").block(Duration.ofSeconds(2));
        assertThat(accepted.username()).isEqualTo("ada@example.com");
        assertThat(accepted.toString()).doesNotContain("secret");

        assertThat(adapter.authenticate("ada@example.com", "other").block(Duration.ofSeconds(2))).isNull();
        assertThat(adapter.authenticate("ada@example.com", "sandbox").block(Duration.ofSeconds(2))).isNull();
        assertThat(adapter.authenticate("ada@example.com", "123456").block(Duration.ofSeconds(2))).isNull();

        AtomicReference<String> seen = new AtomicReference<>();
        WebClient client = WebClient.builder()
                .baseUrl("http://verifier.internal/check")
                .exchangeFunction(request -> {
                    seen.set(request.url().toString());
                    return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
                })
                .build();
        assertThat(new HttpPasswordVerifier(client).matches("ada@example.com", "secret").block(Duration.ofSeconds(2)))
                .isTrue();
        assertThat(seen.get()).isEqualTo("http://verifier.internal/check");
        assertThatThrownBy(() -> new HttpPasswordVerifier(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verifier-url");
    }

    private static UserAccountDTO account() {
        UserAccountDTO account = new UserAccountDTO(UUID.fromString("11111111-1111-4111-8111-111111111111"));
        account.setEmail("ada@example.com");
        account.setIsActive(true);
        return account;
    }

    private static UserAccountsApi accountsReturning(UserAccountDTO account) {
        java.util.List<Object> content = new ArrayList<>();
        content.add(account);
        PaginationResponse page = new PaginationResponse();
        page.setContent(content);
        UserAccountsApi api = mock(UserAccountsApi.class);
        when(api.filterUserAccounts(any(), any())).thenReturn(Mono.just(page));
        return api;
    }
}
