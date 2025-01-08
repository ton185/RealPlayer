package com.name.realplayer;

import com.google.gson.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    public static JsonObject data;
    public static String strPath;
    public static Path path;

    public static boolean isPatchingEnabledForClassAndMethod(String clazz, String method) {
        if (data.has(clazz)) {
            JsonObject classData = data.get(clazz).getAsJsonObject();
            if (classData.has(method)) {
                return classData.get(method).getAsBoolean();
            }
            classData.add(method, new JsonPrimitive(true));
            return true;
        }
        JsonObject obj = new JsonObject();
        obj.add(method, new JsonPrimitive(true));
        obj.add("0-all-enabled", new JsonPrimitive(true));
        data.add(clazz, obj);
        return true;
    }

    public static void save() {
        try (FileWriter fileWriter = new FileWriter(strPath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(data, fileWriter);
        } catch (IOException e) {
            RealPlayer.LOGGER.info("Error saving JSON to file: {}", e.toString());
        }
    }

    public static void load() {
        try (FileReader fileReader = new FileReader(strPath)) {
            Gson gson = new Gson();
            try {
                data = gson.fromJson(fileReader, JsonElement.class).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                RealPlayer.LOGGER.info("Json file was invalid, rewriting: {}", e.toString());
                Files.deleteIfExists(path);
                load();
            }
        } catch (IOException e) {
            data = new JsonObject();
            RealPlayer.LOGGER.info("Error reading config from file, this is probably due to it not existing: {}", e.toString());
            try (InputStream is = Config.class.getResourceAsStream("/default-config.json")) {
                if (is == null) {
                    throw new IOException("inputStream is null");
                }
                Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
                load();
            } catch (IOException ex) {
                RealPlayer.LOGGER.warn("Failed to copy default config: {}. Will use empty instead", ex.toString());
                try (FileWriter fileWriter = new FileWriter(strPath)) {
                    fileWriter.write("{}");
                } catch (IOException exc) {
                    RealPlayer.LOGGER.info("Error creating config file: {}", exc.toString());
                }
            }

        }
    }
}
