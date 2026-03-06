plugins {
    id("application")
    id("org.graalvm.buildtools.native")
    id("io.micronaut.build.internal.convention-library")
}

dependencies {
    implementation(projects.micronautInjectJava)

    testImplementation(projects.micronautContext)
    testImplementation(libs.jakarta.inject.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("io.micronaut.micronautc.Micronautc")
}

graalvmNative {
    toolchainDetection = false
    metadataRepository {
        enabled = true
    }
    binaries {
        named("main") {
            imageName.set("micronautc")
            mainClass.set("io.micronaut.micronautc.Micronautc")
            sharedLibrary.set(false)
            buildArgs.add("-Ob")
            buildArgs.add("--add-modules=java.compiler")
            buildArgs.add("-H:ConfigurationFileDirectories=${project.layout.projectDirectory.dir("src/main/resources/META-INF/native-image/io.micronaut/micronautc").asFile.absolutePath}")
            buildArgs.add("-H:EnableURLProtocols=jar")
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:+RuntimeClassLoading")
            buildArgs.add("-H:+AllowJRTFileSystem")
            buildArgs.add("-H:Preserve=package=javax.annotation.processing")
            buildArgs.add("-H:Preserve=package=javax.lang.model")
            buildArgs.add("-H:Preserve=package=javax.tools")
            buildArgs.add("-H:Preserve=package=java.lang")
            buildArgs.add("-H:Preserve=package=java.lang.invoke")
            buildArgs.add("-H:Preserve=package=jdk.internal.misc")
            buildArgs.add("-H:Preserve=package=jdk.internal.access")
            buildArgs.add("-H:Preserve=package=io.micronaut.inject")
            buildArgs.add("-H:Preserve=package=io.micronaut.context")
            buildArgs.add("-H:-UnlockExperimentalVMOptions")
            buildArgs.add("--initialize-at-build-time=com.sun.tools.javac.api.JavacTool")
            buildArgs.add("--initialize-at-build-time=io.micronaut.sourcegen.model,org.objectweb.asm")
            buildArgs.add("--initialize-at-run-time=jdk.internal.loader.ClassLoaders")
            buildArgs.add("--initialize-at-run-time=io.micronaut.annotation.processing.TypeElementVisitorProcessor")
            buildArgs.add("--initialize-at-run-time=io.micronaut.annotation.processing.AggregatingTypeElementVisitorProcessor")
            buildArgs.add("--initialize-at-run-time=io.micronaut.annotation.processing.PackageElementVisitorProcessor")
            buildArgs.add("--initialize-at-run-time=com.sun.tools.javac")
            buildArgs.add("--initialize-at-run-time=com.sun.tools.javac.file")
            buildArgs.add("--initialize-at-run-time=com.sun.source")
            buildArgs.add("--initialize-at-run-time=jdk.javadoc.internal")
            buildArgs.add("--initialize-at-run-time=com.sun.tools.doclint")
            buildArgs.add("--initialize-at-run-time=jdk.internal.jshell.tool")
            buildArgs.add("--initialize-at-run-time=jdk.internal.org.jline.terminal.impl.ffm")
            buildArgs.add("--initialize-at-run-time=io.micronaut.inject.annotation.AbstractAnnotationMetadataBuilder")
            resources.autodetect()
        }
    }
}

tasks.named<Test>("test") {
    dependsOn(tasks.named("nativeCompile"))
    systemProperty("micronautc.executable", layout.buildDirectory.file("native/nativeCompile/micronautc").get().asFile.absolutePath)
    systemProperty("micronautc.compile.classpath", configurations.testRuntimeClasspath.get().asPath)
}
