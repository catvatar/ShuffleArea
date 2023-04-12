package me.catvatar.shufflearea;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import com.sk89q.worldedit.world.World;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

//TODO figure out a way to run replaceWith on block update inside selection
//TODO add more coments
//TODO code needs refactoring
public final class ShuffleArea extends JavaPlugin {

    Map<Material,Integer> map = new HashMap<>();
    Region saveRegion = null;
    String listOfCommands = "[clear|loadSelection|replaceWith|reshuffle]";

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("\n\n\tShuffleArea has been enabled!\n\n");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("ShuffleArea has been disabled!");
    }

    //TODO integrate masks
    void Shuffle(CommandSender sender,Region selection,World world,Map<Material,Integer> patternRecipe){
        if(selection != null) {
            RandomPattern pattern = new RandomPattern();
            String verbousPattern = "";
            for(Map.Entry<Material,Integer> entry : patternRecipe.entrySet()){
                //TODO add flags to ignore on demand
                if(entry.getKey() != Material.AIR){
                    verbousPattern += entry.getValue() + "%, " + entry.getKey();
                    pattern.add(BukkitAdapter.adapt(entry.getKey().createBlockData()),entry.getValue());
                }
            }

            //TODO check if saveRegion differs from selection
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)){
                RegionFunction set = new BlockReplace(editSession, pattern);
                RegionVisitor visitor = new RegionVisitor(selection, set);
                Operations.completeBlindly(visitor);
            }

            sender.sendMessage("Replaced with " + verbousPattern);

        }else {
            sender.sendMessage("You must first loadSelection");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        //catch commands of interest
        if(cmd.getName().equalsIgnoreCase("shufflearea")||cmd.getName().equalsIgnoreCase("shfa")){
            //check if command was cast by a player
            if(sender instanceof Player){
                Player player = (Player) sender;
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(new BukkitPlayer(player));
                World world = session.getSelectionWorld();
                Region selection = null;

                //handle arguments
                //TODO refactor arguments handeler
                if(args.length > 0){
                    String argument = args[0];
                    if(argument.equalsIgnoreCase("clear")) {
                        map.clear();
                        saveRegion = null;
                        sender.sendMessage("Cleared");
                        return true;
                    }else if (argument.equalsIgnoreCase("loadSelection")) {
                        try {
                            selection = session.getSelection(world);
                        } catch (IncompleteRegionException e) {
                            throw new RuntimeException(e);
                        }

                        if (selection != null) {
                            saveRegion = selection.clone();
                            sender.sendMessage("Selection loaded");
                            return true;
                        } else {
                            sender.sendMessage("You haven't made a selection yet!");
                        }
                    }else if(argument.equalsIgnoreCase("replaceWith")) {
                        try {
                            selection = session.getSelection(world);
                        } catch (IncompleteRegionException e) {
                            throw new RuntimeException(e);
                        }
                        if (selection != null) {
                            map.clear();
                            for (BlockVector3 block : selection) {
                                Material blockType = player.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getType();
                                map.putIfAbsent(blockType, 0);
                                map.put(blockType, map.get(blockType) + 1);
                            }
                            Shuffle(sender, saveRegion,world, map);
                            return true;
                        } else {
                            sender.sendMessage("You haven't made a selection yet!");
                        }
                    }else if(argument.equalsIgnoreCase("reshuffle")){
                        if(!map.isEmpty()){
                            Shuffle(sender,saveRegion,world,map);
                            return true;
                        }else{
                            sender.sendMessage("You must first replaceWith");
                        }
                    }else{
                        sender.sendMessage("Unknown argument, available: " + listOfCommands);
                    }

                }else{
                    sender.sendMessage("Argument not specified, available: " + listOfCommands);
                }

            }else{
                sender.sendMessage("This command can only be run by a player.");
            }
        }
        return false;
    }
}
