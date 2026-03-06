package io.micronaut.micronautc;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicronautcNativeTest {

    @Test
    void nativeMicronautcCompilesAndProducesInjectableBeans(@TempDir Path tempDir) throws Exception {
        String executable = System.getProperty("micronautc.executable");
        String compileClasspath = System.getProperty("micronautc.compile.classpath");
        assertTrue(executable != null && !executable.isBlank(), "micronautc.executable must be set");
        assertTrue(compileClasspath != null && !compileClasspath.isBlank(), "micronautc.compile.classpath must be set");

        Path sourcesDir = tempDir.resolve("src").resolve("example");
        Path probeClassesDir = tempDir.resolve("probe-classes");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(sourcesDir);
        Files.createDirectories(probeClassesDir);
        Files.createDirectories(classesDir);

        Path greeterSource = sourcesDir.resolve("Greeter.java");
        Path helloServiceSource = sourcesDir.resolve("HelloService.java");
        Path customVisitorJar = createCustomVisitorJar(tempDir, compileClasspath);

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

        List<String> probeCommand = new ArrayList<>();
        probeCommand.add(executable);
        probeCommand.add("-d");
        probeCommand.add(probeClassesDir.toString());
        probeCommand.add("-processorpath");
        probeCommand.add(customVisitorJar.toString());
        probeCommand.add("-classpath");
        probeCommand.add(compileClasspath);
        probeCommand.add(greeterSource.toString());
        probeCommand.add(helloServiceSource.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(probeCommand);
        processBuilder.environment().put("MICRONAUTC_JAVA_HOME", System.getProperty("java.home"));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String probeOutput = readOutput(process.getInputStream());
        int probeExitCode = process.waitFor();
        assertNotEquals(0, probeExitCode, () -> "custom visitor probe should fail compilation:\n" + probeOutput);
        assertTrue(probeOutput.contains("CUSTOM_VISITOR_RAN"), () -> "custom visitor did not run:\n" + probeOutput);

        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-d");
        command.add(classesDir.toString());
        command.add("-classpath");
        command.add(compileClasspath);
        command.add(greeterSource.toString());
        command.add(helloServiceSource.toString());

        processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("MICRONAUTC_JAVA_HOME", System.getProperty("java.home"));
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
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

    private static Path createCustomVisitorJar(Path tempDir, String compileClasspath) throws IOException {
        Path visitorSourcesDir = tempDir.resolve("visitor-src").resolve("custom").resolve("visitor");
        Path visitorClassesDir = tempDir.resolve("visitor-classes");
        Path visitorSource = visitorSourcesDir.resolve("MarkerVisitor.java");

        Files.createDirectories(visitorSourcesDir);
        Files.createDirectories(visitorClassesDir);
        Files.writeString(visitorSource, """
            package custom.visitor;

            import io.micronaut.inject.ast.ClassElement;
            import io.micronaut.inject.visitor.TypeElementVisitor;
            import io.micronaut.inject.visitor.VisitorContext;

            public final class MarkerVisitor implements TypeElementVisitor<Object, Object> {
                @Override
                public VisitorKind getVisitorKind() {
                    return VisitorKind.ISOLATING;
                }

                @Override
                public void visitClass(ClassElement element, VisitorContext context) {
                    if (!\"example.HelloService\".equals(element.getName())) {
                        return;
                    }
                    context.fail("CUSTOM_VISITOR_RAN", element);
                }
            }
            """);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "System Java compiler not available for test visitor compilation");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(visitorSource.toFile());
            List<String> options = List.of(
                "-classpath", compileClasspath,
                "-d", visitorClassesDir.toString()
            );
            Boolean compiled = compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();
            assertTrue(Boolean.TRUE.equals(compiled), "Failed to compile custom type element visitor");
        }

        Path serviceFile = visitorClassesDir.resolve("META-INF/services/io.micronaut.inject.visitor.TypeElementVisitor");
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, "custom.visitor.MarkerVisitor\n");

        Path visitorJar = tempDir.resolve("custom-visitor.jar");
        createJar(visitorClassesDir, visitorJar);
        return visitorJar;
    }

    private static void createJar(Path inputDir, Path jarFile) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile));
             Stream<Path> paths = Files.walk(inputDir).sorted(Comparator.naturalOrder())) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                JarEntry entry = new JarEntry(inputDir.relativize(path).toString().replace('\\', '/'));
                try {
                    jarOutputStream.putNextEntry(entry);
                    jarOutputStream.write(Files.readAllBytes(path));
                    jarOutputStream.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
