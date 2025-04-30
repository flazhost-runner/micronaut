package io.micronaut.annotation.processing

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.inject.ast.ClassElement
import io.micronaut.inject.ast.MethodElement
import io.micronaut.inject.visitor.TypeElementVisitor
import io.micronaut.inject.visitor.VisitorContext
import org.intellij.lang.annotations.Language

class NextRoundExceptionSpec extends AbstractTypeElementSpec {

    private CollectingVisitor collectingVisitor

    void "test"() {
        when:
        buildClassLoader('example.Trigger', '''
package example;

import jakarta.inject.Singleton;

@Singleton
class Trigger {}

// Parent is generated, we want to retrieve inherited annotations correctly
@Singleton
class Child implements Parent {

    @Override
    public String hello() {
        return "Hola!";
    }

}
''')
        then:
        collectingVisitor.numVisited == 1
        collectingVisitor.numMethodVisited == 1
        collectingVisitor.controllerPath == "/hello"
        collectingVisitor.getPath == "/get"
    }

    @Override
    protected Collection<TypeElementVisitor> getLocalTypeElementVisitors() {
        collectingVisitor = new CollectingVisitor()
        return List.of(new GeneratorVisitor(), collectingVisitor)
    }

    static class GeneratorVisitor implements TypeElementVisitor<Object, Object> {

        private static final @Language("java") String SOURCE = """
package example;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/hello")
public interface Parent {

    @Get("/get")
    String hello();

}
"""

        @Override
        void visitClass(ClassElement element, VisitorContext context) {
            if (element.getName() != "example.Trigger") {
                return
            }

            context.visitGeneratedSourceFile("example", "Parent", element)
                    .ifPresent(generatedFile -> {
                        try {
                            generatedFile.write(writer -> writer.write(SOURCE))
                        } catch (IOException e) {
                            throw new RuntimeException(e)
                        }
                    })
        }
    }

    static class CollectingVisitor implements TypeElementVisitor<Object, Object> {

        int numVisited = 0
        int numMethodVisited = 0
        String controllerPath
        String getPath

        @Override
        void visitClass(ClassElement element, VisitorContext context) {
            if (element.getName() != "example.Child") {
                return
            }

            ++numVisited
            controllerPath = element.stringValue(Controller).orElse(null)
        }

        @Override
        void visitMethod(MethodElement element, VisitorContext context) {
            if (element.getOwningType().getName() != "example.Child") {
                return
            }

            ++numMethodVisited
            getPath = element.stringValue(Get).orElse(null)
        }
    }

}
