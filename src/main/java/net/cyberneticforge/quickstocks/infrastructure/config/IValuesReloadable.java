package net.cyberneticforge.quickstocks.infrastructure.config;

public interface IValuesReloadable {
    String getConfig();
    void reloadValues();
}
