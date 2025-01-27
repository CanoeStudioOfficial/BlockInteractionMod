package com.lonelyxiya.blockinteractionmod;

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
import net.minecraftforge.common.config.Configuration;
import net.minecraft.util.ResourceLocation;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.resources.I18n;

@Mod(modid = "blockinteractionmod", name = "BlockInteractionMod", version = "1.0")
public class Blockinteractionmod {
    private static Set<BlockData> blockedBlocks = new HashSet<>();
    private static Set<ItemData> blockedItems = new HashSet<>();
    private static boolean defaultBlockInteraction;

    private static class MultiBlockData {
        private Set<BlockData> blockSet;

        public MultiBlockData(Set<BlockData> blockSet) {
            this.blockSet = blockSet;
        }

        public Set<BlockData> getBlockSet() {
            return blockSet;
        }
    }

    private static Set<MultiBlockData> blockedMultiBlocks = new HashSet<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        loadConfig();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld() == null || event.getWorld().isRemote) return;

        Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
        int meta = getMetaFromState(event);
        if (isBlocked(block, meta)) {
            event.setCanceled(true);
            event.getEntityPlayer().sendMessage(new TextComponentString(I18n.format("blockinteractionmod.blockedBlockMessage")));
            return;
        }

        for (MultiBlockData multiBlock : blockedMultiBlocks) {
            Set<BlockData> blockSet = multiBlock.getBlockSet();
            boolean isMultiBlockPresent = true;
            for (BlockData data : blockSet) {
                if (!checkMultiBlockPresence(event, data)) {
                    isMultiBlockPresent = false;
                    break;
                }
            }
            if (isMultiBlockPresent) {
                event.setCanceled(true);
                event.getEntityPlayer().sendMessage(new TextComponentString(I18n.format("blockinteractionmod.blockedMultiBlockMessage")));
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerInteractItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld() == null || event.getItemStack() == null || event.getWorld().isRemote) return;

        ItemStack heldItem = event.getItemStack();
        int meta = heldItem.getMetadata();
        if (isBlocked(heldItem.getItem(), meta)) {
            event.setCanceled(true);
            event.getEntityPlayer().sendMessage(new TextComponentString(I18n.format("blockinteractionmod.blockedItemMessage")));
        }
    }

    private static boolean isBlocked(Block block, int meta) {
        return blockedBlocks.stream().anyMatch(blockedBlock -> blockedBlock.block == block && blockedBlock.meta == meta);
    }

    private static boolean isBlocked(Item item, int meta) {
        return blockedItems.stream().anyMatch(blockedItem -> blockedItem.item == item && blockedItem.meta == meta);
    }

    private static void loadConfig() {
        Configuration config = new Configuration(new File("config/blockinteractionmod.cfg"));
        try {
            config.load();
            backupConfigFile(config);
            defaultBlockInteraction = config.getBoolean("defaultBlockInteraction", "general", false, "Whether block interaction is allowed by default");

            blockedBlocks.clear();
            Set<BlockData> defaultBlockedBlocks = new HashSet<>();
            defaultBlockedBlocks.add(new BlockData(Blocks.CRAFTING_TABLE, 0)); // Example default blocked block with metadata 0
            Set<BlockData> configBlocks = getBlocksFromConfig(config, "blockedBlocks", new HashSet<>());
            mergeBlockData(defaultBlockedBlocks, configBlocks);
            blockedBlocks.addAll(configBlocks);

            blockedItems.clear();
            Set<ItemData> defaultBlockedItems = new HashSet<>();
            defaultBlockedItems.add(new ItemData(Items.DIAMOND_SWORD, 0)); // Example default blocked item with metadata 0
            Set<ItemData> configItems = getItemsFromConfig(config, "blockedItems", new HashSet<>());
            mergeItemData(defaultBlockedItems, configItems);
            blockedItems.addAll(configItems);

            blockedMultiBlocks.clear();
            loadBlockedMultiBlocks(config);

            if (config.hasChanged()) {
                config.save();
            }
        } catch (Exception e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
        }
    }

    private static int getMetaFromState(PlayerInteractEvent.RightClickBlock event) {
        return event.getWorld().getBlockState(event.getPos()).getBlock().getMetaFromState(event.getWorld().getBlockState(event.getPos()));
    }

    private static boolean checkMultiBlockPresence(PlayerInteractEvent.RightClickBlock event, BlockData data) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block checkBlock = event.getWorld().getBlockState(event.getPos().add(x, y, z)).getBlock();
                    int checkMeta = getMetaFromState(event);
                    if (checkBlock == data.block && checkMeta == data.meta) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void mergeBlockData(Set<BlockData> defaultBlockedBlocks, Set<BlockData> configBlocks) {
        for (BlockData defaultBlock : defaultBlockedBlocks) {
            if (!configBlocks.contains(defaultBlock)) {
                configBlocks.add(defaultBlock);
            }
        }
    }

    private static void mergeItemData(Set<ItemData> defaultBlockedItems, Set<ItemData> configItems) {
        for (ItemData defaultItem : defaultBlockedItems) {
            if (!configItems.contains(defaultItem)) {
                configItems.add(defaultItem);
            }
        }
    }

    private static void loadBlockedMultiBlocks(Configuration config) {
        String[] multiBlockConfigs = config.getStringList("blockedMultiBlocks", "general", new String[]{}, "List of blocked multi-block structures");
        for (String multiBlockConfig : multiBlockConfigs) {
            String[] blockConfigs = multiBlockConfig.split(";");
            Set<BlockData> blockSet = new HashSet<>();
            for (String blockConfig : blockConfigs) {
                String[] parts = blockConfig.split(":");
                if (parts.length == 2) {
                    Block block = GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(parts[0], parts[1]));
                    if (block != null) {
                        int meta = parseMeta(parts);
                        blockSet.add(new BlockData(block, meta));
                    }
                }
            }
            if (!blockSet.isEmpty()) {
                blockedMultiBlocks.add(new MultiBlockData(blockSet));
            }
        }
    }

    private static int parseMeta(String[] parts) {
        int meta = 0;
        if (parts.length == 3) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                // Handle invalid metadata
            }
        }
        return meta;
    }

    private static void backupConfigFile(Configuration config) {
        File configFile = config.getConfigFile();
        // Backup logic remains the same
    }

    private static Set<BlockData> getBlocksFromConfig(Configuration config, String category, Set<BlockData> defaultValues) {
        Set<BlockData> blocks = new HashSet<>();
        String[] blockNames = config.getStringList("blockedBlocks", category, getBlockDataAsStringArray(defaultValues), "List of blocked block names");
        for (String blockName : blockNames) {
            String[] parts = blockName.split(":");
            if (parts.length == 2) {
                Block block = GameRegistry.findRegistry(Block.class).getValue(new ResourceLocation(parts[0], parts[1]));
                if (block != null) {
                    int meta = parseMeta(parts);
                    blocks.add(new BlockData(block, meta));
                }
            }
        }
        return blocks;
    }

    private static Set<ItemData> getItemsFromConfig(Configuration config, String category, Set<ItemData> defaultValues) {
        Set<ItemData> items = new HashSet<>();
        String[] itemNames = config.getStringList("blockedItems", category, getItemDataAsStringArray(defaultValues), "List of blocked item names");
        for (String itemName : itemNames) {
            String[] parts = itemName.split(":");
            if (parts.length == 2) {
                Item item = GameRegistry.findRegistry(Item.class).getValue(new ResourceLocation(parts[0], parts[1]));
                if (item != null) {
                    int meta = parseMeta(parts);
                    items.add(new ItemData(item, meta));
                }
            }
        }
        return items;
    }

    private static String[] getBlockDataAsStringArray(Set<BlockData> blockDataSet) {
        String[] dataArray = new String[blockDataSet.size()];
        int index = 0;
        for (BlockData data : blockDataSet) {
            dataArray[index++] = data.toString();
        }
        return dataArray;
    }

    private static String[] getItemDataAsStringArray(Set<ItemData> itemDataSet) {
        String[] dataArray = new String[itemDataSet.size()];
        int index = 0;
        for (ItemData data : itemDataSet) {
            dataArray[index++] = data.toString();
        }
        return dataArray;
    }

    private static class BlockData {
        public Block block;
        public int meta;

        public BlockData(Block block, int meta) {
            this.block = block;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BlockData that = (BlockData) obj;
            return meta == that.meta && block.equals(that.block);
        }

        @Override
        public int hashCode() {
            return 31 * block.hashCode() + meta;
        }

        @Override
        public String toString() {
            return block.getRegistryName() + ":" + meta;
        }
    }

    private static class ItemData {
        public Item item;
        public int meta;

        public ItemData(Item item, int meta) {
            this.item = item;
            this.meta = meta;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ItemData that = (ItemData) obj;
            return meta == that.meta && item.equals(that.item);
        }

        @Override
        public int hashCode() {
            return 31 * item.hashCode() + meta;
        }

        @Override
        public String toString() {
            return item.getRegistryName() + ":" + meta;
        }
    }
}