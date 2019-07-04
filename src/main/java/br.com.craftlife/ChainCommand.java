package br.com.craftlife;


import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    private ArrayList<Player> chainPlayers = new ArrayList<>();
    private HashMap<String, Integer> points = new HashMap<>();
    private List<String> joinedPlayers = new ArrayList<>();

    public ChainCommand() {
        initSchedulers();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            if(strings.length > 0){
                switch (strings[0]){
                    case "entrar":
                        boolean temItem = verificaSeTemItensOuArmadura(player);
                        if(!temItem){
                            colocaJogadorNaArenaChain(player);
                        }
                        break;
                    case "list":
                        mostraListaDeJogadores(player);
                        break;
                    case "sair":
                        if(verficaSePlayerTaNoChain(player)){
                            chainSair(player);
                        }else{
                            player.sendMessage("§4Voce nao esta no chain");
                        }
                        break;
                    case "point":
                        if (strings.length < 2) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&cUso correto: /chain point <player>"));
                        } else {
                            Player target = Bukkit.getPlayer(strings[1]);
                            if (target != null)
                                mostraChainPoint(player, target.getName());
                            else
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                        "&cO jogador '" + strings[1] + "' está offline"));
                        }
                        break;
                    case "setdeatch":
                        if(player.isOp()){
                            player.sendMessage("§2O player quando morrer ou deslogar do chain nascera aqui");
                            ChainPlugin.config.setLocation("deatch", player.getLocation());
                        }
                        break;
                    case "setarena":
                        if (player.isOp()) {
                            player.sendMessage("§2Arena setada com sucesso");
                            ChainPlugin.config.setLocation("spawn", player.getLocation());
                        }
                        break;
                    default:
                        comandosDeAjuda(player);
                        break;
                }

            }else{
                comandosDeAjuda(player);
            }


        }
        return false;
    }

    private void initSchedulers() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ChainPlugin.instance, () -> {
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
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&2Você acaba de ganhar &e" + realPoints + " &2pontos no chain!"));
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
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7[Chain] &6O jogador &e" + dominants.get(0) + " &6está dominando a arena chain!"));
                else {
                    String alldominants = String.join(", ", dominants);
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7[Chain] &6Os jogadores: &e" + alldominants + " &6estão dominando a arena chain!"));
                }
            }
        }, 6000, 6000);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ChainPlugin.instance, () -> {
            if (!joinedPlayers.isEmpty()) {
                if (joinedPlayers.size() == 1) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7[Chain] &2O jogador &e" + joinedPlayers.get(0) + " &2entrou no /chain"));
                } else {
                    String joined = String.join( ", ", joinedPlayers);
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                            "&7[Chain] &2Os jogadores &e" + joined + "entraram no /chain"));
                }
            }
        }, 100, 100);
    }
    private boolean verificaSeTemSpawnESpawnDeatch() {
        boolean status = false;
        try{
            Location loc1 = ChainPlugin.config.getLocation("spawn");
            Location loc2 = ChainPlugin.config.getLocation("deatch");

            status = true;
        }catch (Exception e){
            status = false;
        }
        return status;
    }

    private boolean verficaSePlayerTaNoChain(Player player) {
        if(chainPlayers.contains(player)){
            return true;
        }else{
            return false;
        }
    }

    private void comandosDeAjuda(Player player) {
        player.sendMessage("§4Comandos disponiveis");
        player.sendMessage("§e/chain entrar       §2-> Entra na arena chain");
        player.sendMessage("§e/chain sair         §2-> Sai da arena chain");
        player.sendMessage("§e/chain list         §2-> Lista todos jogadores na arena chain");
        player.sendMessage("§e/chain point <nick> §2-> Mostra pontuação de um jogador no chain");

        if(player.isOp()){
            player.sendMessage("§e/chain setdeatch §2-> Seta o local que o player vai nascer caso deslogue ou morra");
            player.sendMessage("§e/chain setarena     §2-> Seta o spawn da arena chain");
        }
    }

    private void mostraListaDeJogadores(Player p) {
        if(chainPlayers.isEmpty()){
            p.sendMessage(  "§4Nao ha jogadores na arena chain");
        }else{
            p.sendMessage("§2Jogadores na arena chain" );
            StringBuilder buffer = new StringBuilder();
            for (Player player : chainPlayers) {
                buffer.insert(0, player.getDisplayName());
                buffer.insert(0, " ");
            }
            p.sendMessage(buffer.toString());
        }

    }

    private void colocaJogadorNaArenaChain(Player player) {
        player.sendMessage("§2Voce entrou no /chain, para sair /chain sair");
        player.teleport(ChainPlugin.config.getLocation("spawn").clone());

        if(player.hasPermission("cl.vip")){
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

        joinedPlayers.add(player.getName());
        chainPlayers.add(player);
    }

    private boolean verificaSeTemItensOuArmadura(Player player) {
        boolean temItem = false;
        for(ItemStack item: player.getInventory().getContents()){
            if(item != null){
                temItem = true;
            }
        }
        boolean temArmadura = false;
        for(ItemStack item: player.getInventory().getArmorContents()){
            if(item != null){
                temArmadura = true;
            }
        }

        if(temItem || temArmadura){
            player.sendMessage("§4Voce precisa limpar o inventario para entrar no chain");
            if(temArmadura){
                player.sendMessage("§4Voce tem armadura tambem, lembre-se de retirar ela");
            }
        }
        return temItem;
    }

    private void mostraChainPoint(Player player, String target) {
        int point = points.getOrDefault(target, 0);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&2Pontuação de " + target + ": &e" + point));
    }

    public void removeAllPlayers() {
        for (Player player : chainPlayers) {
            chainSair(player);
        }
    }

    private ItemStack getEnchantedArmor(Material material, boolean glow) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Armadura Chain"));
        itemMeta.setUnbreakable(true);
        if (glow)
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        if(verficaSePlayerTaNoChain(event.getPlayer())){
            //chain sair true // false
            boolean cancelCommand = true;
            for (String allowedcmd : ChainPlugin.config.getConfig().getStringList("allowed-cmds")){
                if (event.getMessage().startsWith(allowedcmd)) {
                    cancelCommand = false;
                    break;
                }
            }
            if(cancelCommand) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§4Voce nao pode usar comandos no /chain, saia usando /chain sair");
            }
        }
    }

    @EventHandler
    public void onquit(PlayerQuitEvent e){
        if(chainPlayers.contains(e.getPlayer())){
            Player p = e.getPlayer();
            this.chainSair(p);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if(chainPlayers.contains(e.getEntity().getPlayer())){
            this.chainSair(e.getEntity().getPlayer());
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
    public void onMove(PlayerTeleportEvent e) {
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

    private void chainSair(Player player){
        chainPlayers.remove(player);
        player.getInventory().clear();
        player.teleport(ChainPlugin.config.getLocation("deatch"));
        player.sendMessage("§2Voce saiu do /chain");
    }
}