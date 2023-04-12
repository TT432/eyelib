package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.JsonSyntaxException;
import io.github.tt432.eyelib.util.json.JsonUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Import this package:
 * import software.bernie.geckolib.file.geo.Converter;
 * Then you can deserialize a JSON string with
 * GeoModel data = Converter.fromJsonString(jsonString);
 */
public class Converter {
    // Date-time helpers

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_DATE_TIME).appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_INSTANT)
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX"))
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toFormatter().withZone(ZoneOffset.UTC);

    public static OffsetDateTime parseDateTimeString(String str) {
        return ZonedDateTime.from(Converter.DATE_TIME_FORMATTER.parse(str)).toOffsetDateTime();
    }

    public static RawGeoModel fromJsonString(String fileName, String json) {
        try {
            return JsonUtils.normal.fromJson(json, RawGeoModel.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Filed to load json: " + fileName);
        }
    }
}
