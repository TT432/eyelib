package io.github.tt432.eyelib.nodegraph.migration;

import com.google.gson.JsonElement;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import java.util.Set;

/** 迁移共用的节点选项/uid 小工具。 */
final class MigrationSupport {
    private MigrationSupport() {
    }

    static String optionString(NodeInstance node, String id, String fallback) {
        JsonElement value = node.options().get(id);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    static String stripRoot(String name, String root) {
        String prefix = root + ".";
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    static String freshUid(Set<String> taken, int[] counter) {
        String uid;
        do {
            uid = "mig" + counter[0]++;
        } while (taken.contains(uid));
        taken.add(uid);
        return uid;
    }
}
