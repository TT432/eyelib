package io.github.tt432.eyelib.nodegraph;

import io.github.tt432.eyelib.bridge.molang.MolangBuiltInQuery;
import io.github.tt432.eyelib.client.molang.MolangQuery;
import io.github.tt432.eyelib.molang.mapping.MolangMath;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingDiscovery;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingRegistries;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名表 {@link MolangFunctionSignatures} 与 molang 映射树的完备性契约：表内每个内置函数必须
 * 在映射树可解析。来历：2026-08-16 {@code query.entity_biome_has_any_identifier} 曾在签名表内但
 * 无实现——导入 Actions & Stuff 后节点 out 端口退化为 Any，且运行时静默求值为 null。
 */
class MolangFunctionSignaturesCompletenessTest {
    @AfterEach
    void tearDown() {
        MolangMappingRegistries.mappingTree().clear();
    }

    @Test
    void everyBuiltInSignatureResolvesInMappingTree() {
        MolangMappingTree.setupMolangMappingTree(() -> List.of(
                new MolangMappingDiscovery.MolangMappingClassEntry("math", MolangMath.class, true),
                new MolangMappingDiscovery.MolangMappingClassEntry("query", MolangBuiltInQuery.class, false),
                new MolangMappingDiscovery.MolangMappingClassEntry("query", MolangQuery.class, false)));
        MolangMappingTree tree = MolangMappingRegistries.mappingTree();
        for (String name : MolangFunctionSignatures.builtInNames()) {
            assertTrue(tree.findMethod(name) != null || tree.findField(name) != null,
                    "签名表函数未在映射树实现: " + name);
        }
    }
}
