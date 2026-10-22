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

import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.ledger.sdk.model.TransactionDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDTransactionDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reads ledger transactions for one account. An account filter never falls back to the first ledger page.
 */
public final class SdkTransactionServiceAdapter {

    private final TransactionsApi transactions;

    public SdkTransactionServiceAdapter(TransactionsApi transactions) {
        this.transactions = transactions;
    }

    public Flux<PSDTransactionDTO> getTransactionsByAccountId(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        if (accountId == null) {
            return Flux.empty();
        }
        return transactions.listTransactionsByAccount(accountId, SdkCalls.key())
                .map(this::toTransaction)
                .filter(transaction -> within(transaction, fromDate, toDate));
    }

    public Mono<PSDTransactionDTO> getTransactionById(UUID transactionId) {
        return SdkCalls.emptyIfMissing(transactions.getTransaction(transactionId, SdkCalls.key()))
                .map(this::toTransaction);
    }

    private PSDTransactionDTO toTransaction(TransactionDTO transaction) {
        LocalDate booked = transaction.getBookingDate() == null ? null : transaction.getBookingDate().toLocalDate();
        LocalDate value = transaction.getValueDate() == null ? booked : transaction.getValueDate().toLocalDate();
        return PSDTransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .transactionStatus(ledgerStatus(transaction))
                .bookingDate(booked)
                .valueDate(value)
                .creditorName(transaction.getDescription())
                .transactionAmount(transaction.getTotalAmount() == null ? null : PSDTransactionDTO.PSDAmountDTO.builder()
                        .currency(transaction.getCurrency())
                        .amount(transaction.getTotalAmount())
                        .build())
                .build();
    }

    private static String ledgerStatus(TransactionDTO transaction) {
        if (transaction.getTransactionStatus() == null) {
            return null;
        }
        String value = transaction.getTransactionStatus().getValue().toLowerCase();
        return switch (value) {
            case "booked", "posted", "settled", "completed" -> "booked";
            case "pending", "authorized", "authorised" -> "pending";
            case "rejected", "declined", "failed" -> "rejected";
            case "cancelled", "canceled", "reversed" -> "cancelled";
            default -> value;
        };
    }

    private static boolean within(PSDTransactionDTO transaction, LocalDate fromDate, LocalDate toDate) {
        LocalDate booked = transaction.getBookingDate();
        if (booked == null) {
            return fromDate == null && toDate == null;
        }
        if (fromDate != null && booked.isBefore(fromDate)) {
            return false;
        }
        return toDate == null || !booked.isAfter(toDate);
    }
}
