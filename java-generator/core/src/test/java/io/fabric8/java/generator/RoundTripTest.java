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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.google.testing.compile.JavaFileObjects;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.tools.JavaFileObject;

import io.fabric8.kubernetes.client.Version;
import io.fabric8.zjsonpatch.JsonDiff;
import io.fabric8.zjsonpatch.JsonPatch;
import org.apache.maven.shared.invoker.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

class RoundTripTest {

  private static TemporaryFolder tmpFolder = TemporaryFolder.builder().assureDeletion().build();

  CRGeneratorRunner runner = new CRGeneratorRunner();

  File getResource(String name) {
    try {
      return Paths.get(this.getClass().getClassLoader().getResource(name).toURI()).toFile();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  File roundtripPom = getResource("roundtrip-pom.xml");

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

  @BeforeAll
  public static void beforeAll() throws IOException {
    tmpFolder.create();
  }

  // TODO: use a library
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";

  // Execute with:
  // mvn clean test -f java-generator/core/pom.xml -Dtest=RoundTripTest -Dmaven.executable=$(which mvn)
  @Test
  void testCrontabCRDCompiles() throws Exception {
    // Arrange
    File crd = getResource("crontab-crd.yml");
    File baseDir = tmpFolder.newFolder("crontab");
    File dest = tmpFolder.newFolder("crontab", "src", "main", "java");
    File pom = baseDir.toPath().resolve("pom.xml").toFile();
    Files.copy(roundtripPom.toPath(), pom.toPath());

    // Act
    runner.run(crd, dest);

    // TODO improve the maven invocation stuffs
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(pom);
    request.setGoals(Collections.singletonList( "compile" ));
    Properties props = new Properties();
    props.put("io-fabric8.version", Version.clientVersion());
    request.setProperties(props);

    File workingDir = tmpFolder.getRoot().toPath().resolve("crontab").toFile();
    request.setBaseDirectory(workingDir);
    Invoker invoker = new DefaultInvoker();

   File mavenExecutable = new File(System.getProperty("maven.executable"));
   invoker.setMavenExecutable(mavenExecutable);
   invoker.setMavenHome(mavenExecutable.getParentFile().getParentFile());
   invoker.setWorkingDirectory(workingDir);

   InvocationResult mvnExecutionResult = invoker.execute( request );

    File generatedCRD = Arrays.stream(workingDir.toPath()
      .resolve("target")
      .resolve("classes")
      .resolve("META-INF")
      .resolve("fabric8")
      .toFile()
        .listFiles())
      .filter(f -> f.getName().endsWith("v1.yml")).findFirst().get();

    JsonNode originalCRDJson = yamlMapper.readTree(crd);
    JsonNode generatedCRDJson = yamlMapper.readTree(generatedCRD);

    // TODO: check if needed
    // EnumSet<DiffFlags> flags = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone()
    JsonNode diff = JsonDiff.asJson(originalCRDJson, generatedCRDJson);

    List<JsonNode> aggregatedDiffs = StreamSupport.stream(diff.spliterator(), false).collect(Collectors.toList());

    JsonNode generatedDiff = JsonPatch.apply(diff, originalCRDJson);

    DiffRowGenerator generator = DiffRowGenerator.create()
      .showInlineDiffs(true)
      .inlineDiffByWord(true)
      .oldTag(b -> b ? ANSI_RED : ANSI_RESET)
      .newTag(b -> b ? ANSI_GREEN : ANSI_RESET)
      .build();

    List<DiffRow> rows = generator.generateDiffRows(
      Arrays.asList(yamlMapper.writeValueAsString(originalCRDJson).split("\n")),
      Arrays.asList(yamlMapper.writeValueAsString(generatedDiff).split("\n")));

    int maxWidth = 0;
    for (DiffRow row : rows) {
      maxWidth = Math.max(
        maxWidth,
        Math.max(row.getOldLine().getBytes(StandardCharsets.UTF_8).length, row.getNewLine().getBytes(StandardCharsets.UTF_8).length)
      );
    }

    // Result should be similar to: https://www.yamldiff.com/
    for (DiffRow row : rows) {
      System.out.printf("%-" + maxWidth + "s %-" + maxWidth + "s\n", row.getOldLine(), row.getNewLine());
    }

    // Assert
    assertEquals(0, mvnExecutionResult.getExitCode());
    assertTrue(aggregatedDiffs.size() < 7);
  }

  @AfterAll
  public static void afterAll() {
    tmpFolder.delete();
  }
}
