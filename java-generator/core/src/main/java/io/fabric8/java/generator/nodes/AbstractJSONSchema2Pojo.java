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

import static io.fabric8.java.generator.nodes.Keywords.JAVA_KEYWORDS;

import com.github.javaparser.ast.CompilationUnit;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import java.util.List;

public abstract class AbstractJSONSchema2Pojo {

    public abstract String getType();

    public abstract List<String> generateJava(CompilationUnit cu);

    public static String sanitizeString(String str) {
        String sanitized = "";
        if (JAVA_KEYWORDS.stream().filter((s) -> s.equals(str)).findFirst().isPresent()) {
            sanitized = "_" + str;
        } else {
            sanitized = str;
        }

        if (sanitized.startsWith("-")) {
            sanitized = sanitized.replaceFirst("-", "minus");
        }

        int index = sanitized.indexOf('-');
        while (index != -1) {
            int next = Math.min(index + 2, sanitized.length());
            sanitized =
                    sanitized.substring(0, index)
                            + sanitized.substring(index + 1, next).toUpperCase()
                            + sanitized.substring(next);
            index = sanitized.indexOf('-');
        }

        return sanitized;
    }

    public static AbstractJSONSchema2Pojo fromJsonSchema(
            String key, JSONSchemaProps prop, String prefix, String suffix) {
        if (prop.getXKubernetesIntOrString() != null && prop.getXKubernetesIntOrString()) {
            return fromJsonSchema(
                    key,
                    new JPrimitiveNameAndType("io.fabric8.kubernetes.api.model.IntOrString"),
                    prop,
                    prefix,
                    suffix);
        } else if (prop.getType() == null
                && prop.getXKubernetesPreserveUnknownFields() != null
                && prop.getXKubernetesPreserveUnknownFields()) {
            return fromJsonSchema(key, new JObjectNameAndType(key), prop, prefix, suffix);
        } else {
            if (prop.getType() == null) {
                throw new RuntimeException("Type for key:" + key + " is null");
            }

            switch (prop.getType()) {
                case "boolean":
                    return fromJsonSchema(
                            key, new JPrimitiveNameAndType("Boolean"), prop, prefix, suffix);
                case "integer":
                    String intFormat = prop.getFormat();
                    if (intFormat == null) intFormat = "int64";

                    switch (intFormat) {
                        case "int32":
                            return fromJsonSchema(
                                    key,
                                    new JPrimitiveNameAndType("Integer"),
                                    prop,
                                    prefix,
                                    suffix);
                        case "int64":
                        default:
                            return fromJsonSchema(
                                    key, new JPrimitiveNameAndType("Long"), prop, prefix, suffix);
                    }
                case "number":
                    String numberFormat = prop.getFormat();
                    if (numberFormat == null) numberFormat = "double";

                    switch (numberFormat) {
                        case "float":
                            return fromJsonSchema(
                                    key, new JPrimitiveNameAndType("Float"), prop, prefix, suffix);
                        case "double":
                        default:
                            return fromJsonSchema(
                                    key, new JPrimitiveNameAndType("Double"), prop, prefix, suffix);
                    }
                case "string":
                    return fromJsonSchema(
                            key, new JPrimitiveNameAndType("String"), prop, prefix, suffix);
                case "object":
                    // Taking the schema defined in AdditionalProperties instead
                    if (prop.getAdditionalProperties() != null
                            && prop.getAdditionalProperties().getSchema() != null) {
                        return fromJsonSchema(
                                key, prop.getAdditionalProperties().getSchema(), prefix, suffix);
                    } else {
                        return fromJsonSchema(
                                key, new JObjectNameAndType(key), prop, prefix, suffix);
                    }
                case "array":
                    return fromJsonSchema(key, new JArrayNameAndType(key), prop, prefix, suffix);
                default:
                    throw new RuntimeException("unmanaged type " + prop.getType());
            }
        }
    }

    private static AbstractJSONSchema2Pojo fromJsonSchema(
            String key, JavaNameAndType nt, JSONSchemaProps prop, String prefix, String suffix) {
        switch (nt.getType()) {
            case PRIMITIVE:
                return new JPrimitive(nt.getName());
            case ARRAY:
                return new JArray(fromJsonSchema(key, prop.getItems().getSchema(), prefix, suffix));
            case OBJECT:
                if (prop.getAdditionalProperties() != null &&
                    prop.getAdditionalProperties().getSchema() != null) {
                    return new JMap(
                            fromJsonSchema(key, prop.getAdditionalProperties().getSchema(), prefix, suffix));
                } else {
                    boolean preserveUnknownFields =
                            (prop.getXKubernetesPreserveUnknownFields() != null
                                    && prop.getXKubernetesPreserveUnknownFields());
                    return new JObject(
                            key,
                            prop.getProperties(),
                            new JObjectOptions(preserveUnknownFields, prefix, suffix));
                }
            default:
                throw new RuntimeException("unreachable " + nt.getType());
        }
    }
}
