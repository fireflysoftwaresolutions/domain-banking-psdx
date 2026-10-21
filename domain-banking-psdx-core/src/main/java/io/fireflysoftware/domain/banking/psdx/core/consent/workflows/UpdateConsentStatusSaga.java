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
import io.fireflysoftware.domain.banking.psdx.core.consent.StoredConsent;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.LoadConsentCommand;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.UpdateConsentStatusCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Saga(name = UpdateConsentStatusSaga.NAME)
@Service
public class UpdateConsentStatusSaga {
    public static final String NAME = "UpdateConsentStatusSaga";
    public static final String LOAD = "load";
    public static final String STEP = "update";
    public static final String COMPENSATE = "restore";
    private final CommandBus commandBus;
    public UpdateConsentStatusSaga(CommandBus commandBus) { this.commandBus = commandBus; }
    @SagaStep(id = LOAD)
    @StepEvent(type = "psdx.consent.loaded")
    public Mono<StoredConsent> load(LoadConsentCommand command, ExecutionContext ctx) {
        return commandBus.<StoredConsent>send(command).doOnNext(current -> ctx.putVariable("previous", current));
    }
    @SagaStep(id = STEP, dependsOn = LOAD, compensate = COMPENSATE)
    @StepEvent(type = "psdx.consent.status.updated")
    public Mono<StoredConsent> update(UpdateConsentStatusCommand command, ExecutionContext ctx) {
        return commandBus.send(command);
    }
    public Mono<Void> restore(StoredConsent updated, ExecutionContext ctx) {
        StoredConsent previous = ctx.getVariableAs("previous", StoredConsent.class);
        if (previous == null || previous.id() == null || previous.status() == null) return Mono.empty();
        return commandBus.send(new UpdateConsentStatusCommand(previous.id(), previous.status())).then();
    }
}
