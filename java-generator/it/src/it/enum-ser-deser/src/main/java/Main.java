/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.cert_manager.v1.CertificateRequest;
import io.cert_manager.v1.CertificateRequestSpec;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Main {

  private static void assertions(List<CertificateRequestSpec.Usages> usagesList) {
    assertThat(usagesList.size()).isEqualTo(4);
    assertThat(usagesList.get(0)).isEqualTo(CertificateRequestSpec.Usages.signing);
    assertThat(usagesList.get(1)).isEqualTo(CertificateRequestSpec.Usages.digital_signature);
    assertThat(usagesList.get(2)).isEqualTo(CertificateRequestSpec.Usages.server_auth);
    assertThat(usagesList.get(3)).isEqualTo(CertificateRequestSpec.Usages.s_mime);
  }

  public static void main(String... args) {
    CertificateRequest sample =
      Serialization.unmarshal(Main.class.getResourceAsStream("sample1.yaml"), CertificateRequest.class);

    assertions(sample.getSpec().getUsages());

    // Round-trip serialization
    String yaml = Serialization.asYaml(sample);

    System.out.println(yaml);

    CertificateRequest req =
      Serialization.unmarshal(yaml, CertificateRequest.class);

    assertions(req.getSpec().getUsages());
  }
}
