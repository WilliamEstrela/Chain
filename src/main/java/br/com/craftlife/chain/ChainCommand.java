package br.com.craftlife.chain;

import br.com.craftlife.chain.resource.Message;
import br.com.craftlife.chain.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChainCommand implements CommandExecutor, Listener {

    private ChainPlugin plugin;

    private ArrayList<Player> chainPlayers = new ArrayList<>();
    private HashMap<String, Integer> points = new HashMap<>();
    private List<String> joinedPlayers = new ArrayList<>();

    public ChainCommand(ChainPlugin plugin) {
        this.plugin = plugin;
        initSchedulers();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            if(strings.length > 0){
                if (strings[0].equalsIgnoreCase(new Message("commands.join").getString())) {
                    boolean hasItem = verifyInventory(player);
                    if(!hasItem){
                        putPlayerInArena(player);
                    }
                } else if (strings[0].equalsIgnoreCase(new Message("commands.exit").getString())) {
                    if(chainPlayers.contains(player)) {
                        chainExit(player);
                    } else {
                        new Message("messages.exit.error").colored().send(player);
                    }
                } else if (strings[0].equalsIgnoreCase(new Message("commands.cabin").getString())) {
                    player.teleport(LocationUtils.deserialize(plugin.getConfig().getString("locations.cabin")));
                    new Message("messages.cabin.success").colored().send(player);
                } else if (strings[0].equalsIgnoreCase(new Message("commands.list").getString())) {
                    showPlayerList(player);
                } else if (strings[0].equalsIgnoreCase(new Message("commands.point").getString())) {
                    if (strings.length < 2) {
                        help(player);
                    } else {
                        Player target = Bukkit.getPlayer(strings[1]);
                        if (target != null)
                            showPoint(player, target.getName());
                        else
                            new Message("messages.point.player-offline").set("player", strings[1]).colored().send(player);
                    }
                } else if (strings[0].equalsIgnoreCase(new Message("commands.set").getString())) {
                    if (strings.length < 2) {
                        help(player);
                    } else {
                        String position = strings[1].toLowerCase();
                        if (position.equals("arena") || position.equals("exit")
                            || position.equals("cabin")){
                            new Message("messages.set.success").set("position", position).colored().send(player);
                            plugin.getConfig().set("locations." + position, LocationUtils.serialize(player.getLocation()));
                            plugin.saveConfig();
                        } else {
                            help(player);
                        }
                    }
                } else {
                    help(player);
                }
            } else {
                help(player);
            }
        }
        return true;
    }

    private void initSchedulers() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ChainPlugin.getInstance(), () -> {
            List<String> dominants = new ArrayList<>();
            int dominantPoints = 0;
            for (Player player : chainPlayers) {
                int points = 0;
                for (ItemStack itemStack : player.getInventory().getContents()){
                    if (itemStack == null) continue;
                    if (itemStack.getType().equals(Material.DIAMOND_BLOCK) || itemStack.getType().equals(Material.IRON_BLOCK)){
                        points += itemStack.getAmount();
                    }
                }
                int realPoints = points - 1;
                this.points.put(player.getName(), this.points.getOrDefault(player, 0) + realPoints);
                player.getInventory().remove(Material.DIAMOND_BLOCK);
                player.getInventory().remove(Material.IRON_BLOCK);
                if (realPoints != 0)
                    new Message("messages.point.receiver").set("points", String.valueOf(realPoints)).colored().send(player);
                if (realPoints >= dominantPoints) {
                    if (realPoints == dominantPoints)
                        dominants.add(player.getName());
                    else {
                        dominantPoints = realPoints;
                        dominants.clear();
                        dominants.add(player.getName());
                    }
                }
            }
            if (!dominants.isEmpty()) {
                if (dominants.size() == 1)
                    new Message("messages.arena.domination.one").set("player", dominants.get(0)).colored().broadcast();
                else {
                    String alldominants = String.join(", ", dominants);
                    new Message("messages.arena.domination.more").set("players", alldominants).colored().broadcast();
                }
            }
        }, 6000, 6000);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ChainPlugin.getInstance(), () -> {
            if (!joinedPlayers.isEmpty()) {
                if (joinedPlayers.size() == 1) {
                    new Message("messages.join.broadcast.one").set("player", joinedPlayers.get(0)).colored().broadcast();
                } else {
                    String joined = String.join( ", ", joinedPlayers);
                    new Message("messages.join.broadcast.more").set("players", joined).colored().broadcast();
                }
            }
            joinedPlayers.clear();
        }, 100, 100);
    }

    private void help(Player player) {
        new Message("messages.help.player", true)
                .set("command_join", new Message("commands.join").getString())
                .set("command_exit", new Message("commands.exit").getString())
                .set("command_cabin", new Message("commands.cabin").getString())
                .set("command_list", new Message("commands.list").getString())
                .set("command_point", new Message("commands.point").getString())
                .colored().send(player);

        if (player.isOp()) {
            new Message("messages.help.admin", true)
                    .set("command_set", new Message("commands.set").getString())
                    .colored().send(player);
        }
    }

    private void showPlayerList(Player player) {
        if(chainPlayers.isEmpty()){
            new Message("messages.list.no-players").colored().send(player);
        }else{
            List<String> players = new ArrayList<>();
            for (Player chainPlayer : chainPlayers) players.add(chainPlayer.getName());
            String strPlayers = String.join(", ", players);
            new Message("messages.list.show").set("players", strPlayers).colored().send(player);
        }

    }

    private void putPlayerInArena(Player player) {
        new Message("messages.join.player").set("command_exit", new Message("commands.exit").getString())
                .colored().send(player);
        player.teleport(LocationUtils.deserialize(plugin.getConfig().getString("locations.arena")));

        if(player.hasPermission("chain.vip")){
            player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_BLOCK));
            player.getInventory().setChestplate(this.getEnchantedArmor(Material.CHAINMAIL_CHESTPLATE, true));
            player.getInventory().setLeggings(this.getEnchantedArmor(Material.CHAINMAIL_LEGGINGS, true));
            player.getInventory().setBoots(this.getEnchantedArmor(Material.CHAINMAIL_BOOTS, true));
        }else{
            player.getInventory().setHelmet(new ItemStack(Material.IRON_BLOCK));
            player.getInventory().setChestplate(this.getEnchantedArmor(Material.CHAINMAIL_CHESTPLATE, false));
            player.getInventory().setLeggings(this.getEnchantedArmor(Material.CHAINMAIL_LEGGINGS, false));
            player.getInventory().setBoots(this.getEnchantedArmor(Material.CHAINMAIL_BOOTS, false));
        }


        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));


        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
        bow.setItemMeta(bowMeta);
        player.getInventory().addItem(new ItemStack(bow));

        player.getInventory().addItem(new ItemStack(Material.ARROW));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE,10));

        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setAllowFlight(false);

        for(PotionEffect effect : player.getActivePotionEffects())
        {
            player.removePotionEffect(effect.getType());
        }
        if (!joinedPlayers.contains(player.getName()))
            joinedPlayers.add(player.getName());
        chainPlayers.add(player);
    }

    private boolean verifyInventory(Player player) {
        boolean hasItem = false;
        for (ItemStack item: player.getInventory().getContents()) {
            if (item != null) {
                hasItem = true;
            }
        }
        boolean hasArmor = false;
        for(ItemStack item: player.getInventory().getArmorContents()){
            if(item != null){
                hasArmor = true;
            }
        }

        if(hasItem || hasArmor){
            new Message("messages.join.error.inventory").colored().send(player);
            if(hasArmor){
                new Message("messages.join.error.armor").colored().send(player);
            }
        }
        return hasItem;
    }

    private void showPoint(Player player, String target) {
        int point = points.getOrDefault(target, 0);
        new Message("messages.point.see").set("player", target).set("points", String.valueOf(point)).colored().send(player);
    }

    public void removeAllPlayers() {
        for (Player player : chainPlayers) {
            chainExit(player);
        }
    }

    private ItemStack getEnchantedArmor(Material material, boolean glow) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Chainmail Armor"));
        itemMeta.setUnbreakable(true);
        if (glow)
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void blockCommand(PlayerCommandPreprocessEvent e){
        if(chainPlayers.contains(e.getPlayer())){
            for (String allowedcmd : plugin.getConfig().getStringList("allowed-cmds"))
                if (e.getMessage().startsWith(allowedcmd)) return;
            e.setCancelled(true);
            new Message("messages.arena.blocked-command").set("command_exit", new Message("commands.exit").getString())
                    .colored().send(e.getPlayer());
        }
    }

    @EventHandler
    public void exitByQuit(PlayerQuitEvent e){
        if(chainPlayers.contains(e.getPlayer())){
            Player p = e.getPlayer();
            this.chainExit(p);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if(chainPlayers.contains(e.getEntity().getPlayer())){
            this.chainExit(e.getEntity().getPlayer());
        }
    }

    @EventHandler
    public void preDeath(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;
        Player player = (Player) e.getEntity();
        if (!chainPlayers.contains(player))
            return;
        if (e.getFinalDamage() >= player.getHealth()) {
            player.getInventory().setChestplate(new ItemStack(Material.AIR));
            player.getInventory().setLeggings(new ItemStack(Material.AIR));
            player.getInventory().setBoots(new ItemStack(Material.AIR));
            player.getInventory().remove(Material.IRON_SWORD);
            player.getInventory().remove(Material.ARROW);
            player.getInventory().remove(Material.BOW);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if(chainPlayers.contains(e.getPlayer())){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (chainPlayers.contains(e.getWhoClicked()) && e.getSlotType().equals(InventoryType.SlotType.ARMOR)) {
            e.setCancelled(true);
        }
    }

    private void chainExit(Player player){
        chainPlayers.remove(player);
        player.getInventory().clear();
        player.teleport(LocationUtils.deserialize(plugin.getConfig().getString("locations.exit")));
        new Message("messages.exit.success").colored().send(player);
    }
}