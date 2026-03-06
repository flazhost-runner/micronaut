package io.micronaut.micronautc;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicronautcNativeTest {

    @Test
    void nativeMicronautcCompilesAndProducesInjectableBeans(@TempDir Path tempDir) throws Exception {
        String executable = System.getProperty("micronautc.executable");
        String compileClasspath = System.getProperty("micronautc.compile.classpath");
        assertTrue(executable != null && !executable.isBlank(), "micronautc.executable must be set");
        assertTrue(compileClasspath != null && !compileClasspath.isBlank(), "micronautc.compile.classpath must be set");

        Path sourcesDir = tempDir.resolve("src").resolve("example");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(sourcesDir);
        Files.createDirectories(classesDir);

        Path greeterSource = sourcesDir.resolve("Greeter.java");
        Path helloServiceSource = sourcesDir.resolve("HelloService.java");

        Files.writeString(greeterSource, """
            package example;

            import jakarta.inject.Singleton;

            @Singleton
            public class Greeter {
                public String greet() {
                    return \"hello\";
                }
            }
            """);

        Files.writeString(helloServiceSource, """
            package example;

            import jakarta.inject.Singleton;

            @Singleton
            public class HelloService {
                private final Greeter greeter;

                public HelloService(Greeter greeter) {
                    this.greeter = greeter;
                }

                public String message() {
                    return greeter.greet();
                }
            }
            """);

        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-d");
        command.add(classesDir.toString());
        command.add("-classpath");
        command.add(compileClasspath);
        command.add(greeterSource.toString());
        command.add(helloServiceSource.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("MICRONAUTC_JAVA_HOME", System.getProperty("java.home"));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = readOutput(process.getInputStream());
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, () -> "micronautc failed:\n" + output);

        Path beanRefPath = classesDir.resolve("META-INF/micronaut/io.micronaut.inject.BeanDefinitionReference");
        assertTrue(Files.isDirectory(beanRefPath), "BeanDefinitionReference metadata directory must exist");
        try (Stream<Path> files = Files.list(beanRefPath)) {
            assertFalse(files.findAny().isEmpty(), "BeanDefinitionReference metadata must be generated");
        }

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, getClass().getClassLoader());
             ApplicationContext applicationContext = ApplicationContext.builder().classLoader(classLoader).build().start()) {
            Class<?> helloServiceClass = classLoader.loadClass("example.HelloService");
            Object helloService = applicationContext.getBean(helloServiceClass);
            Method messageMethod = helloServiceClass.getMethod("message");
            assertEquals("hello", messageMethod.invoke(helloService));
        }
    }

    private static String readOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
