package net.cyberneticforge.quickstocks.hooks;


import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.HookType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;

public class HookManager {

    private final HashSet<HookType> hookedPlugins;

    public HookManager() {
        hookedPlugins = new HashSet<>();
        for (HookType hook : HookType.values()) {
            if (QuickStocksPlugin.getInstance().getServer().getPluginManager().getPlugin(hook.toString()) != null) {
                hookedPlugins.add(hook);
                QuickStocksPlugin.getInstance().getLogger().log(Level.INFO, "Hook " + hook + " registered");
            }
        }
    }

    public boolean isHooked(HookType... hookTypes) {
        return hookedPlugins.containsAll(Arrays.stream(hookTypes).toList());
    }
}
