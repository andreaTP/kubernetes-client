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
/* (C)2015 */
package io.fabric8.java.generator.nodes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

public class JMap extends AbstractJSONSchema2Pojo {

    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String JAVA_UTIL_MAP = "java.util.Map";

    private String type = null;
    private AbstractJSONSchema2Pojo nested = null;

    public JMap(AbstractJSONSchema2Pojo nested) {
        this.type =
                new ClassOrInterfaceType()
                        .setName(JAVA_UTIL_MAP)
                        .setTypeArguments(
                                new ClassOrInterfaceType().setName(JAVA_LANG_STRING),
                                new ClassOrInterfaceType()
                                        .setName(
                                                AbstractJSONSchema2Pojo.sanitizeString(
                                                        nested.getType())))
                        .toString();
        this.nested = nested;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public GeneratorResult generateJava(CompilationUnit cu) {
        return nested.generateJava(cu);
    }
}
