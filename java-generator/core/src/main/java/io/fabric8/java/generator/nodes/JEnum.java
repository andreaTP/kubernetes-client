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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JEnum extends AbstractJSONSchema2Pojo {

    private String type = null;
    // TODO: handle number enum
    private List<String> values;

    public JEnum(String type, List<JsonNode> values) {
      this.type =
        AbstractJSONSchema2Pojo.sanitizeString(
            type.substring(0, 1).toUpperCase()
            + type.substring(1));
      this.values = new ArrayList<>(values.size());
      for (JsonNode v: values) {
        this.values.add(v.textValue());
      }
    }

    @Override
    public String getType() {
        return this.type;
    }

  @Override
  public GeneratorResult generateJava(CompilationUnit cu) {
    EnumDeclaration en = cu.getEnumByName(this.type).orElse(null);

    if (en == null) {
      en = cu.addEnum(this.type);

      for (String k : this.values) {
        en.addEnumConstant(k);
      }
    }

    List<String> ret = new ArrayList<>(1);
    ret.add(this.type);
    return new GeneratorResult(new ArrayList<>(), ret);
  }
}
