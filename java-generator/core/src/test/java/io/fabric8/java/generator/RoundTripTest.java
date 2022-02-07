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

import io.fabric8.kubernetes.client.Version;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.maven.shared.invoker.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

public class RoundTripTest {
    private static TemporaryFolder tmpFolder = TemporaryFolder.builder().build();

    CRGeneratorRunner runner = new CRGeneratorRunner();

    void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) {
        try {
            Files.walk(Paths.get(sourceDirectoryLocation))
                    .forEach(
                            source -> {
                                Path destination =
                                        Paths.get(
                                                destinationDirectoryLocation,
                                                source.toString()
                                                        .substring(
                                                                sourceDirectoryLocation.length()));
                                try {
                                    Files.copy(source, destination);
                                } catch (IOException e) {
                                    // ignore
                                }
                            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void prepareTmpFolder(String name, File dest) throws Exception {
        copyDirectory(
                Paths.get(
                                Paths.get(
                                                this.getClass()
                                                        .getClassLoader()
                                                        .getResource("roundtrip")
                                                        .toURI())
                                        .toFile()
                                        .getAbsolutePath(),
                                name)
                        .toFile()
                        .getAbsolutePath(),
                dest.getAbsolutePath());
    }

    File getJavaSourcesDir(File dest) {
        return Paths.get(dest.getAbsolutePath(), "src", "main", "java").toFile();
    }

    File getCRD(String name) throws Exception {
        return Paths.get(this.getClass().getClassLoader().getResource(name).toURI()).toFile();
    }

    InvocationResult execMaven(File folder, String... command)
            throws MavenInvocationException, IOException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList(command));
        request.setOffline(false);
        request.setQuiet(true);
        request.setBatchMode(true);
        Properties props = new Properties();
        props.put("io-fabric8.version", Version.clientVersion());
        request.setProperties(props);

        request.setBaseDirectory(folder);
        Invoker invoker = new DefaultInvoker();

        // MAVEN_HOME is tailored on the Windows build in CI
        Optional<String> mvnHomeJP = Optional.ofNullable(System.getenv("MAVEN_HOME"));
        if (mvnHomeJP.isPresent()) {
            invoker.setMavenHome(new File(mvnHomeJP.get()));
            invoker.setMavenExecutable(
                    new File(
                            new File(mvnHomeJP.get()).getAbsolutePath()
                                    + File.separator
                                    + "bin"
                                    + File.separator
                                    + "mvn"));
        } else {
            boolean windows =
                    (System.getProperty("os.name").toLowerCase(Locale.ROOT).equals("windows"));
            String[] detectMavenCmd =
                    windows ? new String[] {"where", "mvn"} : new String[] {"which", "mvn"};

            Process process = Runtime.getRuntime().exec(detectMavenCmd);

            BufferedReader lineReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = lineReader.readLine();
            lineReader.close();

            File mavenExecutable = new File(line);
            invoker.setMavenExecutable(mavenExecutable);
            invoker.setMavenHome(mavenExecutable.getParentFile().getParentFile());
        }
        invoker.setWorkingDirectory(folder);

        return invoker.execute(request);
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        tmpFolder.create();
    }

    @Test
    void testCertManagerCRSerializationDeserialization() throws Exception {
        // Arrange
        File crd = getCRD("cert-manager-crd.yml");
        File destFolder = tmpFolder.newFolder("cert-manager");
        prepareTmpFolder("cert-manager", destFolder);
        File javaSourcesDest = getJavaSourcesDir(destFolder);

        // Act
        runner.run(crd, javaSourcesDest);
        InvocationResult mvnExecutionResult = execMaven(destFolder, "compile", "exec:java");

        // Assert
        assertEquals(0, mvnExecutionResult.getExitCode());
    }

    @AfterAll
    public static void afterAll() {
        tmpFolder.delete();
    }
}
