package io.github.tt432.eyelib.common.bedrock.model.pojo;

import io.github.tt432.eyelib.util.json.JsonUtils;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Import this package:
 *     import software.bernie.geckolib.file.geo.Converter;
 * Then you can deserialize a JSON string with
 *     GeoModel data = Converter.fromJsonString(jsonString);
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

	private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendOptional(DateTimeFormatter.ISO_TIME).appendOptional(DateTimeFormatter.ISO_OFFSET_TIME)
			.parseDefaulting(ChronoField.YEAR, 2020).parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
			.parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter().withZone(ZoneOffset.UTC);

	public static OffsetTime parseTimeString(String str) {
		return ZonedDateTime.from(Converter.TIME_FORMATTER.parse(str)).toOffsetDateTime().toOffsetTime();
	}

	public static RawGeoModel fromJsonString(String json) {
		return JsonUtils.normal.fromJson(json, RawGeoModel.class);
	}

	public static String toJsonString(RawGeoModel obj) {
		return JsonUtils.pretty.toJson(obj);
	}
}
