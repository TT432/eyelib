package io.github.tt432.eyelib.debug.benchmark;

import net.minecraft.client.Minecraft;

/** Runtime resources and behavior for one benchmark scenario. */
interface BenchmarkWorkload extends AutoCloseable {
    void prepare(Minecraft minecraft, BenchmarkScenario scenario) throws Exception;

    boolean isReady(Minecraft minecraft);

    void maintain(Minecraft minecraft);

    void render(Minecraft minecraft) throws Exception;

    int actualEntityCount(Minecraft minecraft);

    @Override
    void close() throws Exception;
}
