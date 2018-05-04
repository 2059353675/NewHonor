package com.github.euonmyoji.newhonor;

import com.github.euonmyoji.newhonor.command.HonorCommand;
import com.github.euonmyoji.newhonor.configuration.EffectsData;
import com.github.euonmyoji.newhonor.configuration.HonorData;
import com.github.euonmyoji.newhonor.configuration.NewHonorConfig;
import com.github.euonmyoji.newhonor.configuration.PlayerData;
import com.github.euonmyoji.newhonor.listener.NewHonorMessageListener;
import com.github.euonmyoji.newhonor.listener.UltimateChatEventListener;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.omg.CORBA.UNKNOWN;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.plugin.meta.version.ComparableVersion;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author yinyangshi
 */
@Plugin(id = "newhonor", name = "New Honor", version = NewHonor.VERSION, authors = "yinyangshi", description = "NewHonor plugin")
public class NewHonor {
    public static final String VERSION = "1.5.5";
    public static final NewHonorMessageChannel M_MESSAGE = new NewHonorMessageChannel();
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path cfgDir;

    @Inject
    public Logger logger;

    public static NewHonor plugin;
    public static final HashMap<UUID, Text> HONOR_TEXT_CACHE = new HashMap<>();
    private static final HashMap<UUID, String> PLAYER_USING_EFFECT_CACHE = new HashMap<>();
    public static final HashMap<String, List<PotionEffect>> EFFECTS_CACHE = new HashMap<>();

    private static final String COMPATIBLE_UCHAT_NODE_PATH = "compatibleUChat";
    private static final String DISPLAY_HONOR_NODE_PATH = "displayHonor";
    private static final String USE_PAPI_NODE_PATH = "usePAPI";
    private static final String FORCE_ENABLE_DEFAULT_LISTENER = "force-enable-default-listener";

    private final UltimateChatEventListener UChatListener = new UltimateChatEventListener();
    private final NewHonorMessageListener NewHonorListener = new NewHonorMessageListener();


    @Listener
    public void onStarting(GameStartingServerEvent event) {
        plugin = this;
        try {
            NewHonorConfig.defaultCfgDir = cfgDir;
            if (Files.notExists(cfgDir)) {
                Files.createDirectory(cfgDir);
            }
            cfgDir = null;
            NewHonorConfig.init();
            if (Files.notExists(NewHonorConfig.cfgDir)) {
                Files.createDirectory(NewHonorConfig.cfgDir);
            }
            final String playerData = "PlayerData";
            if (Files.notExists(NewHonorConfig.cfgDir.resolve(playerData))) {
                Files.createDirectory(NewHonorConfig.cfgDir.resolve(playerData));
            }
            if (NewHonorConfig.isCheckUpdate()) {
                checkUpdate();
            } else {
                logger.info("check update was canceled");
            }
            NewHonorConfig.getCfg().getNode(COMPATIBLE_UCHAT_NODE_PATH)
                    .setValue(NewHonorConfig.getCfg().getNode(COMPATIBLE_UCHAT_NODE_PATH).getBoolean(false));
            NewHonorConfig.getCfg().getNode(DISPLAY_HONOR_NODE_PATH)
                    .setValue(NewHonorConfig.getCfg().getNode(DISPLAY_HONOR_NODE_PATH).getBoolean(false));
            NewHonorConfig.getCfg().getNode(USE_PAPI_NODE_PATH)
                    .setValue(NewHonorConfig.getCfg().getNode(USE_PAPI_NODE_PATH).getBoolean(false));
            NewHonorConfig.getCfg().getNode(FORCE_ENABLE_DEFAULT_LISTENER)
                    .setValue(NewHonorConfig.getCfg().getNode(FORCE_ENABLE_DEFAULT_LISTENER).getBoolean(false));
            NewHonorConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkUpdate() {
        Task.builder().async().name("NewHonor - check for update").execute(() -> {
            try {
                URL url = new URL("https://api.com.github.com/repos/euOnmyoji/NewHonor-SpongePlugin/releases");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.getResponseCode();
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), Charsets.UTF_8)) {
                    JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonArray().get(0).getAsJsonObject();
                    String version = jsonObject.get("tag_name").getAsString().replace("v", "")
                            .replace("version", "");
                    int c = new ComparableVersion(version).compareTo(new ComparableVersion(VERSION));
                    if (c > 0) {
                        logger.info("found a latest version:" + version + ".Your version now:" + VERSION);
                    } else if (c < 0) {
                        logger.info("the latest version in com.github:" + version + "[Your version:" + VERSION + "]");
                    }
                }
            } catch (Exception e) {
                logger.info("check for updating failed");
            }
        }).submit(this);
    }

    @Inject
    private Metrics metrics;

    @Listener
    public void onStarted(GameStartedServerEvent event) {
        Sponge.getCommandManager().register(this, HonorCommand.honor, "honor", "honour");
        Task.builder().execute(() -> PLAYER_USING_EFFECT_CACHE.forEach((uuid, s) -> Sponge.getServer().getPlayer(uuid)
                .ifPresent(player -> {
                    if (EFFECTS_CACHE.containsKey(s)) {
                        PotionEffectData effects = player.getOrCreate(PotionEffectData.class).orElseThrow(UNKNOWN::new);
                        List<PotionEffect> list = EFFECTS_CACHE.get(s);
                        list.forEach(effects::addElement);
                        player.offer(effects);
                    }
                }))).name("newhonor - givePlayerEffects").intervalTicks(20).submit(this);
        logger.info("NewHonor插件作者邮箱:1418780411@qq.com");
        choosePluginMode();
        metrics.addCustomChart(new Metrics.SimplePie("useeffects", () -> EFFECTS_CACHE.size() > 0 ? "true" : "false"));
        metrics.addCustomChart(new Metrics.SimplePie("displayhonor", () -> ScoreBoardManager.enable ? "true" : "false"));
        metrics.addCustomChart(new Metrics.SimplePie("usepapi",
                () -> NewHonorConfig.getCfg().getNode(USE_PAPI_NODE_PATH).getBoolean() ? "true" : "false"));
    }

    @Listener
    public void onClientConnectionJoin(ClientConnectionEvent.Join event) {
        Player p = event.getTargetEntity();
        PlayerData pd = new PlayerData(p);
        Task.builder().execute(() -> {
            if (!pd.init()) {
                logger.error("初始化玩家" + p.getName() + "," + p.getUniqueId() + ",头衔数据失败！");
            }
            doSomething(pd);
        }).async().delayTicks(20).name("newhonor - init Player" + p.getName()).submit(this);
        ScoreBoardManager.initPlayer(p);

    }

    @Listener
    public void onPlayerDie(RespawnPlayerEvent event) {
        Player p = event.getTargetEntity();
        PlayerData pd = new PlayerData(p);
        Task.builder().execute(() -> doSomething(pd)).async().name("newhonor - (die) init Player" + p.getName()).submit(this);
    }


    public static void clearCaches() {
        HONOR_TEXT_CACHE.clear();
        EFFECTS_CACHE.clear();
        PLAYER_USING_EFFECT_CACHE.clear();
    }

    public void choosePluginMode() {
        EventManager eventManager = Sponge.getEventManager();
        eventManager.unregisterListeners(UChatListener);
        eventManager.unregisterListeners(NewHonorListener);
        ScoreBoardManager.enable = false;
        ScoreBoardManager.clear();
        if (NewHonorConfig.getCfg().getNode(COMPATIBLE_UCHAT_NODE_PATH).getBoolean(false)) {
            Sponge.getEventManager().registerListeners(this, UChatListener);
            logger.info("uchat mode enabled");
        } else if (NewHonorConfig.getCfg().getNode(DISPLAY_HONOR_NODE_PATH).getBoolean(false)) {
            ScoreBoardManager.enable = true;
            ScoreBoardManager.init();
            logger.info("displayHonor mode enabled");
            if (NewHonorConfig.getCfg().getNode(FORCE_ENABLE_DEFAULT_LISTENER).getBoolean()) {
                Sponge.getEventManager().registerListeners(this, NewHonorListener);
            }
        } else {
            Sponge.getEventManager().registerListeners(this, NewHonorListener);
        }
        boolean usePAPI = NewHonorConfig.getCfg().getNode(USE_PAPI_NODE_PATH).getBoolean(false);
        if (usePAPI) {
            PlaceHolderManager.create();
            logger.info("enabled PAPI");
        }
    }

    public static void doSomething(PlayerData pd) {
        pd.checkUsing();
        PLAYER_USING_EFFECT_CACHE.remove(pd.getUUID());
        HONOR_TEXT_CACHE.remove(pd.getUUID());
        if (pd.isUseHonor()) {
            pd.getHonor().ifPresent(text -> HONOR_TEXT_CACHE.put(pd.getUUID(), text));
            if (pd.isEnableEffects()) {
                HonorData.getEffectsID(pd.getUsingHonorID()).ifPresent(s -> {
                    try {
                        EFFECTS_CACHE.put(s, new EffectsData(s).getEffects());
                        PLAYER_USING_EFFECT_CACHE.put(pd.getUUID(), s);
                    } catch (ObjectMappingException e) {
                        NewHonor.plugin.logger.warn("解析头衔效果组" + s + "配置文件时出错", e);
                    }
                });
            }
        }
        Sponge.getServer().getPlayer(pd.getUUID()).ifPresent(ScoreBoardManager::initPlayer);
    }
}
