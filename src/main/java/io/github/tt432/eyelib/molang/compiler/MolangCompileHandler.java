package io.github.tt432.eyelib.molang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassHierarchyResolver;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;
import io.github.tt432.eyelib.molang.MolangUncompilableException;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static io.github.dmlloyd.classfile.extras.constant.ConstantUtils.referenceClassDesc;
import static io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion.RELEASE_21;
import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.CD_MolangObject;
import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.CD_MolangScope;
import static java.lang.constant.ConstantDescs.*;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangCompileHandler {
    private static final MolangCompileVisitor visitor = new MolangCompileVisitor();
    private static int currIdx;

    public static class CompileContext {
        String compiledClassName = "";
        byte[] code = new byte[0];
    }

    public static MolangValue.MolangFunction compile(String content) {
        CompileContext compileContext = new CompileContext();
        try {
            return tryCompile(content.trim(), compileContext);
        } catch (Throwable e) {
            new File("eyelib_generatedClasses").mkdirs();
            try (var fs = new FileOutputStream("./eyelib_generatedClasses/" + compileContext.compiledClassName + ".class")) {
                fs.write(compileContext.code);
            } catch (IOException ee) {
                throw new RuntimeException(ee);
            }
            throw new MolangUncompilableException("can't compile molang: " + content + ", class name: " + compileContext.compiledClassName, e);
        }
    }

    static class MyClassLoader extends ClassLoader {
        private static final MyClassLoader INSTANCE = new MyClassLoader(MolangValue.class.getClassLoader());

        protected MyClassLoader(ClassLoader parent) {
            super(parent);
        }

        public void myDefineClass(String className, byte[] b, int off, int len) {
            defineClass(className, b, off, len);
        }
    }

    public static MolangValue.MolangFunction tryCompile(String molangString, CompileContext context) throws Throwable {
        if (molangString.isEmpty()) {
            return MolangValue.MolangFunction.NULL;
        }
        currIdx++;
        var compiledClassName = context.compiledClassName = "CompiledMolang$" + currIdx;

        var code = context.code = ClassFile.of()
                .withOptions(ClassFile.ClassHierarchyResolverOption.of(cd -> {
                    if (!cd.isClassOrInterface())
                        return null;

                    if (cd.equals(CD_Object))
                        return ClassHierarchyResolver.ClassHierarchyInfo.ofClass(null);

                    Class<?> cl = null;
                    try {
                        String result;
                        if (cd.isClassOrInterface()) {
                            String desc = cd.descriptorString();
                            result = desc.substring(1, desc.length() - 1);
                        } else {
                            throw new IllegalArgumentException(cd.descriptorString());
                        }
                        cl = Class.forName(result.replace('/', '.'), false, MolangValue.class.getClassLoader());
                    } catch (ClassNotFoundException ignored) {
                    }
                    if (cl == null) {
                        return null;
                    }

                    return cl.isInterface() ? ClassHierarchyResolver.ClassHierarchyInfo.ofInterface()
                            : ClassHierarchyResolver.ClassHierarchyInfo.ofClass(referenceClassDesc(cl.getSuperclass()));
                }))
                .build(ClassDesc.of(compiledClassName),
                        classBuilder -> classBuilder.withVersion(RELEASE_21.major(), 0)
                                .withInterfaceSymbols(ClassDesc.of(MolangValue.MolangFunction.class.getName()))
                                .withField("originalString", CD_String, fieldBuilder -> {
                                    fieldBuilder.withFlags(AccessFlag.FINAL, AccessFlag.STATIC);
                                })
                                .withMethod("<clinit>", MethodTypeDesc.of(CD_void), ClassFile.ACC_STATIC, methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                    codeBuilder.ldc(molangString).putstatic(ClassDesc.of(compiledClassName), "originalString", CD_String).return_();
                                }))
                                .withMethod("<init>", MethodTypeDesc.of(CD_void), ClassFile.ACC_PUBLIC, methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                    codeBuilder.aload(0)
                                            .invokespecial(CD_Object, "<init>", MethodTypeDesc.of(CD_void))
                                            .return_();
                                }))
                                .withMethod("apply", MethodTypeDesc.of(CD_MolangObject, CD_MolangScope), ClassFile.ACC_PUBLIC,
                                        methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                            visitor.startVisitor(codeBuilder);

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

                                            visitor.visit(molangParser.exprSet());

                                            codeBuilder.return_(TypeKind.REFERENCE);
                                        })));

        MyClassLoader.INSTANCE.myDefineClass(compiledClassName, code, 0, code.length);
        var clazz = MyClassLoader.INSTANCE.loadClass(compiledClassName);
        return (MolangValue.MolangFunction) clazz.getDeclaredConstructors()[0].newInstance();
    }
}
