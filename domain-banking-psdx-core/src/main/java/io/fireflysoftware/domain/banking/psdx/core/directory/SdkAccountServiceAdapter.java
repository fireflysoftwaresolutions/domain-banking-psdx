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

import io.fireflysoftware.core.banking.accounts.sdk.api.AccountBalancesApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountBalanceDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.banking.accounts.sdk.model.FilterRequestAccountDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDAccountDTO;
import io.fireflysoftware.core.banking.psdx.interfaces.dtos.PSDBalanceDTO;
import io.fireflysoftware.core.contract.sdk.api.GlobalContractPartiesApi;
import io.fireflysoftware.core.contract.sdk.model.ContractPartyDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reads accounts and balances from core-banking-accounts and the owning party from core-common-contract-mgmt.
 */
public final class SdkAccountServiceAdapter {

    private final AccountsApi accounts;
    private final AccountBalancesApi balances;
    private final GlobalContractPartiesApi parties;

    public SdkAccountServiceAdapter(AccountsApi accounts, AccountBalancesApi balances, GlobalContractPartiesApi parties) {
        this.accounts = accounts;
        this.balances = balances;
        this.parties = parties;
    }

    public Flux<PSDAccountDTO> getAccountsByPartyId(UUID partyId) {
        if (partyId == null) {
            return Flux.empty();
        }
        return parties.getContractPartiesByPartyId(partyId, Boolean.TRUE, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(page.getContent() == null ? List.<ContractPartyDTO>of() : page.getContent()))
                .map(ContractPartyDTO::getContractId)
                .filter(Objects::nonNull)
                .distinct()
                .concatMap(contractId -> accountsForContract(contractId)
                        .map(account -> toAccount(account, partyId)));
    }

    public Mono<PSDAccountDTO> getAccountById(UUID accountId) {
        return SdkCalls.emptyIfMissing(accounts.getAccount(accountId, SdkCalls.key()))
                .map(account -> toAccount(account, null));
    }

    public Flux<PSDBalanceDTO> getBalancesByAccountId(UUID accountId) {
        return balanceRows(accountId).map(this::toBalance);
    }

    public Flux<PSDBalanceDTO> balancesForReference(String accountReference) {
        if (accountReference == null || accountReference.isBlank()) {
            return Flux.empty();
        }
        AccountDTO filter = new AccountDTO();
        filter.setAccountNumber(accountReference.replace(" ", ""));
        FilterRequestAccountDTO request = new FilterRequestAccountDTO();
        request.setFilters(filter);
        return accounts.filterAccounts(request, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(page.getContent() == null ? List.<AccountDTO>of() : page.getContent()))
                .filter(account -> sameNumber(account.getAccountNumber(), accountReference))
                .take(1)
                .concatMap(account -> balanceRows(account.getAccountId()))
                .map(this::toBalance);
    }

    private Flux<AccountDTO> accountsForContract(UUID contractId) {
        AccountDTO filter = new AccountDTO();
        filter.setContractId(contractId);
        FilterRequestAccountDTO request = new FilterRequestAccountDTO();
        request.setFilters(filter);
        return accounts.filterAccounts(request, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(page.getContent() == null ? List.<AccountDTO>of() : page.getContent()));
    }

    private Flux<AccountBalanceDTO> balanceRows(UUID accountId) {
        return balances.getAllBalances(accountId, 0, 50, null, null, SdkCalls.key())
                .flatMapMany(page -> Flux.fromIterable(SdkCalls.convert(page.getContent(), AccountBalanceDTO.class)));
    }

    private static boolean sameNumber(String stored, String requested) {
        if (stored == null || requested == null) {
            return false;
        }
        return stored.replace(" ", "").equalsIgnoreCase(requested.replace(" ", ""));
    }

    private static PSDAccountDTO toAccount(AccountDTO account, UUID partyId) {
        return PSDAccountDTO.builder()
                .resourceId(account.getAccountId())
                .ownerPartyId(partyId)
                .iban(account.getAccountNumber())
                .currency(account.getCurrency())
                .name(account.getDescription())
                .product(account.getAccountType() == null ? null : account.getAccountType().getValue())
                .status(status(account))
                .build();
    }

    private static String status(AccountDTO account) {
        if (account.getAccountStatus() == null) {
            return null;
        }
        return switch (account.getAccountStatus()) {
            case OPEN -> "enabled";
            case CLOSED -> "deleted";
            case SUSPENDED, DORMANT -> "blocked";
            case DRAFT -> "draft";
        };
    }

    private PSDBalanceDTO toBalance(AccountBalanceDTO balance) {
        BigDecimal amount = balance.getBalanceAmount() == null ? BigDecimal.ZERO : balance.getBalanceAmount();
        return PSDBalanceDTO.builder()
                .balanceType(balance.getBalanceType() == null ? null : balance.getBalanceType().getValue())
                .creditDebitIndicator(amount.signum() < 0 ? "DBIT" : "CRDT")
                .referenceDate(balance.getAsOfDatetime())
                .balanceAmount(PSDBalanceDTO.PSDAmountDTO.builder()
                        .amount(amount.abs())
                        .build())
                .build();
    }
}
