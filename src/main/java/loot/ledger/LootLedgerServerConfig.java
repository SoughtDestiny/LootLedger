package loot.ledger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LootLedgerServerConfig {
    private static final Logger LOGGER = LootLedger.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = LootLedger.MOD_ID + ".json";

    private final transient File configFile;

    private boolean logDataSave = false;
    private int logDataSaveInterval = 20 * 60 * 5;

    public LootLedgerServerConfig() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME).toFile();
    }

    public void load() {
        if (!configFile.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            LootLedgerServerConfig loaded = GSON.fromJson(reader, LootLedgerServerConfig.class);
            if (loaded != null) {
                this.logDataSave = loaded.logDataSave;
                this.logDataSaveInterval = loaded.logDataSaveInterval;
            }
        } catch (IOException e) {
            LOGGER.error("[LootLedger] Failed to load config", e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("[LootLedger] Failed to save config", e);
        }
    }

    public boolean doLogDataSave() {
        return logDataSave;
    }

    public int getLogDataSaveInterval() {
        return logDataSaveInterval;
    }
}
