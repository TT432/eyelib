package io.github.tt432.eyelibimporter.addon;

import java.util.LinkedHashMap;
import java.util.Map;

public record BrLanguageFile(
        LinkedHashMap<String, String> entries
) {
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
