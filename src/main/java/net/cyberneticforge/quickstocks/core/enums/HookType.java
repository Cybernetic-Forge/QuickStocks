package net.cyberneticforge.quickstocks.core.enums;

@SuppressWarnings("SameParameterValue")
public enum HookType {
    ChestShop("ChestShop"),
    WorldGuard("WorldGuard");

    private final String pluginName;

    HookType(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String toString() { return pluginName; }
}
