package io.github.tt432.eyelib.client.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoaderParsingOpsTest {
    @Test
    void parseBySourceKeyKeepsSourceKeyForSuccessfulEntries() {
        String key = "eyelib:ok";
        Map<String, JsonElement> source = Map.of(
                key,
                JsonParser.parseString("\"value\"")
        );

        Map<String, String> parsed = LoaderParsingOps.parseBySourceKey(
                source,
                Codec.STRING,
                LoggerFactory.getLogger(LoaderParsingOpsTest.class),
                "string"
        );

        assertEquals(Map.of(key, "value"), parsed);
    }

    @Test
    void parseAndTranslateUsesTranslatedDomainKeyAndSkipsInvalidInput() {
        Map<String, JsonElement> source = new LinkedHashMap<>();
        source.put("eyelib:valid", JsonParser.parseString("""
                {"identifier":"domain:translated","weight":7}
                """));
        source.put("eyelib:invalid", JsonParser.parseString("""
                {"identifier":"domain:broken"}
                """));

        Map<String, ParsedEntry> parsed = LoaderParsingOps.parseAndTranslate(
                source,
                ParsedEntry.CODEC,
                (resourceLocation, entry) -> entry.identifier(),
                LoggerFactory.getLogger(LoaderParsingOpsTest.class),
                "entry"
        );

        assertEquals(1, parsed.size());
        assertTrue(parsed.containsKey("domain:translated"));
        assertEquals(7, parsed.get("domain:translated").weight());
        assertFalse(parsed.containsKey("domain:broken"));
    }

    private record ParsedEntry(String identifier, int weight) {
        private static final Codec<ParsedEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("identifier").forGetter(ParsedEntry::identifier),
                Codec.INT.fieldOf("weight").forGetter(ParsedEntry::weight)
        ).apply(instance, ParsedEntry::new));
    }
}
