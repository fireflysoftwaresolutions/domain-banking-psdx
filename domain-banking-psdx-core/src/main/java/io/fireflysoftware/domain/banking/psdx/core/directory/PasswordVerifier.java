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

import reactor.core.publisher.Mono;

/**
 * Checks a presented password. core-common-user-mgmt publishes no password.
 * {@link SdkStaffIdentityAdapter} calls an implementation only when
 * {@code integration.identity.verifier-url} is set.
 * {@link HttpPasswordVerifier} is the class an installer replaces.
 */
public interface PasswordVerifier {

    Mono<Boolean> matches(String username, String presentedPassword);
}
