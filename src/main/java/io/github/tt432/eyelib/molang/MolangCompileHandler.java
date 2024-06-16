package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangCompileHandler {
    private static final MolangCompileVisitor visitor = new MolangCompileVisitor();
    private static int currIdx;

    public static MethodHandle compile(MolangValue value) {
        try {
            return tryCompile(value);
        } catch (IllegalAccessException | ClassNotFoundException | CompileException e) {
            throw new MolangUncompilableException(e);
        }
    }

    public static MethodHandle tryCompile(MolangValue value) throws IllegalAccessException, ClassNotFoundException, CompileException {
        currIdx++;
        String classname = "CompiledMolang$" + currIdx;

        String molangString = value.getContext().trim();
        String body;

        if (molangString.isEmpty()) {
            body = "return 0F;";
        } else {
            MolangParser molangParser = new MolangParser(
                    new CommonTokenStream(
                            new MolangLexer(CharStreams.fromString(molangString)))
            );
            molangParser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    log.error("parsing: {} with error:{}", molangString, e.getMessage());
                }
            });
            body = visitor.visitExprSet(molangParser.exprSet());
        }

        String sourceCode = """
                public final class %s {
                    public static float eval(%s $1) {
                        %s
                    }
                }
                """.formatted(classname, MolangScope.class.getName(), body);

        SimpleCompiler simpleCompiler = new SimpleCompiler();
        simpleCompiler.setParentClassLoader(MolangScope.class.getClassLoader());
        simpleCompiler.cook(sourceCode);
        var clazz = simpleCompiler.getClassLoader().loadClass(classname);
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        return publicLookup.unreflect(clazz.getMethods()[0]);
    }
}
