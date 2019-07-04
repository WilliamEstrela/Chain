package br.com.craftlife;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.Bukkit.getServer;

public class ChainPlugin extends JavaPlugin implements CommandExecutor {

    static Config config;

    @java.lang.Override
    public void onEnable() {

		config = new Config("plugins/Chain", "config.yml", this);

        ChainCommand chain = new ChainCommand();
        getCommand("chain").setExecutor(chain);
        getServer().getPluginManager().registerEvents(chain, this);
    }

    @java.lang.Override
    public void onDisable() {

    }
}