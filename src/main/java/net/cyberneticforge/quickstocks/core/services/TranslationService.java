package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.infrastructure.config.YamlParser;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Objects;

public class TranslationService {

    private final YamlParser config;

    public TranslationService() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "Translations.yml");
        setup();
    }

    public void setup() {
        FileConfiguration def = YamlParser.getDefaultConfig("Translations.yml");
        for (Translation translation : Translation.values()) {
            config.addMissing(translation.getPath(), def.getString(translation.getPath()));
        }
        config.saveChanges();
    }

    public String messageString(Translation message, Replaceable... replaceables) {
        StringBuilder builder = new StringBuilder();

        if (config.isList(message.getPath())) {
            List<String> stringList = config.getStringList(message.getPath());
            for (int i = 0; i < stringList.size(); i++) {
                stringList.set(i, ChatUT.hexString(stringList.get(i)));
                for (Replaceable r : replaceables) {
                    stringList.set(i, stringList.get(i).replace(r.getKey(), r.getValue()));
                }
                builder.append(stringList.get(i));
                if (i < stringList.size() - 1)
                    builder.append("\n");
            }
        } else {
            String text = Objects.requireNonNull(ChatUT.hexString(config.getString(message.getPath())));
            for (Replaceable r : replaceables) {
                text = text.replace(r.getKey(), r.getValue());
            }
            builder.append(text);
        }

        return builder.toString();
    }


    public Component message(Translation message, Replaceable... replaceables) {
        Component component = Component.text("");
        if (config.isList(message.getPath())) {
            List<String> stringList = config.getStringList(message.getPath());
            for (int i = 0; i < stringList.size(); i++) {
                String msg = stringList.get(i);
                component = component.append(ChatUT.hexComp(msg));
                if (i < stringList.size() - 1)
                    component = component.append(Component.newline());
            }
        } else {
            String msg = Objects.requireNonNull(config.getString(message.getPath()));
            component = component.append(ChatUT.hexComp(msg));
        }

        for (Replaceable r : replaceables) {
            Component text = ChatUT.fixColor(ChatUT.serialize(component), r.getKey(), r.getValue());
            if(r.hasHoverEvent())
                text = text.hoverEvent(r.getHoverEvent());
            if(r.hasClickEvent())
                text = text.clickEvent(r.getClickEvent());
            TextReplacementConfig replace = TextReplacementConfig.builder().match(r.getKey()).replacement(text).build();
            component = component.replaceText(replace);
        }
        return component;
    }

    public String[] lore(Translation message, Replaceable... replaceables) {
        List<String> stringList = config.getStringList(message.getPath());

        if (config.isList(message.getPath())) {
            for (int i = 0; i < stringList.size(); i++) {
                stringList.set(i, ChatUT.hexString(stringList.get(i)));
                for (Replaceable r : replaceables) {
                    stringList.set(i, stringList.get(i).replace(r.getKey(), r.getValue()));
                }
            }
        }

        String[] entries = new String[stringList.size()];
        for(int i = 0; i < stringList.size(); i++)
            entries[i] = stringList.get(i);

        return entries;
    }
}
