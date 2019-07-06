package br.com.craftlife.chain.utils;

import br.com.craftlife.chain.ChainPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtils {

    public static void saveInventory(String path, PlayerInventory inventory) {
        ChainPlugin plugin = ChainPlugin.getInstance();

        for (int i = 0 ; i < inventory.getSize() ; i++){
            plugin.getKitConfig().getFile().set(path + ".contents." + i, inventory.getItem(i) != null ? inventory.getItem(i) : new ItemStack(Material.AIR));
        }
        plugin.getKitConfig().saveFile();
    }

    public static void setKit(String path, Player player) {
        ChainPlugin plugin = ChainPlugin.getInstance();

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            player.getInventory().setItem(i, plugin.getKitConfig().getFile().getItemStack(path + ".contents." + i));
        }
    }
}
