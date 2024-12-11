package io.github.tt432.eyelib.client.loader;

import com.google.gson.GsonBuilder;

/**
 * @author TT432
 */
public abstract class BrResourcesLoader extends SimpleJsonWithSuffixResourceReloadListener {
    public BrResourcesLoader(String directory, String suffix) {
        super(new GsonBuilder().setLenient().create(), "eyelib/" + directory, suffix);
    }
}
