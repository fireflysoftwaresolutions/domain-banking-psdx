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
import io.fireflysoftware.core.banking.psdx.sdk.api.ConsentManagementApi;
import io.fireflysoftware.core.banking.psdx.sdk.model.PSDConsentDTO;
import io.fireflysoftware.domain.banking.psdx.infra.ConsentClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class SdkConsentAdapterTest {
    @Test void sandboxIsTheDefaultAndDoesNotOpenASocket() {
        new ApplicationContextRunner().withUserConfiguration(ConsentAdapterConfiguration.class).run(context -> {
            ConsentPort port = context.getBean(ConsentPort.class);
            assertThat(port).isInstanceOf(SandboxConsentAdapter.class);
            assertThat(port.read(null).block().label()).isEqualTo("RECEIVED");
        });
    }
    @Test void corePropertySelectsTheSdkAdapter() {
        new ApplicationContextRunner().withUserConfiguration(ConsentAdapterConfiguration.class, ConsentClientFactory.class)
                .withPropertyValues("firefly.psdx.consent.adapter=core")
                .run(context -> assertThat(context.getBean(ConsentPort.class)).isInstanceOf(SdkConsentAdapter.class));
    }
    @Test void liveReadCallsTheCoreSdk() {
        UUID id = UUID.randomUUID();
        ConsentManagementApi api = mock(ConsentManagementApi.class);
        PSDConsentDTO dto = mock(PSDConsentDTO.class);
        when(dto.getId()).thenReturn(id);
        when(api.getConsent(id, null)).thenReturn(Mono.just(dto));
        assertThat(new SdkConsentAdapter(api).read(id).block().id()).isEqualTo(id);
        verify(api).getConsent(id, null);
    }
}
