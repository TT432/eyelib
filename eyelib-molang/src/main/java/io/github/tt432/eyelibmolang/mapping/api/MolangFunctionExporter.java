package io.github.tt432.eyelibmolang.mapping.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Molang 函数导出工具，将注册的函数写入 CSV 文件。
 *
 * @author TT432
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangFunctionExporter {
    public static void export() {
        try (var fw = new FileWriter("./exportedMolang.csv")) {
            visitNode(MolangMappingTree.INSTANCE.toplevelNode, fw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void visitNode(MolangMappingTree.Node node, FileWriter fw) {
        node.actualFunctions.forEach((k, v) -> {
            for (MolangMappingTree.FunctionInfo functionInfo : v) {
                MolangFunction molangFunction = functionInfo.molangFunction();
                if (molangFunction != null) {
                    try {
                        fw.write(molangFunction.value() + "," + String.join(" ", molangFunction.alias()) + "," + molangFunction.description() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        fw.write(functionInfo.method().getName() + ", ,\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        node.children.values().forEach(c -> visitNode(c, fw));
    }
}