package com.yourserver.itemauction;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Список предметов, которые реально можно получить в выживании.
 * Исключены технические/креативные блоки (бедрок, командные блоки и т.п.),
 * яйца призыва мобов, заражённые блоки и предметы со сложными
 * NBT-данными, которые нельзя корректно сравнить (фейерверки, карты и т.п.).
 *
 * Если что-то важное пропущено или наоборот не должно быть в списке -
 * просто отредактируй BLACKLIST ниже и пересобери плагин.
 */
public final class MaterialCatalog {

    private static final Set<Material> BLACKLIST = Set.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.LIGHT,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK,
            Material.END_PORTAL_FRAME,
            Material.REINFORCED_DEEPSLATE,
            Material.PETRIFIED_OAK_SLAB,
            Material.END_GATEWAY,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.MOVING_PISTON,
            Material.PISTON_HEAD,
            Material.FIREWORK_ROCKET,
            Material.FIREWORK_STAR,
            Material.TIPPED_ARROW,
            Material.FILLED_MAP,
            Material.SUSPICIOUS_STEW
    );

    private static final List<Material> CACHE = build();

    private MaterialCatalog() {
    }

    private static List<Material> build() {
        List<Material> list = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isItem()) continue;
            if (m.isLegacy()) continue;
            if (m.isAir()) continue;
            if (BLACKLIST.contains(m)) continue;
            String name = m.name();
            if (name.endsWith("_SPAWN_EGG")) continue;
            if (name.startsWith("INFESTED_")) continue;
            if (name.startsWith("LEGACY_")) continue;
            list.add(m);
        }
        list.sort(Comparator.comparing(Material::name));
        return list;
    }

    public static List<Material> all() {
        return CACHE;
    }

    public static List<Material> search(String query) {
        if (query == null || query.isBlank()) return CACHE;
        String q = query.toLowerCase();
        List<Material> result = new ArrayList<>();
        for (Material m : CACHE) {
            if (m.name().toLowerCase().contains(q)) {
                result.add(m);
            }
        }
        return result;
    }
}
