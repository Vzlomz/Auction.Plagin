package com.yourserver.itemauction;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Список предметов, которые реально можно получить в выживании, разбитый на разделы
 * (как вкладки в творческом инвентаре). Исключены технические/креативные блоки
 * (бедрок, командные блоки и т.п.), яйца призыва мобов, заражённые блоки
 * и предметы со сложными NBT-данными, которые нельзя корректно сравнить.
 *
 * Если что-то важное пропущено, попало не в тот раздел, или наоборот не должно
 * быть в списке - отредактируй BLACKLIST/наборы ключевых слов ниже и пересобери плагин.
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

    private static final Set<String> ARMOR_EXACT = Set.of(
            "SHIELD", "ELYTRA", "LEATHER_HORSE_ARMOR", "IRON_HORSE_ARMOR",
            "GOLDEN_HORSE_ARMOR", "DIAMOND_HORSE_ARMOR"
    );

    private static final Set<String> TOOLS_WEAPONS_EXACT = Set.of(
            "BOW", "CROSSBOW", "TRIDENT", "MACE", "FISHING_ROD", "FLINT_AND_STEEL",
            "SHEARS", "ARROW", "SPECTRAL_ARROW", "SPYGLASS", "BRUSH", "CARROT_ON_A_STICK",
            "WARPED_FUNGUS_ON_A_STICK"
    );

    private static final Set<String> FOOD_EXACT = Set.of(
            "APPLE", "BAKED_POTATO", "BEEF", "BEETROOT", "BEETROOT_SOUP", "BREAD", "CAKE",
            "CARROT", "CHICKEN", "CHORUS_FRUIT", "COD", "COOKED_BEEF", "COOKED_CHICKEN",
            "COOKED_COD", "COOKED_MUTTON", "COOKED_PORKCHOP", "COOKED_RABBIT", "COOKED_SALMON",
            "COOKIE", "DRIED_KELP", "ENCHANTED_GOLDEN_APPLE", "GOLDEN_APPLE", "GOLDEN_CARROT",
            "HONEY_BOTTLE", "MELON_SLICE", "MUSHROOM_STEW", "MUTTON", "POISONOUS_POTATO",
            "PORKCHOP", "POTATO", "PUFFERFISH", "PUMPKIN_PIE", "RABBIT", "RABBIT_STEW",
            "ROTTEN_FLESH", "SALMON", "SPIDER_EYE", "SWEET_BERRIES", "GLOW_BERRIES",
            "TROPICAL_FISH", "MILK_BUCKET"
    );

    public enum Category {
        ALL("Все предметы", Material.CHEST),
        BLOCKS("Блоки", Material.BRICKS),
        TOOLS_WEAPONS("Инструменты и оружие", Material.DIAMOND_SWORD),
        ARMOR("Броня", Material.DIAMOND_CHESTPLATE),
        FOOD("Еда", Material.COOKED_BEEF),
        MISC("Ресурсы и разное", Material.DIAMOND);

        private final String displayName;
        private final Material icon;

        Category(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private static final Predicate<Material> IS_ARMOR = m ->
            ARMOR_EXACT.contains(m.name())
                    || m.name().endsWith("_HELMET") || m.name().endsWith("_CHESTPLATE")
                    || m.name().endsWith("_LEGGINGS") || m.name().endsWith("_BOOTS");

    private static final Predicate<Material> IS_TOOL_WEAPON = m ->
            TOOLS_WEAPONS_EXACT.contains(m.name())
                    || m.name().endsWith("_SWORD") || m.name().endsWith("_PICKAXE")
                    || m.name().endsWith("_AXE") || m.name().endsWith("_SHOVEL")
                    || m.name().endsWith("_HOE");

    private static final Predicate<Material> IS_FOOD = m -> FOOD_EXACT.contains(m.name());

    private static final List<Material> ALL_ITEMS = build();
    private static final Map<Category, List<Material>> BY_CATEGORY = buildCategories();

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

    private static Map<Category, List<Material>> buildCategories() {
        Map<Category, List<Material>> map = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            map.put(c, new ArrayList<>());
        }
        for (Material m : ALL_ITEMS) {
            if (IS_ARMOR.test(m)) {
                map.get(Category.ARMOR).add(m);
            } else if (IS_TOOL_WEAPON.test(m)) {
                map.get(Category.TOOLS_WEAPONS).add(m);
            } else if (IS_FOOD.test(m)) {
                map.get(Category.FOOD).add(m);
            } else if (m.isBlock()) {
                map.get(Category.BLOCKS).add(m);
            } else {
                map.get(Category.MISC).add(m);
            }
        }
        return map;
    }

    public static List<Material> all() {
        return ALL_ITEMS;
    }

    public static List<Material> inCategory(Category category) {
        if (category == Category.ALL) return ALL_ITEMS;
        return BY_CATEGORY.get(category);
    }

    /**
     * Возвращает список зачарований, применимых к данному предмету (например меч -> Острота, Прочность...).
     * Использует официальную проверку Enchantment#canEnchantItem, а не жёстко прописанные списки.
     */
    public static List<org.bukkit.enchantments.Enchantment> applicableEnchantments(Material material) {
        org.bukkit.inventory.ItemStack probe = new org.bukkit.inventory.ItemStack(material);
        List<org.bukkit.enchantments.Enchantment> result = new ArrayList<>();
        for (org.bukkit.enchantments.Enchantment enchantment : org.bukkit.Registry.ENCHANTMENT) {
            if (enchantment.canEnchantItem(probe)) {
                result.add(enchantment);
            }
        }
        result.sort(Comparator.comparing(e -> e.getKey().getKey()));
        return result;
    }
}
