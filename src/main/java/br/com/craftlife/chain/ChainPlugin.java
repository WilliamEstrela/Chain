package br.com.craftlife.chain;

import br.com.craftlife.chain.resource.LanguageResource;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class ChainPlugin extends JavaPlugin implements CommandExecutor {

    private static ChainPlugin instance;
    private ChainCommand chain;
    private LanguageResource language;

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
    }

    private void setLanguage(LanguageResource languageResource) {
        this.language = languageResource;
    }

    public static ChainPlugin getInstance() {
        return ChainPlugin.instance;
    }
    public LanguageResource getLanguage() {
        return language;
    }
}
