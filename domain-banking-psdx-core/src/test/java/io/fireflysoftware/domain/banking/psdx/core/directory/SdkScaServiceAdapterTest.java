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

import io.fireflysoftware.common.sca.sdk.api.ScaChallengeControllerApi;
import io.fireflysoftware.common.sca.sdk.api.ScaOperationControllerApi;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAValidationRequestDTO;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SdkScaServiceAdapterTest {

    @Test
    void sandboxAndFixedCodeStayRejected() {
        ScaOperationControllerApi operations = mock(ScaOperationControllerApi.class);
        ScaChallengeControllerApi challenges = mock(ScaChallengeControllerApi.class);
        SdkScaServiceAdapter adapter = new SdkScaServiceAdapter(operations, challenges, true, 30.00, "EUR");

        var sandbox = adapter.validateSCA(PSDSCAValidationRequestDTO.builder()
                .challengeId("sca-challenge")
                .authenticationCode("sandbox")
                .build()).block(Duration.ofSeconds(2));
        var fixed = adapter.validateSCA(PSDSCAValidationRequestDTO.builder()
                .challengeId("sca-challenge")
                .authenticationCode("123456")
                .build()).block(Duration.ofSeconds(2));

        assertFalse(sandbox.getSuccess());
        assertEquals("Invalid authentication code", sandbox.getErrorMessage());
        assertFalse(fixed.getSuccess());
        assertEquals("Invalid authentication code", fixed.getErrorMessage());
        verifyNoInteractions(operations, challenges);
    }
}
