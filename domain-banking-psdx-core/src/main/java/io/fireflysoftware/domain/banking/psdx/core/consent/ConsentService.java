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

package io.fireflysoftware.domain.banking.psdx.core.consent;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentRequestDTO;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.LoadConsentCommand;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.RegisterConsentCommand;
import io.fireflysoftware.domain.banking.psdx.core.consent.commands.UpdateConsentStatusCommand;
import io.fireflysoftware.domain.banking.psdx.core.consent.workflows.RegisterConsentSaga;
import io.fireflysoftware.domain.banking.psdx.core.consent.workflows.UpdateConsentStatusSaga;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.UUID;
@Service
public class ConsentService {
    private final SagaEngine engine;
    private final ConsentPort port;
    public ConsentService(SagaEngine engine, ConsentPort port) { this.engine = engine; this.port = port; }
    public Mono<ConsentView> read(UUID id) { return port.read(id); }
    public Mono<StoredConsent> register(PSDConsentRequestDTO draft) {
        StepInputs inputs = StepInputs.builder().forStepId(RegisterConsentSaga.STEP, new RegisterConsentCommand(draft)).build();
        return engine.execute(RegisterConsentSaga.NAME, inputs).flatMap(result -> outcome(result, RegisterConsentSaga.STEP));
    }
    public Mono<StoredConsent> updateStatus(UUID id, String status) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(UpdateConsentStatusSaga.LOAD, new LoadConsentCommand(id))
                .forStepId(UpdateConsentStatusSaga.STEP, new UpdateConsentStatusCommand(id, status))
                .build();
        return engine.execute(UpdateConsentStatusSaga.NAME, inputs).flatMap(result -> outcome(result, UpdateConsentStatusSaga.STEP));
    }
    private static Mono<StoredConsent> outcome(SagaResult result, String stepId) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() -> new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(stepId, StoredConsent.class))
                .switchIfEmpty(Mono.error(new IllegalStateException("Saga step " + stepId + " produced no result")));
    }
}
