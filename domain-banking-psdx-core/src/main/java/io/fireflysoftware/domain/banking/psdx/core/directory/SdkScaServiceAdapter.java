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
import io.fireflysoftware.common.sca.sdk.model.SCAChallengeDTO;
import io.fireflysoftware.common.sca.sdk.model.SCAOperationDTO;
import io.fireflysoftware.common.sca.sdk.model.ValidationResultDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAAuthenticationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAAuthenticationResponseDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAValidationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAValidationResponseDTO;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Issues and checks challenges through core-common-sca-mgmt. The word sandbox is never stored or accepted as a code.
 */
public final class SdkScaServiceAdapter {

    private final ScaOperationControllerApi operations;
    private final ScaChallengeControllerApi challenges;
    private final boolean requiredForAllPayments;
    private final double exemptionThresholdAmount;
    private final String exemptionThresholdCurrency;
    private final SecureRandom random = new SecureRandom();

    public SdkScaServiceAdapter(ScaOperationControllerApi operations,
                                 ScaChallengeControllerApi challenges,
                                 boolean requiredForAllPayments,
                                 double exemptionThresholdAmount,
                                 String exemptionThresholdCurrency) {
        this.operations = operations;
        this.challenges = challenges;
        this.requiredForAllPayments = requiredForAllPayments;
        this.exemptionThresholdAmount = exemptionThresholdAmount;
        this.exemptionThresholdCurrency = exemptionThresholdCurrency;
    }

    public Mono<PSDSCAAuthenticationResponseDTO> initiateSCA(PSDSCAAuthenticationRequestDTO request) {
        String code = digits();
        SCAOperationDTO operation = new SCAOperationDTO()
                .referenceId(request == null ? null : request.getResourceId())
                .operationType(SCAOperationDTO.OperationTypeEnum.TRANSFER)
                .partyId(request == null || request.getPartyId() == null ? null : request.getPartyId().toString())
                .status(SCAOperationDTO.StatusEnum.PENDING);
        return operations.createOperation(operation, SdkCalls.key())
                .flatMap(created -> {
                    if (created.getId() == null) {
                        return Mono.error(new IllegalStateException("core-common-sca-mgmt did not return an operation id"));
                    }
                    SCAChallengeDTO challenge = new SCAChallengeDTO()
                            .scaOperationId(created.getId())
                            .challengeCode(code)
                            .expiresAt(LocalDateTime.now().plusMinutes(5).withNano(0))
                            .used(false);
                    return challenges.createChallenge(created.getId(), challenge, SdkCalls.key())
                            .map(stored -> response(created, code, stored));
                });
    }

    public Mono<PSDSCAValidationResponseDTO> validateSCA(PSDSCAValidationRequestDTO request) {
        String submitted = request == null ? null : request.getAuthenticationCode();
        if (submitted == null || submitted.isBlank()
                || "sandbox".equalsIgnoreCase(submitted)
                || "123456".equals(submitted)) {
            return Mono.just(rejected("Invalid authentication code"));
        }
        try {
            var operationId = java.util.UUID.fromString(request.getChallengeId());
            return operations.validateSCA(operationId, submitted, SdkCalls.key())
                    .map(result -> accepted(result, operationId.toString()))
                    .onErrorResume(SdkCalls::missing, error -> Mono.just(rejected("Challenge not found")));
        } catch (IllegalArgumentException ex) {
            return Mono.just(rejected("Challenge not found"));
        }
    }

    public Mono<Boolean> isSCARequired(Double amount, String currency) {
        if (requiredForAllPayments) {
            return Mono.just(true);
        }
        if (amount != null && currency != null
                && amount <= exemptionThresholdAmount
                && exemptionThresholdCurrency.equals(currency)) {
            return Mono.just(false);
        }
        return Mono.just(true);
    }

    private PSDSCAAuthenticationResponseDTO response(SCAOperationDTO operation, String issued, SCAChallengeDTO challenge) {
        String returned = challenge.getChallengeCode();
        String code = returned == null || returned.isBlank() ? issued : returned;
        if (code == null || code.isBlank() || "sandbox".equalsIgnoreCase(code) || "123456".equals(code)) {
            throw new IllegalStateException("core-common-sca-mgmt did not return a challenge code");
        }
        int expires = 300;
        if (challenge.getExpiresAt() != null) {
            expires = (int) Math.max(1, ChronoUnit.SECONDS.between(LocalDateTime.now(), challenge.getExpiresAt()));
        }
        return PSDSCAAuthenticationResponseDTO.builder()
                .challengeId(operation.getId().toString())
                .method("SOFTWARE_TOKEN")
                .expiresIn(expires)
                .additionalInfo(code)
                .build();
    }

    private static PSDSCAValidationResponseDTO accepted(ValidationResultDTO result, String operationId) {
        boolean success = result != null && Boolean.TRUE.equals(result.getSuccess());
        if (!success) {
            return rejected(result == null ? "Invalid authentication code" : result.getMessage());
        }
        return PSDSCAValidationResponseDTO.builder()
                .success(true)
                .authenticationToken(operationId)
                .expiresIn(3600)
                .build();
    }

    private static PSDSCAValidationResponseDTO rejected(String message) {
        return PSDSCAValidationResponseDTO.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }

    private String digits() {
        int value = 100000 + random.nextInt(900000);
        if (value == 123456) {
            value = 123457;
        }
        return Integer.toString(value);
    }
}
