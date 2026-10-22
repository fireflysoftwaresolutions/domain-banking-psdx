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
import io.fireflysoftware.core.users.sdk.model.FilterRequestUserAccountDTO;
import io.fireflysoftware.core.users.sdk.model.PaginationRequest;
import io.fireflysoftware.core.users.sdk.model.PaginationResponse;
import io.fireflysoftware.core.users.sdk.model.UserAccountDTO;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * Looks up a staff account through {@code UserAccountsApi}. core-common-user-mgmt publishes
 * no password, so {@link #authenticate} succeeds only when a {@link PasswordVerifier} is present.
 * Selected when {@code integration.identity.enabled=true}. The account base URL is
 * {@code integration.identity.base-url}. An installer replaces {@link HttpPasswordVerifier}.
 */
public final class SdkStaffIdentityAdapter {

    private final UserAccountsApi accounts;
    private final PasswordVerifier verifier;

    public SdkStaffIdentityAdapter(UserAccountsApi accounts, PasswordVerifier verifier) {
        this.accounts = accounts;
        this.verifier = verifier;
    }

    public Mono<StaffPrincipal> findByUsername(String username) {
        return lookup(username).map(this::principal);
    }

    public Mono<StaffPrincipal> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Mono.empty();
        }
        return lookup(username).flatMap(account -> {
            if (verifier == null) {
                return Mono.empty();
            }
            return verifier.matches(username, password)
                    .onErrorReturn(false)
                    .filter(Boolean::booleanValue)
                    .map(matched -> principal(account));
        });
    }

    private Mono<UserAccountDTO> lookup(String username) {
        if (username == null || username.isBlank()) {
            return Mono.empty();
        }
        Optional<UUID> id = parse(username);
        Mono<UserAccountDTO> found = id.isPresent()
                ? SdkCalls.emptyIfMissing(accounts.getUserAccountById(id.get(), SdkCalls.key()))
                : byEmail(username);
        return found.filter(SdkStaffIdentityAdapter::usable);
    }

    private Mono<UserAccountDTO> byEmail(String email) {
        UserAccountDTO filter = new UserAccountDTO();
        filter.setEmail(email);
        PaginationRequest pagination = new PaginationRequest();
        pagination.setPageNumber(0);
        pagination.setPageSize(5);
        FilterRequestUserAccountDTO request = new FilterRequestUserAccountDTO();
        request.setFilters(filter);
        request.setPagination(pagination);
        return SdkCalls.emptyIfMissing(accounts.filterUserAccounts(request, SdkCalls.key()))
                .flatMap(response -> matchEmail(response, email));
    }

    private static Mono<UserAccountDTO> matchEmail(PaginationResponse response, String email) {
        if (response == null) {
            return Mono.empty();
        }
        return SdkCalls.convert(response.getContent(), UserAccountDTO.class).stream()
                .filter(row -> email.equalsIgnoreCase(row.getEmail()))
                .findFirst()
                .map(Mono::just)
                .orElseGet(Mono::empty);
    }

    private StaffPrincipal principal(UserAccountDTO account) {
        String name = account.getEmail() == null || account.getEmail().isBlank()
                ? account.getId().toString()
                : account.getEmail();
        return new StaffPrincipal(account.getId(), name);
    }

    private static boolean usable(UserAccountDTO row) {
        return row != null
                && Boolean.TRUE.equals(row.getIsActive())
                && (row.getId() != null || (row.getEmail() != null && !row.getEmail().isBlank()));
    }

    private static Optional<UUID> parse(String username) {
        try {
            return Optional.of(UUID.fromString(username));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
