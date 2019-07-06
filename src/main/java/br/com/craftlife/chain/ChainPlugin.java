package br.com.craftlife.chain;

import br.com.craftlife.chain.resource.KitResource;
import br.com.craftlife.chain.resource.LanguageResource;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ChainPlugin extends JavaPlugin implements CommandExecutor {

    private static ChainPlugin instance;
    private ChainCommand chain;
    private LanguageResource language;
    private KitResource kitConfig;

    @Override
    public void onEnable() {
        instance = this;
        this.setupFiles();
		chain = new ChainCommand(this);
        getCommand("chain").setExecutor(chain);
        getServer().getPluginManager().registerEvents(chain, this);
    }

    @Override
    public void onDisable() {
        chain.removeAllPlayers();
    }

    private void setupFiles() {
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.setLanguage(new LanguageResource(this, "lang-" + this.getConfig().getString("language") + ".yml"));
        this.getLanguage().getFile().options().copyDefaults(true);
        this.getLanguage().saveFile();

        this.setKitConfig(new KitResource(this, "kits.yml"));
        File file = new File(this.getDataFolder(), "kits.yml");
        this.getKitConfig().getFile().options().copyDefaults(!file.exists());
        this.getKitConfig().saveFile();
    }

    public static ChainPlugin getInstance() {
        return ChainPlugin.instance;
    }

    private void setLanguage(LanguageResource languageResource) {
        this.language = languageResource;
    }

    public LanguageResource getLanguage() {
        return language;
    }

    private void setKitConfig(KitResource kitResource) {
        this.kitConfig = kitResource;
    }

    public KitResource getKitConfig() {
        return kitConfig;
    }
}
