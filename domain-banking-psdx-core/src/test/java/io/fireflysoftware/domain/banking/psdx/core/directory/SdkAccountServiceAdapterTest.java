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
import io.fireflysoftware.core.banking.accounts.sdk.model.AccountDTO;
import io.fireflysoftware.core.contract.sdk.api.GlobalContractPartiesApi;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SdkAccountServiceAdapterTest {

    @Test
    void readsTheAccountFromTheAccountsCore() {
        UUID accountId = UUID.fromString("10000000-0000-4000-8000-000000000010");
        AccountsApi accounts = mock(AccountsApi.class);
        AccountDTO row = new AccountDTO();
        row.setAccountNumber("DE89370400440532013000");
        row.setCurrency("EUR");
        row.setAccountStatus(AccountDTO.AccountStatusEnum.OPEN);
        when(accounts.getAccount(eq(accountId), any())).thenReturn(Mono.just(row));

        var account = new SdkAccountServiceAdapter(accounts, mock(AccountBalancesApi.class), mock(GlobalContractPartiesApi.class))
                .getAccountById(accountId)
                .block();

        verify(accounts).getAccount(eq(accountId), any());
        assertEquals("DE89370400440532013000", account.getIban());
        assertEquals("enabled", account.getStatus());
    }
}
