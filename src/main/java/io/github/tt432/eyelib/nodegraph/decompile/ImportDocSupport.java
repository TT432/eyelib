package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import java.util.Map;

/** 三类文档导入共用的后处理助手：animate 条目/短名解析与标识符回填。 */
final class ImportDocSupport {
    private ImportDocSupport() {
    }

    /**
     * animate / ac.state animations 条目：纯字符串（weight 缺省 1）或 {short_name: weight}。
     * 短名解析：animations 声明表 → ref.animation；animation_controllers 声明表 → ref.ac；
     * 未声明 → 补仅 short_name 的 ref.animation（warnIfUnresolved 时 + UNKNOWN_REFERENCE；
     * 不连线声明端口，规格 §2.5）。
     */
    static void importAnimateEntry(ImportGraphBuilder b, String ownerUid, String slotPort,
                                   JsonElement entry, Map<String, String> animationRefs,
                                   Map<String, String> acRefs, boolean warnIfUnresolved) {
        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            String entryUid = b.addNode("ae", NodeTypes.ANIMATE_ENTRY.id(), Map.of());
            b.wire(resolveAnimationRef(b, entry.getAsString(), animationRefs, acRefs, warnIfUnresolved),
                    "ref", entryUid, "ref");
            b.wire(entryUid, "entry", ownerUid, slotPort);
        } else if (entry.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : entry.getAsJsonObject().entrySet()) {
                String entryUid = b.addNode("ae", NodeTypes.ANIMATE_ENTRY.id(), Map.of());
                b.wire(resolveAnimationRef(b, e.getKey(), animationRefs, acRefs, warnIfUnresolved),
                        "ref", entryUid, "ref");
                ImportValueOps.valueSlot(b, entryUid, "weight", "animate." + e.getKey(), e.getValue());
                b.wire(entryUid, "entry", ownerUid, slotPort);
            }
        } else {
            ImportValueOps.invalidField(b, "animate[]", entry);
        }
    }

    static String resolveAnimationRef(ImportGraphBuilder b, String shortName,
                                      Map<String, String> animationRefs,
                                      Map<String, String> acRefs, boolean warnIfUnresolved) {
        String uid = animationRefs.get(shortName);
        if (uid != null) {
            return uid;
        }
        uid = acRefs.get(shortName);
        if (uid != null) {
            return uid;
        }
        uid = b.addNode("ra", NodeTypes.REF_ANIMATION.id(),
                ImportGraphBuilder.opts("short_name", shortName));
        animationRefs.put(shortName, uid);
        if (warnIfUnresolved) {
            b.warn(DecompileDiagnostics.UNKNOWN_REFERENCE,
                    "animate 短名 '" + shortName + "' 不在声明表中，已补独立 ref.animation");
        }
        return uid;
    }

    /** 应用标识符补全并重组 ImportResult（表空时原样返回）。 */
    static ImportResult backfilled(ImportResult result, ShortNameOps.KnownTables known) {
        var rewrite = ShortNameOps.backfillIdentifiers(result.library(), known);
        return rewrite.changed() == 0 ? result : new ImportResult(rewrite.library(), result.diagnostics());
    }
}
