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

public class Main {

  private static void assertions(List<CertificateRequestSpec.Usages> usagesList) {
    assert(usagesList.size() == 4);
    assert(usagesList.get(0) == CertificateRequestSpec.Usages.signing);
    assert(usagesList.get(1) == CertificateRequestSpec.Usages.digital_signature);
    assert(usagesList.get(2) == CertificateRequestSpec.Usages.server_auth);
    assert(usagesList.get(3) == CertificateRequestSpec.Usages.s_mime);
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
