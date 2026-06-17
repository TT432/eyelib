package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrLanguageFile(
        LinkedHashMap<String, String> entries
) {
    public static final Codec<BrLanguageFile> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(LinkedHashMap::new, m -> m)
                    .fieldOf("entries")
                    .forGetter(BrLanguageFile::entries)
    ).apply(ins, BrLanguageFile::new));

    public BrLanguageFile {
        entries = new LinkedHashMap<>(entries);
    }

    public static BrLanguageFile parse(String text) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        String[] lines = text.replace("\r", "").split("\n");
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int delimiter = line.indexOf('=');
            if (delimiter < 0) {
                continue;
            }
            entries.put(line.substring(0, delimiter), line.substring(delimiter + 1));
        }
        return new BrLanguageFile(entries);
    }

    public Map<String, String> entriesView() {
        return Map.copyOf(entries);
    }
}
