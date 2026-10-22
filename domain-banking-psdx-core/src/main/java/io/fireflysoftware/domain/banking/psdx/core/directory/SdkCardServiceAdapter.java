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

import io.fireflysoftware.core.banking.cards.sdk.api.CardBalancesApi;
import io.fireflysoftware.core.banking.cards.sdk.api.CardTransactionsApi;
import io.fireflysoftware.core.banking.cards.sdk.api.CardsApi;
import io.fireflysoftware.core.banking.cards.sdk.model.CardBalanceDTO;
import io.fireflysoftware.core.banking.cards.sdk.model.CardDTO;
import io.fireflysoftware.core.banking.cards.sdk.model.CardTransactionDTO;
import io.fireflysoftware.core.banking.cards.sdk.model.FilterRequestCardDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDBalanceDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDCardAccountDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDTransactionDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reads cards, balances, and card transactions from core-banking-cards.
 */
public final class SdkCardServiceAdapter {

    private final CardsApi cards;
    private final CardBalancesApi balances;
    private final CardTransactionsApi transactions;

    public SdkCardServiceAdapter(CardsApi cards, CardBalancesApi balances, CardTransactionsApi transactions) {
        this.cards = cards;
        this.balances = balances;
        this.transactions = transactions;
    }

    public Flux<PSDCardAccountDTO> getCardAccountsByPartyId(UUID partyId) {
        if (partyId == null) {
            return Flux.empty();
        }
        CardDTO filter = new CardDTO();
        filter.setPartyId(partyId);
        FilterRequestCardDTO request = new FilterRequestCardDTO();
        request.setFilters(filter);
        return cards.filterCards(request, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(page.getContent() == null ? List.<CardDTO>of() : page.getContent()))
                .filter(card -> partyId.equals(card.getPartyId()))
                .map(this::toCard);
    }

    public Mono<PSDCardAccountDTO> getCardAccountById(UUID cardId) {
        return SdkCalls.emptyIfMissing(cards.getCard(cardId, SdkCalls.key())).map(this::toCard);
    }

    public Flux<PSDBalanceDTO> getBalancesByCardId(UUID cardId) {
        return balances.getAllBalances(cardId, 0, 50, null, null, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(SdkCalls.convert(page.getContent(), CardBalanceDTO.class)))
                .map(this::toBalance);
    }

    public Flux<PSDTransactionDTO> getTransactionsByCardId(UUID cardId, LocalDate fromDate, LocalDate toDate) {
        return transactions.getAllTransactions(cardId, null, null, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(SdkCalls.convert(page.getContent(), CardTransactionDTO.class)))
                .map(this::toTransaction)
                .filter(transaction -> within(transaction, fromDate, toDate));
    }

    public Mono<PSDTransactionDTO> getTransactionByCardIdAndTransactionId(UUID cardId, UUID transactionId) {
        return SdkCalls.emptyIfMissing(transactions.getTransaction(cardId, transactionId, SdkCalls.key()))
                .map(this::toTransaction);
    }

    private PSDCardAccountDTO toCard(CardDTO card) {
        return PSDCardAccountDTO.builder()
                .resourceId(card.getCardId())
                .ownerPartyId(card.getPartyId())
                .maskedPan(card.getMaskedCardNumber())
                .currency(card.getCurrencyCode())
                .name(card.getCardHolderName())
                .status(card.getCardStatus() == null ? null : card.getCardStatus().getValue().toLowerCase())
                .build();
    }

    private PSDBalanceDTO toBalance(CardBalanceDTO balance) {
        BigDecimal amount = balance.getBalanceAmount() == null ? BigDecimal.ZERO : balance.getBalanceAmount();
        return PSDBalanceDTO.builder()
                .balanceType(balance.getBalanceType())
                .creditDebitIndicator(amount.signum() < 0 ? "DBIT" : "CRDT")
                .referenceDate(balance.getAsOfDate())
                .balanceAmount(PSDBalanceDTO.PSDAmountDTO.builder()
                        .amount(amount.abs())
                        .currency(balance.getCurrencyCode())
                        .build())
                .build();
    }

    private PSDTransactionDTO toTransaction(CardTransactionDTO transaction) {
        LocalDate booked = transaction.getCardTransactionTimestamp() == null
                ? null : transaction.getCardTransactionTimestamp().toLocalDate();
        return PSDTransactionDTO.builder()
                .transactionId(transaction.getCardTransactionId())
                .transactionStatus(cardStatus(transaction))
                .bookingDate(booked)
                .valueDate(booked)
                .creditorName(transaction.getCardMerchantName())
                .build();
    }

    private static String cardStatus(CardTransactionDTO transaction) {
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
