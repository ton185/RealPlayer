package com.name.realplayer.transformers;

import com.name.realplayer.Config;
import com.name.realplayer.RealPlayer;
import com.name.realplayer.UniversalUnsafeHacks;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.name.realplayer.RealPlayer.MODID;

public class LoaderService implements ITransformationService {
    @Override
    public String name() {
        return "realplayer_loader";
    }

    @Override
    public void initialize(IEnvironment iEnvironment) {
        Path configDir = iEnvironment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElseThrow(() -> new RuntimeException("No game path found")).resolve("config");
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Config.path = configDir.resolve(MODID + "-config.json");
        Config.strPath = Config.path.toString();
        Config.load();
    }

    @Override
    public void beginScanning(IEnvironment iEnvironment) {
    }


    @Override
    public void onLoad(IEnvironment iEnvironment, Set<String> set) {
        try {
            RealPlayer.LOGGER.info("Attempting to load RealPlayer");
            Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
            launchPluginsField.setAccessible(true);
            LaunchPluginHandler launchPluginHandler = (LaunchPluginHandler) launchPluginsField.get(Launcher.INSTANCE);
            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>) pluginsField.get(launchPluginHandler);
            LinkedHashMap<String, ILaunchPluginService> sortedPlugins = new LinkedHashMap<>();
            sortedPlugins.put("realplayer_launchpluginservice", new LaunchPluginService());
            sortedPlugins.putAll(plugins);
            UniversalUnsafeHacks.setField(pluginsField, launchPluginHandler, sortedPlugins);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            // initial delay, repeat delay
            //save the config every 30 seconds
            scheduler.scheduleAtFixedRate(Config::save, 15, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
