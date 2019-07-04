package br.com.craftlife;


import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;

public class ChainCommand implements CommandExecutor, Listener {
    private ArrayList<Player> chainPlayers = new ArrayList<>();

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
                    case "setdeatch":
                        if(player.isOp()){
                            player.sendMessage("§2O player quando morrer ou deslogar do chain nascera aqui");
                            ChainPlugin.config.setLocation("deatch", player.getLocation());
                        }
                        break;
                    case "setarena":
                        player.sendMessage("§2Arena setada com sucesso");
                        ChainPlugin.config.setLocation("spawn", player.getLocation());
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
        player.sendMessage("§e/chain entrar    §2-> Entra na arena chain");
        player.sendMessage("§e/chain sair      §2-> Sai da arena chain");
        player.sendMessage("§e/chain list      §2-> Lista todos jogadores na arena chain");

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
        player.getInventory().setHelmet(new ItemStack(Material.IRON_BLOCK));
        player.getInventory().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW,64));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE,10));

        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
        player.setHealth(10);
        player.setAllowFlight(false);

        for(PotionEffect effect : player.getActivePotionEffects())
        {
            player.removePotionEffect(effect.getType());
        }

        Bukkit.broadcastMessage("§2O jogador §e" + player.getDisplayName() + "§2 entrou no /chain");
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

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        if(verficaSePlayerTaNoChain(event.getPlayer())){
            //chain sair true // false

            if(!event.getMessage().startsWith("/chain sair") && !event.getMessage().startsWith("/chain list")){
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
    public void onMove(PlayerMoveEvent e) {
        if(chainPlayers.contains(e.getPlayer())){
            this.chainSair(e.getPlayer());
        }
    }

    private void chainSair(Player player){
        chainPlayers.remove(player);
        player.getInventory().clear();
        player.teleport(ChainPlugin.config.getLocation("deatch"));
        player.sendMessage("§2Voce saiu do /chain");
    }
}