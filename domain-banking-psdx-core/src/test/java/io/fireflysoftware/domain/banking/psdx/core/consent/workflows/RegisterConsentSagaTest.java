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

package io.fireflysoftware.domain.banking.psdx.core.consent.workflows;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentRequestDTO;
import io.fireflysoftware.domain.banking.psdx.core.consent.StoredConsent;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.RegisterConsentCommand;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.RemoveConsentCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class RegisterConsentSagaTest {
    @Mock private CommandBus commandBus;
    @Test void removeRevokesTheCreatedConsent() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveConsentCommand.class))).thenReturn(Mono.empty());
        StepVerifier.create(new RegisterConsentSaga(commandBus).remove(new StoredConsent(id, "RECEIVED"))).verifyComplete();
        ArgumentCaptor<RemoveConsentCommand> captor = ArgumentCaptor.forClass(RemoveConsentCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(id);
    }
    @Test void registerStoresTheCoreResult() {
        UUID id = UUID.randomUUID();
        StoredConsent created = new StoredConsent(id, "RECEIVED");
        when(commandBus.send(any(RegisterConsentCommand.class))).thenReturn(Mono.just(created));
        ExecutionContext ctx = ExecutionContext.forSaga(RegisterConsentSaga.NAME, "test");
        StepVerifier.create(new RegisterConsentSaga(commandBus).register(new RegisterConsentCommand(new PSDConsentRequestDTO()), ctx))
                .expectNext(created).verifyComplete();
        assertThat(ctx.getVariableAs("created", StoredConsent.class).id()).isEqualTo(id);
    }
}
