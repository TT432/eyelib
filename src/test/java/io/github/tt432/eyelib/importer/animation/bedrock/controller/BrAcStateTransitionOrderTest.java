package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 动画控制器 transitions 的键序 = 求值优先级（先匹配先转移）。
 * 列表形式（BE 惯例：单键对象列表）合并时必须保序——曾用 Object2ObjectOpenHashMap
 * 导致键序被打散，多条件同时成立时转移目标不确定。
 */
class BrAcStateTransitionOrderTest {

    @Test
    void listFormTransitionsPreserveJsonOrder() {
        String json = """
                {"transitions":[{"extra_1":"v.a==1"},{"extra_2":"v.a==2"},{"extra_3":"v.a==3"},
                                {"extra_4":"v.a==4"},{"extra_5":"v.a==5"},{"gift":"v.g>0"},{"talking":"v.t>0"}]}
                """;
        BrAcState state = BrAcState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .result().orElseThrow();
        assertEquals(List.of("extra_1", "extra_2", "extra_3", "extra_4", "extra_5", "gift", "talking"),
                List.copyOf(state.transitions().keySet()));
    }

    @Test
    void singleMapFormTransitionsPreserveJsonOrder() {
        String json = """
                {"transitions":{"extra_1":"v.a==1","extra_2":"v.a==2","extra_3":"v.a==3","gift":"v.g>0","talking":"v.t>0"}}
                """;
        BrAcState state = BrAcState.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .result().orElseThrow();
        assertEquals(List.of("extra_1", "extra_2", "extra_3", "gift", "talking"),
                List.copyOf(state.transitions().keySet()));
    }
}
