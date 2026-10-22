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

package io.fireflysoftware.domain.banking.psdx.web;

import io.fireflysoftware.domain.banking.psdx.core.directory.SdkStaffIdentityAdapter;
import io.fireflysoftware.domain.banking.psdx.core.directory.StaffPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Live staff sign-in. Present only when {@code integration.identity.enabled=true}.
 * The account is read from core-common-user-mgmt by {@link SdkStaffIdentityAdapter}.
 */
@RestController
@RequestMapping("/api/v1/directory/staff")
@ConditionalOnProperty(name = "integration.identity.enabled", havingValue = "true")
public class StaffIdentityController {

    private final SdkStaffIdentityAdapter identity;

    public StaffIdentityController(SdkStaffIdentityAdapter identity) {
        this.identity = identity;
    }

    @PostMapping
    public Mono<ResponseEntity<StaffPrincipal>> authenticate(@RequestBody StaffSignIn request) {
        if (request == null || request.username() == null || request.password() == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return identity.authenticate(request.username(), request.password())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public record StaffSignIn(String username, String password) {
    }
}
