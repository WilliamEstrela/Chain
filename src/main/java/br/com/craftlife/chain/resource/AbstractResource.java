package br.com.craftlife.chain.resource;

import br.com.craftlife.chain.ChainPlugin;
import com.google.common.base.Charsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public abstract class AbstractResource {

    private final ChainPlugin plugin;
    private final String resource;
    private final File file;
    private FileConfiguration newFile;

    AbstractResource(ChainPlugin plugin, String resource) {
        this.plugin = plugin;
        this.resource = resource;
        this.file = new File(this.plugin.getDataFolder(), resource);
    }

    private void reloadFile() {
        this.newFile = YamlConfiguration.loadConfiguration(this.file);

        final InputStream defLangStream = this.plugin.getResource(this.resource);
        if (defLangStream == null) {
            return;
        }

        this.newFile.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, Charsets.UTF_8)));
    }

    public void saveFile() {
        try {
            this.getFile().save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.file, e);
        }
    }

    public FileConfiguration getFile() {
        if (this.newFile == null) {
            this.reloadFile();
        }
        return this.newFile;
    }
}
