/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.micronautc;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Micronaut-aware javac launcher.
 */
public final class Micronautc {

    private static final String PROCESSORS = String.join(",",
        "io.micronaut.annotation.processing.MixinVisitorProcessor",
        "io.micronaut.annotation.processing.PackageElementVisitorProcessor",
        "io.micronaut.annotation.processing.TypeElementVisitorProcessor",
        "io.micronaut.annotation.processing.AggregatingTypeElementVisitorProcessor",
        "io.micronaut.annotation.processing.BeanDefinitionInjectProcessor"
    );

    private Micronautc() {
    }

    /**
     * Main entry point.
     *
     * @param args Compiler arguments
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        var javaHome = resolveJavaHome();
        if (javaHome == null || javaHome.isBlank()) {
            System.err.println("A JDK is required. Set MICRONAUTC_JAVA_HOME or micronautc.java.home.");
            return 1;
        }
        System.setProperty("java.home", javaHome);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("Unable to locate system Java compiler (JDK required).");
            return 1;
        }

        String[] compilerArgs = prepareCompilerArgs(args);
        return compiler.run(System.in, System.out, System.err, compilerArgs);
    }

    private static String[] prepareCompilerArgs(String[] args) {
        List<String> compilerArgs = new ArrayList<>(Arrays.asList(args));
        if (!hasProcessorConfiguration(args) && !hasProcNone(args)) {
            compilerArgs.add("-processor");
            compilerArgs.add(PROCESSORS);
        }
        return compilerArgs.toArray(String[]::new);
    }

    private static boolean hasProcessorConfiguration(String[] args) {
        for (String arg : args) {
            if ("-processor".equals(arg) || arg.startsWith("-processor=")) {
                return true;
            }
            if ("--processor".equals(arg) || arg.startsWith("--processor=")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasProcNone(String[] args) {
        for (String arg : args) {
            if ("-proc:none".equals(arg) || "--proc:none".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveJavaHome() {
        String configured = System.getProperty("micronautc.java.home");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String envConfigured = System.getenv("MICRONAUTC_JAVA_HOME");
        if (envConfigured != null && !envConfigured.isBlank()) {
            return envConfigured;
        }
        String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null && !javaHomeEnv.isBlank()) {
            return javaHomeEnv;
        }
        return System.getProperty("java.home");
    }
}
