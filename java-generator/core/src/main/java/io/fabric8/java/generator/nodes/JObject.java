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
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JObject extends AbstractJSONSchema2Pojo {

    private static final String JAVA_UTIL_MAP = "java.util.Map";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";

    private static final Logger LOGGER = LoggerFactory.getLogger(JObject.class);
    private static final Set<String> IGNORED_FIELDS = new HashSet<>();

    static {
        IGNORED_FIELDS.add("description");
        IGNORED_FIELDS.add("schema");
        IGNORED_FIELDS.add("example");
        IGNORED_FIELDS.add("examples");
    }

    private String type = null;
    private Map<String, AbstractJSONSchema2Pojo> fields = new HashMap<>();
    private Map<String, String> descriptions = new HashMap<>();
    private Set<String> required = new HashSet<>();
    private JObjectOptions options;

    public JObject(
            String type,
            Map<String, JSONSchemaProps> fields,
            List<String> required,
            JObjectOptions options) {
        this.options = options;

        if (required != null) {
            this.required.addAll(required);
        }

        String nextPrefix = options.getPrefix();
        String nextSuffix = options.getSuffix();

        if (type.toLowerCase(Locale.ROOT).equals("spec")) {
            nextPrefix = "";
            nextSuffix = "Spec";
        }

        this.type =
                AbstractJSONSchema2Pojo.disambiguateTypeName(
                        AbstractJSONSchema2Pojo.sanitizeString(
                                options.getPrefix()
                                        + type.substring(0, 1).toUpperCase()
                                        + type.substring(1)
                                        + options.getSuffix()));

        if (fields == null) {
            // no fields
        } else {
            for (Map.Entry<String, JSONSchemaProps> field : fields.entrySet()) {
                String key = field.getKey();
                JSONSchemaProps value = field.getValue();

                if (value.getDescription() != null) {
                    this.descriptions.put(key, value.getDescription().replace("\"", "\\\""));
                }
                if (!IGNORED_FIELDS.contains(key))
                    this.fields.put(
                            field.getKey(),
                            AbstractJSONSchema2Pojo.fromJsonSchema(
                                    field.getKey(), value, nextPrefix, nextSuffix));
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

        if (clz != null) {
            throw new RuntimeException("Found duplicated class " + clz);
        }

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

        if (this.options.isPreserveUnknownFields()) {
            if (!clz.getFieldByName(ADDITIONAL_PROPERTIES).isPresent()) {
                ClassOrInterfaceType mapType =
                        new ClassOrInterfaceType()
                                .setName(JAVA_UTIL_MAP)
                                .setTypeArguments(
                                        new ClassOrInterfaceType().setName("String"),
                                        new ClassOrInterfaceType().setName("Object"));
                FieldDeclaration objField =
                        clz.addField(mapType, ADDITIONAL_PROPERTIES, Modifier.Keyword.PRIVATE);
                objField.setVariables(
                        new NodeList<>(
                                new VariableDeclarator()
                                        .setName(ADDITIONAL_PROPERTIES)
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

        List<String> buffer = new ArrayList<>(this.fields.size() + 1);

        // CU to expand inner Enums
        CompilationUnit supportCU = new CompilationUnit();
        List<String> sortedKeys = this.fields.keySet().stream().sorted().collect(Collectors.toList());
        for (String k : sortedKeys) {
            AbstractJSONSchema2Pojo prop = this.fields.get(k);
            boolean isRequired = this.required.contains(k);
            boolean hasDescription = this.descriptions.containsKey(k);

            GeneratorResult gr = prop.generateJava(supportCU);

            // For now the inner types are only for enums
            if (!gr.getInnerClasses().isEmpty()) {
                for (String enumName : gr.getInnerClasses()) {
                    Optional<EnumDeclaration> ed = supportCU.getEnumByName(enumName);
                    if (ed.isPresent()) {
                        clz.addMember(ed.get());
                    }
                }
            }

            gr = prop.generateJava(cu);
            buffer.addAll(gr.getTopLevelClasses());

            String originalFieldName = k;
            String fieldName = AbstractJSONSchema2Pojo.sanitizeString(k);
            String fieldType = AbstractJSONSchema2Pojo.sanitizeString(prop.getType());

            assert (!clz.getFieldByName(fieldName).isPresent());

            try {
                FieldDeclaration objField =
                        clz.addField(fieldType, fieldName, Modifier.Keyword.PRIVATE);
                objField.addAnnotation(
                        new SingleMemberAnnotationExpr(
                                new Name("com.fasterxml.jackson.annotation.JsonProperty"),
                                new StringLiteralExpr(originalFieldName)));

                if (isRequired) {
                    objField.addAnnotation("javax.validation.constraints.NotNull");
                }

                if (hasDescription) {
                    objField.addAnnotation(
                            new SingleMemberAnnotationExpr(
                                    new Name(
                                            "com.fasterxml.jackson.annotation.JsonPropertyDescription"),
                                    new StringLiteralExpr(this.descriptions.get(k))));
                }

                objField.createGetter();
                objField.createSetter();
            } catch (Exception cause) {
                throw new RuntimeException(
                        "Error generating field " + fieldName + " with type " + prop.getType(),
                        cause);
            }
        }
        buffer.add(this.type);

        return new GeneratorResult(buffer);
    }
}
