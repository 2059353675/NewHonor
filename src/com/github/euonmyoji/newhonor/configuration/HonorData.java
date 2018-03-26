package com.github.euonmyoji.newhonor.configuration;

import com.github.euonmyoji.newhonor.NewHonor;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.util.Optional;

public class HonorData {
    private static CommentedConfigurationNode cfg;
    private static ConfigurationLoader<CommentedConfigurationNode> loader;

    static {
        loader = HoconConfigurationLoader.builder()
                .setPath(NewHonor.plugin.cfgDir.resolve("honor.conf")).build();
        cfg = load();
        set("default", cfg.getNode("default", "value").getString("[默认头衔]"));
    }

    public static boolean effects(String id, String effectsID) {
        cfg.getNode(id, "effects").setValue(effectsID);
        return save();
    }

    public static Optional<String> getEffectsID(String id) {
        return Optional.ofNullable(cfg.getNode(id, "effects").getString(null));
    }

    public static boolean add(String id, String honor) {
        if (!isExist(id)) {
            cfg.getNode(id, "value").setValue(honor);
            return save();
        }
        return false;
    }

    public static Optional<Text> getHonor(String id) {
        return Optional.ofNullable(cfg.getNode(id, "value").getString(null)).map(TextSerializers.FORMATTING_CODE::deserializeUnchecked);
    }

    public static boolean set(String id, String honor) {
        cfg.getNode(id, "value").setValue(honor);
        return save();
    }

    public static boolean delete(String id) {
        return cfg.removeChild(id) && save();
    }

    private static CommentedConfigurationNode load() {
        try {
            return loader.load();
        } catch (IOException e) {
            return loader.createEmptyNode(ConfigurationOptions.defaults());
        }
    }

    public static void reload() {
        cfg = load();
    }

    private static boolean save() {
        try {
            loader.save(cfg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isExist(String id) {
        return !cfg.getNode(id).isVirtual();
    }
}
