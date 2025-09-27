package com.example.quickstocks.application.boot;

/**
 * Mock Material enum for development purposes.
 * This represents a subset of Minecraft materials for testing the ItemSeeder.
 * In production, this would be replaced with org.bukkit.Material.
 */
public enum MockMaterial {
    // Common blocks
    STONE(true),
    DIRT(true),
    GRASS_BLOCK(true),
    COBBLESTONE(true),
    SAND(true),
    GRAVEL(true),
    OAK_LOG(true),
    OAK_PLANKS(true),
    
    // Ores and minerals
    COAL_ORE(true),
    IRON_ORE(true),
    GOLD_ORE(true),
    DIAMOND_ORE(true),
    EMERALD_ORE(true),
    REDSTONE_ORE(true),
    
    // Items
    COAL(true),
    IRON_INGOT(true),
    GOLD_INGOT(true),
    DIAMOND(true),
    EMERALD(true),
    REDSTONE(true),
    
    // Tools
    WOODEN_PICKAXE(true),
    STONE_PICKAXE(true),
    IRON_PICKAXE(true),
    GOLDEN_PICKAXE(true),
    DIAMOND_PICKAXE(true),
    
    WOODEN_SWORD(true),
    STONE_SWORD(true),
    IRON_SWORD(true),
    GOLDEN_SWORD(true),
    DIAMOND_SWORD(true),
    
    // Food items
    APPLE(true),
    BREAD(true),
    COOKED_BEEF(true),
    COOKED_PORKCHOP(true),
    CARROT(true),
    POTATO(true),
    
    // Building materials
    GLASS(true),
    BRICK(true),
    BRICKS(true),
    STONE_BRICKS(true),
    
    // Wool colors
    WHITE_WOOL(true),
    BLACK_WOOL(true),
    RED_WOOL(true),
    BLUE_WOOL(true),
    GREEN_WOOL(true),
    YELLOW_WOOL(true),
    
    // Non-items and legacy (should be filtered out)
    AIR(false),
    LEGACY_STONE(false, true),
    LEGACY_DIRT(false, true),
    VOID_AIR(false),
    CAVE_AIR(false);
    
    private final boolean isItem;
    private final boolean isLegacy;
    
    MockMaterial(boolean isItem) {
        this(isItem, false);
    }
    
    MockMaterial(boolean isItem, boolean isLegacy) {
        this.isItem = isItem;
        this.isLegacy = isLegacy;
    }
    
    /**
     * @return true if this material represents an item that can be held in inventory
     */
    public boolean isItem() {
        return isItem;
    }
    
    /**
     * @return true if this is a legacy material that should be ignored
     */
    public boolean isLegacy() {
        return isLegacy;
    }
}