package net.cyberneticforge.quickstocks.hooks;

public enum HookType {
    ChestShop("ChestShop");

    private final String pluginName;

    HookType(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String toString() { return pluginName; }
}
