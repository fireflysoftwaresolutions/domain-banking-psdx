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

import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDAccountDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDBalanceDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDCardAccountDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentInitiationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDPaymentStatusDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAAuthenticationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAAuthenticationResponseDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAValidationRequestDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDSCAValidationResponseDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDTransactionDTO;
import io.fireflysoftware.domain.banking.psdx.core.directory.SdkAccountServiceAdapter;
import io.fireflysoftware.domain.banking.psdx.core.directory.SdkCardServiceAdapter;
import io.fireflysoftware.domain.banking.psdx.core.directory.SdkPaymentServiceAdapter;
import io.fireflysoftware.domain.banking.psdx.core.directory.SdkScaServiceAdapter;
import io.fireflysoftware.domain.banking.psdx.core.directory.SdkTransactionServiceAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Experience calls this domain for PSD2 account, card, payment, ledger, and SCA data.
 * The PSD2 core keeps its own consent and access-log records.
 */
@RestController
@RequestMapping("/api/v1/directory")
@ConditionalOnProperty(prefix = "firefly.psdx.directory", name = "adapter", havingValue = "core")
public class DirectoryController {

    private final SdkAccountServiceAdapter accounts;
    private final SdkCardServiceAdapter cards;
    private final SdkPaymentServiceAdapter payments;
    private final SdkTransactionServiceAdapter transactions;
    private final SdkScaServiceAdapter sca;

    public DirectoryController(SdkAccountServiceAdapter accounts,
                               SdkCardServiceAdapter cards,
                               SdkPaymentServiceAdapter payments,
                               SdkTransactionServiceAdapter transactions,
                               SdkScaServiceAdapter sca) {
        this.accounts = accounts;
        this.cards = cards;
        this.payments = payments;
        this.transactions = transactions;
        this.sca = sca;
    }

    @GetMapping("/parties/{partyId}/accounts")
    public Mono<List<PSDAccountDTO>> accounts(@PathVariable UUID partyId) {
        return accounts.getAccountsByPartyId(partyId).collectList();
    }

    @GetMapping("/accounts/{accountId}")
    public Mono<PSDAccountDTO> account(@PathVariable UUID accountId) {
        return accounts.getAccountById(accountId);
    }

    @GetMapping("/accounts/{accountId}/balances")
    public Mono<List<PSDBalanceDTO>> balances(@PathVariable UUID accountId) {
        return accounts.getBalancesByAccountId(accountId).collectList();
    }

    @GetMapping("/account-references/balances")
    public Mono<List<PSDBalanceDTO>> balancesByReference(@RequestParam String reference) {
        return accounts.balancesForReference(reference).collectList();
    }

    @GetMapping("/parties/{partyId}/cards")
    public Mono<List<PSDCardAccountDTO>> cards(@PathVariable UUID partyId) {
        return cards.getCardAccountsByPartyId(partyId).collectList();
    }

    @GetMapping("/cards/{cardId}")
    public Mono<PSDCardAccountDTO> card(@PathVariable UUID cardId) {
        return cards.getCardAccountById(cardId);
    }

    @GetMapping("/cards/{cardId}/balances")
    public Mono<List<PSDBalanceDTO>> cardBalances(@PathVariable UUID cardId) {
        return cards.getBalancesByCardId(cardId).collectList();
    }

    @GetMapping("/cards/{cardId}/transactions")
    public Mono<List<PSDTransactionDTO>> cardTransactions(@PathVariable UUID cardId,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return cards.getTransactionsByCardId(cardId, from, to).collectList();
    }

    @GetMapping("/cards/{cardId}/transactions/{transactionId}")
    public Mono<PSDTransactionDTO> cardTransaction(@PathVariable UUID cardId, @PathVariable UUID transactionId) {
        return cards.getTransactionByCardIdAndTransactionId(cardId, transactionId);
    }

    @PostMapping("/payments")
    public Mono<PSDPaymentDTO> initiatePayment(@RequestBody PSDPaymentInitiationRequestDTO request) {
        return payments.initiatePayment(request);
    }

    @GetMapping("/payments/{paymentId}")
    public Mono<PSDPaymentDTO> payment(@PathVariable UUID paymentId) {
        return payments.getPayment(paymentId);
    }

    @GetMapping("/payments/{paymentId}/status")
    public Mono<PSDPaymentStatusDTO> paymentStatus(@PathVariable UUID paymentId) {
        return payments.getPaymentStatus(paymentId);
    }

    @PostMapping("/payments/{paymentId}/cancel")
    public Mono<Boolean> cancelPayment(@PathVariable UUID paymentId) {
        return payments.cancelPayment(paymentId);
    }

    @PostMapping("/payments/{paymentId}/authorize")
    public Mono<PSDPaymentDTO> authorizePayment(@PathVariable UUID paymentId, @RequestParam String authorizationCode) {
        return payments.authorizePayment(paymentId, authorizationCode);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public Mono<List<PSDTransactionDTO>> transactions(@PathVariable UUID accountId,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactions.getTransactionsByAccountId(accountId, from, to).collectList();
    }

    @GetMapping("/transactions/{transactionId}")
    public Mono<PSDTransactionDTO> transaction(@PathVariable UUID transactionId) {
        return transactions.getTransactionById(transactionId);
    }

    @PostMapping("/sca/authenticate")
    public Mono<PSDSCAAuthenticationResponseDTO> authenticate(@RequestBody PSDSCAAuthenticationRequestDTO request) {
        return sca.initiateSCA(request);
    }

    @PostMapping("/sca/validate")
    public Mono<PSDSCAValidationResponseDTO> validate(@RequestBody PSDSCAValidationRequestDTO request) {
        return sca.validateSCA(request);
    }

    @GetMapping("/sca/required")
    public Mono<Boolean> scaRequired(@RequestParam double amount, @RequestParam String currency) {
        return sca.isSCARequired(amount, currency);
    }
}
