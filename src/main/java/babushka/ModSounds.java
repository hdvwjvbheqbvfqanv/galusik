package babushka;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Восемь реплик бабушки — по одному звуковому событию на каждый ваш файл. */
public final class ModSounds {
    private static final String[] LINE_NAMES = {
        "vy_vzroslye_deti",                          // Вы взрослые дети
        "vy_zachem_mne_otklyuchaete_svet",           // Вы зачем мне отключаете свет
        "vyzovem_politsiyu_zlykh_dukhov_perelovim",  // Вызовем полицию, всех злых духов переловим
        "zachem_vy_khuliganite",                     // Зачем вы хулиганите
        "ne_vyklyuchayte",                           // Не выключайте
        "ne_khuliganyte",                            // Не хулиганьте
        "ya_vam_shchas_politsiyu_vyzovu",            // Я вам щас полицию вызову
        "ya_shchas_tvoyu_mat_vyzovu"                 // Я щас твою мать вызову
    };

    /** Длительность каждой реплики в тиках (секунды * 20, с округлением вверх) — в том же порядке. */
    public static final int[] LINE_TICKS = {
        187,  // 9.33 c
        65,   // 3.24 c
        67,   // 3.32 c
        43,   // 2.12 c
        146,  // 7.26 c
        79,   // 3.92 c
        97,   // 4.83 c
        89    // 4.41 c
    };

    public static final List<SoundEvent> LINES = new ArrayList<>();

    private ModSounds() {}

    public static void register() {
        for (String name : LINE_NAMES) {
            Identifier id = BabushkaMod.id("line." + name);
            LINES.add(Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id)));
        }
    }
}
