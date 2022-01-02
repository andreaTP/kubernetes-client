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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.fabric8.java.generator.nodes.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class GeneratorTest {

    static final JObjectOptions dummyOptions = new JObjectOptions(false, "", "");

    @Test
    void testCR() {
        // Arrange
        CompilationUnit cu = new CompilationUnit();
        JCRObject cro = new JCRObject("t", "g", "v");

        // Act
        List<String> res = cro.generateJava(cu);

        // Assert
        assertEquals(1, res.size());
        assertEquals("t", res.get(0));
    }

    @Test
    void testPrimitive() {
        // Arrange
        JPrimitive primitive = new JPrimitive("test");

        // Act
        List<String> res = primitive.generateJava(new CompilationUnit());

        // Assert
        assertEquals("test", primitive.getType());
        assertEquals(0, res.size());
    }

    @Test
    void testArrayOfPrimitives() {
        // Arrange
        JArray array = new JArray(new JPrimitive("primitive"));

        // Act
        List<String> res = array.generateJava(new CompilationUnit());

        // Assert
        assertEquals("java.util.List<primitive>", array.getType());
        assertEquals(0, res.size());
    }

    @Test
    void testMapOfPrimitives() {
        // Arrange
        JMap map = new JMap(new JPrimitive("primitive"));

        // Act
        List<String> res = map.generateJava(new CompilationUnit());

        // Assert
        assertEquals("java.util.Map<java.lang.String, primitive>", map.getType());
        assertEquals(0, res.size());
    }

    @Test
    void testEmptyObject() {
        // Arrange
        JObject obj = new JObject("t", null, dummyOptions);

        // Act
        List<String> res = obj.generateJava(new CompilationUnit());

        // Assert
        assertEquals("T", obj.getType());
        assertEquals(1, res.size());
        assertEquals("T", res.get(0));
    }

    @Test
    void testObjectOfPrimitives() {
        // Arrange
        CompilationUnit cu = new CompilationUnit();
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newBool = new JSONSchemaProps();
        newBool.setType("boolean");
        props.put("o1", newBool);
        JObject obj = new JObject("t", props, dummyOptions);

        // Act
        List<String> res = obj.generateJava(cu);

        // Assert
        assertEquals("T", obj.getType());
        assertEquals(1, res.size());
        assertEquals("T", res.get(0));

        Optional<ClassOrInterfaceDeclaration> clz = cu.getClassByName("T");
        assertTrue(clz.isPresent());
        assertEquals(1, clz.get().getFields().size());
        assertTrue(clz.get().getFieldByName("o1").isPresent());
    }

    @Test
    void testArrayOfObjects() {
        // Arrange
        JArray array = new JArray(new JObject("t", null, dummyOptions));

        // Act
        List<String> res = array.generateJava(new CompilationUnit());

        // Assert
        assertEquals("java.util.List<T>", array.getType());
        assertEquals(1, res.size());
        assertEquals("T", res.get(0));
    }

    @Test
    void testMapOfObjects() {
        // Arrange
        JMap map = new JMap(new JObject("t", null, dummyOptions));

        // Act
        List<String> res = map.generateJava(new CompilationUnit());

        // Assert
        assertEquals("java.util.Map<java.lang.String, T>", map.getType());
        assertEquals(1, res.size());
        assertEquals("T", res.get(0));
    }

    @Test
    void testObjectOfObjects() {
        // Arrange
        CompilationUnit cu = new CompilationUnit();
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newObj = new JSONSchemaProps();
        newObj.setType("object");
        props.put("o1", newObj);
        JObject obj = new JObject("t", props, dummyOptions);

        // Act
        List<String> res = obj.generateJava(cu);

        // Assert
        assertEquals(2, res.size());
        assertEquals("O1", res.get(0));
        assertEquals("T", res.get(1));

        Optional<ClassOrInterfaceDeclaration> clzT = cu.getClassByName("T");
        assertTrue(clzT.isPresent());
        assertEquals(1, clzT.get().getFields().size());
        assertTrue(clzT.get().getFieldByName("o1").isPresent());
        Optional<ClassOrInterfaceDeclaration> clzO1 = cu.getClassByName("O1");
        assertTrue(clzO1.isPresent());
    }

    @Test
    void testObjectWithPreservedFields() {
        // Arrange
        CompilationUnit cu = new CompilationUnit();
        JObject obj = new JObject("t", null, new JObjectOptions(true, "", ""));

        // Act
        List<String> res = obj.generateJava(cu);

        // Assert
        assertEquals(1, res.size());
        assertEquals("T", res.get(0));

        Optional<ClassOrInterfaceDeclaration> clzT = cu.getClassByName("T");
        assertTrue(clzT.isPresent());
        assertTrue(clzT.get().getFieldByName("additionalProperties").isPresent());
    }
}
