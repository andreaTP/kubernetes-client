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
package io.fabric8.java.generator;

import com.github.javaparser.ast.CompilationUnit;
import io.fabric8.java.generator.nodes.AbstractJSONSchema2Pojo;
import io.fabric8.java.generator.nodes.JCRObject;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.File;
import java.util.*;

public class CRGeneratorRunner {

    public void run(File source, File basePath) {
        try (final KubernetesClient client = new DefaultKubernetesClient()) {
            // Parse CRD with fabric8
            CustomResourceDefinition crd =
                    client.apiextensions().v1().customResourceDefinitions().load(source).get();

            List<WritableCRCompilationUnit> writables =
                    generate(crd, getPackage(crd.getSpec().getGroup()));

            for (WritableCRCompilationUnit w : writables) {
                w.writeAllJavaClasses(basePath);
            }
        }
    }

    public List<WritableCRCompilationUnit> generate(
            CustomResourceDefinition crd, Optional<String> basePackageName) {
        CustomResourceDefinitionSpec crSpec = crd.getSpec();
        String crName = crSpec.getNames().getKind();
        String group = crSpec.getGroup();

        List<WritableCRCompilationUnit> writableCUs =
                new ArrayList<WritableCRCompilationUnit>(crSpec.getVersions().size());
        for (CustomResourceDefinitionVersion crdv : crSpec.getVersions()) {
            CompilationUnit cu = new CompilationUnit();

            String version = crdv.getName();

            String pkg = basePackageName.map((p) -> p + "." + version).orElse(version);

            cu.setPackageDeclaration(pkg);

            AbstractJSONSchema2Pojo crGenerator = new JCRObject(crName, version, group);

            AbstractJSONSchema2Pojo specGenerator =
                    AbstractJSONSchema2Pojo.fromJsonSchema(
                            "spec",
                            crdv.getSchema().getOpenAPIV3Schema().getProperties().get("spec"),
                            crName,
                            "");

            AbstractJSONSchema2Pojo statusGenerator =
                    AbstractJSONSchema2Pojo.fromJsonSchema(
                            "status",
                            crdv.getSchema().getOpenAPIV3Schema().getProperties().get("status"),
                            crName,
                            "");

            List<String> classNames = new ArrayList<String>();

            classNames.addAll(crGenerator.generateJava(cu));
            classNames.addAll(specGenerator.generateJava(cu));
            classNames.addAll(statusGenerator.generateJava(cu));

            writableCUs.add(new WritableCRCompilationUnit(cu, classNames));
        }

        return writableCUs;
    }

    private Optional<String> getPackage(String group) {
        if (group == null) {
            return Optional.empty();
        }

        Stack<String> stack = new Stack<String>();
        for (String s : group.split("\\.")) {
            stack.push(s);
        }
        StringBuilder packageName = new StringBuilder();
        packageName.append(stack.pop());
        while (!stack.empty()) {
            packageName.append(".");
            packageName.append(stack.pop().replace("-", "_"));
        }

        return Optional.of(packageName.toString());
    }
}
