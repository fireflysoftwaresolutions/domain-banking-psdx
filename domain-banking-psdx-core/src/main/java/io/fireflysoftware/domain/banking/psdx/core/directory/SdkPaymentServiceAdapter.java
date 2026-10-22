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

import io.fireflysoftware.core.banking.payments.sdk.api.PaymentOrdersApi;
import io.fireflysoftware.core.banking.payments.sdk.model.PaymentOrderDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentInitiationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentStatusDTO;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Creates and reads payment orders through core-banking-payments.
 * Authorization does not mark an order complete: the payments API has no SCA step.
 */
public final class SdkPaymentServiceAdapter {

    private final PaymentOrdersApi orders;

    public SdkPaymentServiceAdapter(PaymentOrdersApi orders) {
        this.orders = orders;
    }

    public Mono<PSDPaymentDTO> initiatePayment(PSDPaymentInitiationRequestDTO paymentRequest) {
        PaymentOrderDTO order = new PaymentOrderDTO();
        order.setStatus(PaymentOrderDTO.StatusEnum.INITIATED);
        if (paymentRequest != null) {
            order.setBeneficiaryName(paymentRequest.getCreditorName());
            order.setEndToEndId(paymentRequest.getEndToEndIdentification());
            order.setRemittanceInformation(paymentRequest.getRemittanceInformationUnstructured());
            if (paymentRequest.getInstructedAmount() != null) {
                order.setAmount(paymentRequest.getInstructedAmount().getAmount());
                order.setCurrencyCode(paymentRequest.getInstructedAmount().getCurrency());
            }
            if (paymentRequest.getCreditorAccount() != null) {
                order.setBeneficiaryIban(paymentRequest.getCreditorAccount().getIban());
            }
        }
        return orders.createPaymentOrder(SdkCalls.key(), order).map(this::toPayment);
    }

    public Mono<PSDPaymentStatusDTO> getPaymentStatus(UUID paymentId) {
        return load(paymentId).map(order -> PSDPaymentStatusDTO.builder()
                .paymentId(order.getPaymentOrderId())
                .transactionStatus(status(order))
                .status(status(order))
                .build());
    }

    public Mono<PSDPaymentDTO> getPayment(UUID paymentId) {
        return load(paymentId).map(this::toPayment);
    }

    public Mono<Boolean> cancelPayment(UUID paymentId) {
        return orders.deletePaymentOrder(paymentId, SdkCalls.key())
                .thenReturn(Boolean.TRUE)
                .onErrorResume(SdkCalls::missing, error -> Mono.just(Boolean.FALSE));
    }

    public Mono<PSDPaymentDTO> authorizePayment(UUID paymentId, String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank() || "sandbox".equalsIgnoreCase(authorizationCode)) {
            return Mono.error(new IllegalArgumentException("Payment authorization code was rejected"));
        }
        return getPayment(paymentId);
    }

    private Mono<PaymentOrderDTO> load(UUID paymentId) {
        return SdkCalls.emptyIfMissing(orders.getPaymentOrderById(paymentId, SdkCalls.key()));
    }

    private PSDPaymentDTO toPayment(PaymentOrderDTO order) {
        return PSDPaymentDTO.builder()
                .paymentId(order.getPaymentOrderId())
                .transactionStatus(status(order))
                .creditorName(order.getBeneficiaryName())
                .endToEndIdentification(order.getEndToEndId())
                .instructedAmount(order.getAmount() == null ? null : PSDPaymentDTO.PSDAmountDTO.builder()
                        .amount(order.getAmount())
                        .currency(order.getCurrencyCode())
                        .build())
                .creditorAccount(order.getBeneficiaryIban() == null ? null : PSDPaymentDTO.PSDAccountReferenceDTO.builder()
                        .iban(order.getBeneficiaryIban())
                        .currency(order.getCurrencyCode())
                        .build())
                .build();
    }

    private static String status(PaymentOrderDTO order) {
        if (order.getStatus() == null) {
            return null;
        }
        return switch (order.getStatus()) {
            case COMPLETED -> "ACSC";
            case FAILED -> "RJCT";
            case INITIATED -> "PDNG";
        };
    }
}
