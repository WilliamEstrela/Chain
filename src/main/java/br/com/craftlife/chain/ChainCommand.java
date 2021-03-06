package br.com.craftlife.chain;

import br.com.craftlife.chain.resource.Message;
import br.com.craftlife.chain.utils.InventoryUtils;
import br.com.craftlife.chain.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChainCommand implements CommandExecutor, Listener {

    private ChainPlugin plugin;

    private ArrayList<Player> chainPlayers = new ArrayList<>();
    private HashMap<String, Integer> points = new HashMap<>();
    private List<String> joinedPlayers = new ArrayList<>();
    private HashMap<String, Location> locations = new HashMap<>();

    public ChainCommand(ChainPlugin plugin) {
        this.plugin = plugin;
        initSchedulers();
        locations.put("arena", LocationUtils.deserialize(plugin.getConfig().getString("locations.arena")));
        locations.put("exit", LocationUtils.deserialize(plugin.getConfig().getString("locations.exit")));
        locations.put("cabin", LocationUtils.deserialize(plugin.getConfig().getString("locations.cabin")));
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
                    if (locations.get("cabin") == null) {
                        new Message("messages.error.location-not-defined").colored().send(player);
                        return true;
                    }
                    player.teleport(locations.get("cabin"));
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
                } else if (strings[0].equalsIgnoreCase(new Message("commands.setloc").getString())) {
                    if (!player.hasPermission("chain.admin")) {
                        new Message("messages.error.permission").colored().send(player);
                        return true;
                    }
                    if (strings.length < 2)
                        help(player);
                    else {
                        String position = strings[1].toLowerCase();
                        if (position.equals("arena") || position.equals("exit")
                            || position.equals("cabin")){
                            new Message("messages.setpos.success").set("position", position).colored().send(player);
                            Location loc = player.getLocation();
                            locations.put(position, loc);
                            plugin.getConfig().set("locations." + position, LocationUtils.serialize(loc));
                            plugin.saveConfig();
                        } else {
                            help(player);
                        }
                    }
                } else if (strings[0].equalsIgnoreCase(new Message("commands.setkit").getString())) {
                    if (!player.hasPermission("chain.admin")) {
                        new Message("messages.error.permission").colored().send(player);
                        return true;
                    }
                    if (strings.length < 2)
                        help(player);
                    else {
                        String kit = strings[1].toLowerCase();
                        if (kit.equals("player") || kit.equals("vip")) {
                            new Message("messages.setkit.success").set("kit", kit).colored().send(player);
                            InventoryUtils.saveInventory(kit, player.getInventory());
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
        int delayCollectPoints = plugin.getConfig().getInt("collect-points.delay") * 20;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ChainPlugin.getInstance(), () -> {
            List<String> dominants = new ArrayList<>();
            int dominantPoints = 0;
            for (Player player : chainPlayers) {
                int points = 0;
                for (ItemStack itemStack : player.getInventory().getStorageContents()){
                    if (itemStack == null) continue;
                    for (String str : plugin.getConfig().getStringList("collect-points.items")) {
                        String[] values = str.split(";");
                        if (itemStack.getType().equals(Material.getMaterial(values[0]))) {
                            points += itemStack.getAmount() * Integer.valueOf(values[1]);
                        }
                    }
                }
                this.points.put(player.getName(), this.points.getOrDefault(player.getName(), 0) + points);
                player.getInventory().remove(Material.DIAMOND_BLOCK);
                player.getInventory().remove(Material.IRON_BLOCK);
                if (points != 0)
                    new Message("messages.point.receive").set("points", String.valueOf(points)).colored().send(player);
                if (points >= dominantPoints) {
                    if (points == dominantPoints)
                        dominants.add(player.getName());
                    else {
                        dominantPoints = points;
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
        }, delayCollectPoints, delayCollectPoints);
        int delayJoinMessage = plugin.getConfig().getInt("join-message-delay") * 20;
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
        }, delayJoinMessage, delayJoinMessage);
    }

    private void help(Player player) {
        new Message("messages.help.player", true)
                .set("command_join", new Message("commands.join").getString())
                .set("command_exit", new Message("commands.exit").getString())
                .set("command_cabin", new Message("commands.cabin").getString())
                .set("command_list", new Message("commands.list").getString())
                .set("command_point", new Message("commands.point").getString())
                .colored().send(player);

        if (player.hasPermission("chain.admin")) {
            new Message("messages.help.admin", true)
                    .set("command_setloc", new Message("commands.setloc").getString())
                    .set("command_setkit", new Message("commands.setkit").getString())
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
        if (locations.get("arena") == null || locations.get("exit") == null) {
            new Message("messages.error.location-not-defined").colored().send(player);
            return;
        }
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, player.getLocation(), locations.get("arena"), PlayerTeleportEvent.TeleportCause.COMMAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        new Message("messages.join.player").set("command_exit", new Message("commands.exit").getString())
                .colored().send(player);
        player.teleport(locations.get("arena"));

        if(player.hasPermission("chain.vip")){
            InventoryUtils.setKit("vip", player);
        } else {
            InventoryUtils.setKit("player", player);
        }

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
        if (locations.get("exit") == null) {
            new Message("messages.error.location-not-defined").colored().send(player);
            return;
        }
        chainPlayers.remove(player);
        player.getInventory().clear();
        player.teleport(locations.get("exit"));
        new Message("messages.exit.success").colored().send(player);
    }
}