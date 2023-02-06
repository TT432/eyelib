package io.github.tt432.eyelib.example.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import io.github.tt432.eyelib.Eyelib;

public class SoundRegistry {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
			Eyelib.MOD_ID);

	public static RegistryObject<SoundEvent> JACK_MUSIC = SOUNDS.register("jack_music",
			() -> new SoundEvent(new ResourceLocation(Eyelib.MOD_ID, "jack_music")));
}
