package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.InvocationTargetException;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangCompileHandler {
    private static final MolangCompileVisitor visitor = new MolangCompileVisitor();
    private static int currIdx;

    public static MolangValue.MolangFunction compile(String content) {
        try {
            return tryCompile(content.trim());
        } catch (IllegalAccessException | ClassNotFoundException | CompileException | InvocationTargetException |
                 InstantiationException e) {
            throw new MolangUncompilableException(e);
        }
    }

    public static MolangValue.MolangFunction tryCompile(String molangString) throws IllegalAccessException, ClassNotFoundException, CompileException, InvocationTargetException, InstantiationException {
        currIdx++;
        String classname = "CompiledMolang$" + currIdx;

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
                public final class %s implements %s {
                    public float apply(%s $1) {
                        %s
                    }
                }
                """.formatted(classname, MolangValue.MolangFunction.class.getName(), MolangScope.class.getName(), body);

        SimpleCompiler simpleCompiler = new SimpleCompiler();
        simpleCompiler.setParentClassLoader(MolangScope.class.getClassLoader());
        simpleCompiler.cook(sourceCode);
        var clazz = simpleCompiler.getClassLoader().loadClass(classname);
        return (MolangValue.MolangFunction) clazz.getDeclaredConstructors()[0].newInstance();
    }
}
