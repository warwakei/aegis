package fun.aegis.utils.client.sound;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


import fun.aegis.utils.display.interfaces.QuickImports;

@Setter
@Getter
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SoundManager implements QuickImports {
    public SoundEvent OPEN_GUI = SoundEvent.of(Identifier.of("minecraft", "gui_open"));
    public SoundEvent CLOSE_GUI = SoundEvent.of(Identifier.of("minecraft", "gui_close"));
    public SoundEvent ENABLE_MODULE = SoundEvent.of(Identifier.of("minecraft", "module_enable"));
    public SoundEvent DISABLE_MODULE = SoundEvent.of(Identifier.of("minecraft", "module_disable"));
    public SoundEvent CATEGORY_CLICK = SoundEvent.of(Identifier.of("minecraft", "guicategory_select"));
    public SoundEvent ORTHODOX = SoundEvent.of(Identifier.of("minecraft", "kolokolnia_kill"));
    public SoundEvent KILL1 = SoundEvent.of(Identifier.of("minecraft", "kill1"));
    public SoundEvent KILL2 = SoundEvent.of(Identifier.of("minecraft", "kill2"));
    public SoundEvent KILL3 = SoundEvent.of(Identifier.of("minecraft", "kill3"));
    public SoundEvent KILL4 = SoundEvent.of(Identifier.of("minecraft", "kill4"));
    public SoundEvent KILL5 = SoundEvent.of(Identifier.of("minecraft", "kill5"));
    public SoundEvent KILL6 = SoundEvent.of(Identifier.of("minecraft", "kill6"));
    public SoundEvent LOADED = SoundEvent.of(Identifier.of("minecraft", "loaded"));
    public SoundEvent SAVED = SoundEvent.of(Identifier.of("minecraft", "saved"));
    public SoundEvent SHUTDOWN = SoundEvent.of(Identifier.of("minecraft", "shutdown"));


    public void init() {
        Registry.register(Registries.SOUND_EVENT, OPEN_GUI.id(), OPEN_GUI);
        Registry.register(Registries.SOUND_EVENT, CLOSE_GUI.id(), CLOSE_GUI);
        Registry.register(Registries.SOUND_EVENT, ENABLE_MODULE.id(), ENABLE_MODULE);
        Registry.register(Registries.SOUND_EVENT, DISABLE_MODULE.id(), DISABLE_MODULE);
        Registry.register(Registries.SOUND_EVENT, CATEGORY_CLICK.id(), CATEGORY_CLICK);
        Registry.register(Registries.SOUND_EVENT, ORTHODOX.id(), ORTHODOX);
        Registry.register(Registries.SOUND_EVENT, KILL1.id(), KILL1);
        Registry.register(Registries.SOUND_EVENT, KILL2.id(), KILL2);
        Registry.register(Registries.SOUND_EVENT, KILL3.id(), KILL3);
        Registry.register(Registries.SOUND_EVENT, KILL4.id(), KILL4);
        Registry.register(Registries.SOUND_EVENT, KILL5.id(), KILL5);
        Registry.register(Registries.SOUND_EVENT, KILL6.id(), KILL6);
        Registry.register(Registries.SOUND_EVENT, LOADED.id(), LOADED);
        Registry.register(Registries.SOUND_EVENT, SAVED.id(), SAVED);
        Registry.register(Registries.SOUND_EVENT, SHUTDOWN.id(), SHUTDOWN);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!PlayerInteractionHelper.nullCheck()) {
            mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, volume, pitch);
        }
    }
}
