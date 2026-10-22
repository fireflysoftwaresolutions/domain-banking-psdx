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
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountBalancesApi;
import io.fireflysoftware.core.banking.accounts.sdk.api.AccountsApi;
import io.fireflysoftware.core.banking.cards.sdk.api.CardBalancesApi;
import io.fireflysoftware.core.banking.cards.sdk.api.CardTransactionsApi;
import io.fireflysoftware.core.banking.cards.sdk.api.CardsApi;
import io.fireflysoftware.core.banking.ledger.sdk.api.TransactionsApi;
import io.fireflysoftware.core.banking.payments.sdk.api.PaymentOrdersApi;
import io.fireflysoftware.core.contract.sdk.api.GlobalContractPartiesApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Live PSD2 directory. Selected when {@code firefly.psdx.directory.adapter=core}.
 * The default leaves these clients unwired, so the process opens no socket.
 */
@Configuration
@ConditionalOnProperty(prefix = "firefly.psdx.directory", name = "adapter", havingValue = "core")
public class DirectoryConfiguration {

    @Bean
    public SdkAccountServiceAdapter accountDirectory(
            @Value("${firefly.psdx.directory.accounts-base-url}") String accountsUrl,
            @Value("${firefly.psdx.directory.contracts-base-url}") String contractsUrl) {
        io.fireflysoftware.core.banking.accounts.sdk.invoker.ApiClient accounts = new io.fireflysoftware.core.banking.accounts.sdk.invoker.ApiClient();
        accounts.setBasePath(SdkCalls.base(accountsUrl));
        io.fireflysoftware.core.contract.sdk.invoker.ApiClient contracts = new io.fireflysoftware.core.contract.sdk.invoker.ApiClient();
        contracts.setBasePath(SdkCalls.base(contractsUrl));
        return new SdkAccountServiceAdapter(new AccountsApi(accounts), new AccountBalancesApi(accounts), new GlobalContractPartiesApi(contracts));
    }

    @Bean
    public SdkCardServiceAdapter cardDirectory(@Value("${firefly.psdx.directory.cards-base-url}") String cardsUrl) {
        io.fireflysoftware.core.banking.cards.sdk.invoker.ApiClient cards = new io.fireflysoftware.core.banking.cards.sdk.invoker.ApiClient();
        cards.setBasePath(SdkCalls.base(cardsUrl));
        return new SdkCardServiceAdapter(new CardsApi(cards), new CardBalancesApi(cards), new CardTransactionsApi(cards));
    }

    @Bean
    public SdkPaymentServiceAdapter paymentDirectory(@Value("${firefly.psdx.directory.payments-base-url}") String paymentsUrl) {
        io.fireflysoftware.core.banking.payments.sdk.invoker.ApiClient payments = new io.fireflysoftware.core.banking.payments.sdk.invoker.ApiClient();
        payments.setBasePath(SdkCalls.base(paymentsUrl));
        return new SdkPaymentServiceAdapter(new PaymentOrdersApi(payments));
    }

    @Bean
    public SdkTransactionServiceAdapter transactionDirectory(@Value("${firefly.psdx.directory.ledger-base-url}") String ledgerUrl) {
        io.fireflysoftware.core.banking.ledger.sdk.invoker.ApiClient ledger = new io.fireflysoftware.core.banking.ledger.sdk.invoker.ApiClient();
        ledger.setBasePath(SdkCalls.base(ledgerUrl));
        return new SdkTransactionServiceAdapter(new TransactionsApi(ledger));
    }

    @Bean
    public SdkScaServiceAdapter scaDirectory(
            @Value("${firefly.psdx.directory.sca-base-url}") String scaUrl,
            @Value("${psdx.sca.required-for-all-payments:true}") boolean required,
            @Value("${psdx.sca.exemption-threshold-amount:30.00}") double amount,
            @Value("${psdx.sca.exemption-threshold-currency:EUR}") String currency) {
        io.fireflysoftware.common.sca.sdk.invoker.ApiClient sca = new io.fireflysoftware.common.sca.sdk.invoker.ApiClient();
        sca.setBasePath(SdkCalls.base(scaUrl));
        return new SdkScaServiceAdapter(new ScaOperationControllerApi(sca), new ScaChallengeControllerApi(sca), required, amount, currency);
    }
}
