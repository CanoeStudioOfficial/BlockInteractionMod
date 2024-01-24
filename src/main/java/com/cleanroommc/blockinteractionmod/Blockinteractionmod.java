package com.cleanroommc.blockinteractionmod;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.client.resources.I18n;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.config.Configuration;
import net.minecraft.util.ResourceLocation;

@Mod(modid = "blockinteractionmod", name = "BlockInteractionMod", version = "1.0")
public class Blockinteractionmod {
    private static List<Block> blockedBlocks = new ArrayList<Block>();
    private static boolean defaultBlockInteraction;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;
        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (blockedBlocks.contains(block)) {
            event.setCanceled(true);

            event.getEntityPlayer().sendMessage(new TextComponentString(I18n.format("blockinteractionmod.blockedBlockMessage")));
        }
    }

    private static void loadConfig() {
        Configuration config = new Configuration(new File("config/blockinteractionmod.cfg"));
        config.load();

        defaultBlockInteraction = config.getBoolean("defaultBlockInteraction", "general", false,
                "Whether block interaction is allowed by default");

        blockedBlocks.clear();
        String[] defaultBlockedBlocks = { Blocks.CRAFTING_TABLE.getRegistryName().toString() };
        blockedBlocks.addAll(getBlocksFromConfig(config, "blockedBlocks", defaultBlockedBlocks));

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static List<Block> getBlocksFromConfig(Configuration config, String category, String[] defaultValues) {
        List<Block> blocks = new ArrayList<Block>();
        String[] blockNames = config.getStringList("blockedBlocks", category, defaultValues, "List of blocked block names");
        for (String blockName : blockNames) {
            Block block = GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(blockName));
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }
}