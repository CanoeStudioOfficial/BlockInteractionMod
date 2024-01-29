package com.cleanroommc.blockinteractionmod;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
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
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.common.config.Configuration;
import net.minecraft.util.ResourceLocation;

@Mod(modid = "blockinteractionmod", name = "BlockInteractionMod", version = "1.0")
public class Blockinteractionmod {
    private static Set<Block> blockedBlocks = new HashSet<Block>();
    private static Set<Item> blockedItems = new HashSet<Item>();
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerInteractItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) return;

        ItemStack heldItem = event.getItemStack();
        if (isItemBlocked(heldItem)) {
            event.setCanceled(true);
            event.getEntityPlayer().sendMessage(new TextComponentString(I18n.format("blockinteractionmod.blockedItemMessage")));
        }
    }

    private static boolean isItemBlocked(ItemStack itemStack) {
        return blockedItems.contains(itemStack.getItem()) || itemStack.getItem() == Items.DIAMOND_SWORD;
    }

    private static void loadConfig() {
        Configuration config = new Configuration(new File("config/blockinteractionmod.cfg"));
        config.load();

        defaultBlockInteraction = config.getBoolean("defaultBlockInteraction", "general", false,
                "Whether block interaction is allowed by default");

        blockedBlocks.clear();
        String[] defaultBlockedBlocks = { Blocks.CRAFTING_TABLE.getRegistryName().toString() };
        blockedBlocks.addAll(getBlocksFromConfig(config, "blockedBlocks", defaultBlockedBlocks));

        blockedItems.clear();
        String[] defaultBlockedItems = { Items.DIAMOND_SWORD.getRegistryName().toString() };
        blockedItems.addAll(getItemsFromConfig(config, "blockedItems", defaultBlockedItems));

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static Set<Block> getBlocksFromConfig(Configuration config, String category, String[] defaultValues) {
        Set<Block> blocks = new HashSet<Block>();
        String[] blockNames = config.getStringList("blockedBlocks", category, defaultValues, "List of blocked block names");
        for (String blockName : blockNames) {
            Block block = GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(blockName));
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private static Set<Item> getItemsFromConfig(Configuration config, String category, String[] defaultValues) {
        Set<Item> items = new HashSet<Item>();
        String[] itemNames = config.getStringList("blockedItems", category, defaultValues, "List of blocked item names");
        for (String itemName : itemNames) {
            Item item = GameRegistry.findRegistry(Item.class).getValue(new ResourceLocation(itemName));
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}