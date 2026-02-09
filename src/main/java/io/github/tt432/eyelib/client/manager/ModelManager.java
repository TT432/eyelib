package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.model.GPUParallelModel;
import io.github.tt432.eyelib.client.model.Model;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelManager extends Manager<Model> {
    public static final ModelManager INSTANCE = new ModelManager();

    public final Map<String, GPUParallelModel> gpuModel = new HashMap<>();

    @Override
    public void put(String name, Model value) {
        super.put(name, value);
        gpuModel.put(name, GPUParallelModel.from(value));
    }
}
