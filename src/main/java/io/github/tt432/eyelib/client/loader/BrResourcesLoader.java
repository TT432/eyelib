package io.github.tt432.eyelib.client.loader;

import com.google.gson.GsonBuilder;
import io.github.tt432.eyelib.bridge.client.loader.SimpleJsonWithSuffixResourceReloadListener;
/**
 * @author TT432
 */
public abstract class BrResourcesLoader extends SimpleJsonWithSuffixResourceReloadListener {
    public BrResourcesLoader(String directory, String suffix) {
        super(new GsonBuilder().setLenient().create(), "eyelib/" + directory, suffix);
    }
}