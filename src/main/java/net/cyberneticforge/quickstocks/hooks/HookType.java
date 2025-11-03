package net.cyberneticforge.quickstocks.hooks;

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
