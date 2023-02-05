package io.github.tt432.eyelib.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import io.github.tt432.eyelib.util.math.molang.math.IValue;
import software.bernie.geckolib3.geo.raw.pojo.Converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author DustW
 **/
public class JsonUtils {
    public static final Gson normal = builder().create();
    public static final Gson pretty = builder().setPrettyPrinting().create();

    private static GsonBuilder builder() {
        return new GsonBuilder()
                // 关闭 html 转义
                .disableHtmlEscaping()
                .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<?>) (json, typeOfT, context) ->
                        Converter.parseDateTimeString(json.getAsString()))
                .registerTypeHierarchyAdapter(IValue.class, new IValue.Serializer());
    }

    public static byte[] compress(String string) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try(GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(string.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public static String uncompress(byte[] string) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(string);

        try(GZIPInputStream gzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[256];
            int index;

            while ((index = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, index);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }
}
