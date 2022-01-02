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
package io.fabric8.java.generator.nodes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import java.util.*;
import java.util.stream.Collectors;

public class JObject extends AbstractJSONSchema2Pojo {

    private static final Set<String> IGNORED_FIELDS = new HashSet<>();

    static {
        IGNORED_FIELDS.add("description");
        IGNORED_FIELDS.add("schema");
        IGNORED_FIELDS.add("example");
        IGNORED_FIELDS.add("examples");
    }

    private String type = null;
    private Map<String, AbstractJSONSchema2Pojo> fields = new HashMap<>();
    private JObjectOptions options;

    public JObject(String type, Map<String, JSONSchemaProps> fields, JObjectOptions options) {
        this.options = options;

        String nextPrefix = options.getPrefix();
        String nextSuffix = options.getSuffix();

        if (type.toLowerCase(Locale.ROOT).equals("spec")) {
            nextPrefix = "";
            nextSuffix = "Spec";
        }

        this.type =
                AbstractJSONSchema2Pojo.sanitizeString(
                        options.getPrefix()
                                + type.substring(0, 1).toUpperCase()
                                + type.substring(1)
                                + options.getSuffix());

        if (fields == null) {
            // no fields
        } else {
            for (Map.Entry<String, JSONSchemaProps> field : fields.entrySet()) {
                if (!IGNORED_FIELDS.contains(field.getKey()))
                    this.fields.put(
                            field.getKey(),
                            AbstractJSONSchema2Pojo.fromJsonSchema(
                                    field.getKey(), field.getValue(), nextPrefix, nextSuffix));
            }
        }
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public GeneratorResult generateJava(CompilationUnit cu) {
        ClassOrInterfaceDeclaration clz = cu.getClassByName(this.type).orElse(null);

        if (clz == null) {
            clz = cu.addClass(this.type);

            clz.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("com.fasterxml.jackson.annotation.JsonInclude"),
                            new NameExpr(
                                    "com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL")));

            List<String> sortedFields =
                    this.fields.keySet().stream().sorted().collect(Collectors.toList());
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            while (!sortedFields.isEmpty()) {
                sb.append("\"" + sortedFields.remove(0) + "\"");
                if (!sortedFields.isEmpty()) {
                    sb.append(",");
                }
            }
            sb.append("}");

            clz.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("com.fasterxml.jackson.annotation.JsonPropertyOrder"),
                            new NameExpr(sb.toString())));

            clz.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("com.fasterxml.jackson.databind.annotation.JsonDeserialize"),
                            new NameExpr(
                                    "using = com.fasterxml.jackson.databind.JsonDeserializer.None.class")));

            clz.addAnnotation("lombok.ToString");
            clz.addAnnotation("lombok.EqualsAndHashCode");
            clz.addAnnotation("lombok.Setter");

            clz.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("lombok.experimental.Accessors"),
                            new NameExpr("prefix = {\n" + "    \"_\",\n" + "    \"\"\n" + "}")));

            clz.addAnnotation(
                    new SingleMemberAnnotationExpr(
                            new Name("io.sundr.builder.annotations.Buildable"),
                            new NameExpr(
                                    "editableEnabled = false, validationEnabled = false, generateBuilderPackage = false, builderPackage = \"io.fabric8.kubernetes.api.builder\", refs = {\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ObjectReference.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.LabelSelector.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Container.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.EnvVar.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.ContainerPort.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.Volume.class),\n"
                                            + "    @io.sundr.builder.annotations.BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class)\n"
                                            + "}")));

            clz.addImplementedType("io.fabric8.kubernetes.api.model.KubernetesResource");
        }

        if (this.options.isPreserveUnknownFields()) {
            if (!clz.getFieldByName("additionalProperties").isPresent()) {
                ClassOrInterfaceType mapType =
                        new ClassOrInterfaceType()
                                .setName("java.util.Map")
                                .setTypeArguments(
                                        new ClassOrInterfaceType().setName("String"),
                                        new ClassOrInterfaceType().setName("Object"));
                FieldDeclaration objField =
                        clz.addField(mapType, "additionalProperties", Modifier.Keyword.PRIVATE);
                objField.setVariables(
                        new NodeList<>(
                                new VariableDeclarator()
                                        .setName("additionalProperties")
                                        .setType(mapType)
                                        .setInitializer(
                                                "new java.util.HashMap<String, Object>()")));

                objField.addAnnotation("com.fasterxml.jackson.annotation.JsonIgnore");

                objField.createGetter()
                        .addAnnotation("com.fasterxml.jackson.annotation.JsonAnyGetter");
                objField.createSetter()
                        .addAnnotation("com.fasterxml.jackson.annotation.JsonAnySetter");
            } else {
                // Warning ???
            }
        }

        List<String> buffer = new ArrayList<String>(this.fields.size() + 1);
        for (String k : this.fields.keySet()) {
            AbstractJSONSchema2Pojo prop = this.fields.get(k);

            GeneratorResult gr = prop.generateJava(cu);

            // For now the inner types are only for enums
            if (gr.getInnerClasses().size() > 0) {
              for (String enumName: gr.getInnerClasses()) {
                if (cu.getEnumByName(enumName).isPresent() &&
                  !clz.getMembers().contains(cu.getEnumByName(enumName).get())) {

                  clz.addMember(cu.getEnumByName(enumName).get());

                  // removing this enum from the top level compilation unit
                  cu.remove(cu.getEnumByName(enumName).get());
                }
              }
            }

            buffer.addAll(gr.getTopLevelClasses());

            String originalFieldName = k;
            String fieldName = AbstractJSONSchema2Pojo.sanitizeString(k);
            String fieldType = AbstractJSONSchema2Pojo.sanitizeString(prop.getType());

            if (!clz.getFieldByName(fieldName).isPresent()) {
                try {
                    FieldDeclaration objField =
                            clz.addField(fieldType, fieldName, Modifier.Keyword.PRIVATE);
                    objField.addAnnotation(
                            new SingleMemberAnnotationExpr(
                                    new Name("com.fasterxml.jackson.annotation.JsonProperty"),
                                    new StringLiteralExpr(originalFieldName)));
                    objField.createGetter();
                    objField.createSetter();
                } catch (Exception cause) {
                    throw new RuntimeException(
                            "Error generating field " + fieldName + " with type " + prop.getType(),
                            cause);
                }
            } else {
                // Warning ???
            }
        }
        buffer.add(this.type);

        return new GeneratorResult(buffer);
    }
}
