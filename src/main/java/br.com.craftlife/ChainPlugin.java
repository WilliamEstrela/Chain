package br.com.craftlife;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class ChainPlugin extends JavaPlugin implements CommandExecutor {

    public static ChainPlugin instance;
    static Config config;
    private ChainCommand chain;

    @java.lang.Override
    public void onEnable() {
        instance = this;
		config = new Config("plugins/Chain", "config.yml", this);

		chain = new ChainCommand();
        getCommand("chain").setExecutor(chain);
        getServer().getPluginManager().registerEvents(chain, this);
    }

    @java.lang.Override
    public void onDisable() {
        chain.removeAllPlayers();
    }
}
