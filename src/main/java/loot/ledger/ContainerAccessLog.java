package loot.ledger;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import java.util.*;

public class ContainerAccessLog {

    private static final Map<BlockPos, List<LogEntry>> log = new HashMap<>();

    public static void addEntry(BlockPos pos, String playerName, ItemStack stack, boolean removed) {
        List<LogEntry> entries = log.computeIfAbsent(pos, k -> new ArrayList<>());

        String itemId   = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.getHoverName().getString();
        int count       = stack.getCount();

        entries.add(0, new LogEntry(playerName, itemId, itemName, count, removed, System.currentTimeMillis()));

        int maxEntries = LootLedgerConfig.getMaxEntries();
        if (maxEntries != -1 && entries.size() > maxEntries) {
            entries.subList(maxEntries, entries.size()).clear();
        }
    }

    public static List<LogEntry> getEntries(BlockPos pos) {
        return log.getOrDefault(pos, Collections.emptyList());
    }

    public static Map<BlockPos, List<LogEntry>> getAllEntries() {
        return Collections.unmodifiableMap(log);
    }

    public static void clearAll() {
        log.clear();
    }

    public static CompoundTag toNbt(BlockPos pos) {
        CompoundTag compound = new CompoundTag();
        ListTag list = new ListTag();
        for (LogEntry entry : log.getOrDefault(pos, Collections.emptyList())) {
            list.add(entry.toNbt());
        }
        compound.put("entries", list);
        return compound;
    }

    public static void fromNbt(BlockPos pos, CompoundTag compound) {
        ListTag list = compound.getListOrEmpty("entries");
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
            entries.add(LogEntry.fromNbt(entry));
        }
        log.put(pos, entries);
    }

    public static class LogEntry {
        public final String playerName;
        public final String itemId;
        public final String itemName;
        public final int count;
        public final boolean removed;
        public final long timestamp;

        public LogEntry(String playerName, String itemId, String itemName, int count, boolean removed, long timestamp) {
            this.playerName = playerName;
            this.itemId     = itemId;
            this.itemName   = itemName;
            this.count      = count;
            this.removed    = removed;
            this.timestamp  = timestamp;
        }

        public CompoundTag toNbt() {
            CompoundTag compound = new CompoundTag();
            compound.putString("player",   playerName);
            compound.putString("itemId",   itemId);
            compound.putString("itemName", itemName);
            compound.putInt("count",       count);
            compound.putBoolean("removed", removed);
            compound.putLong("timestamp",  timestamp);
            return compound;
        }

        public static LogEntry fromNbt(CompoundTag compound) {
            String player   = compound.getString("player").orElse("Unknown");
            String itemId   = compound.getString("itemId").orElse("minecraft:air");
            String itemName = compound.getString("itemName").orElse("Unknown Item");
            int count       = compound.getInt("count").orElse(1);
            boolean removed = compound.getBoolean("removed").orElse(true);
            long timestamp  = compound.getLong("timestamp").orElse(0L);
            return new LogEntry(player, itemId, itemName, count, removed, timestamp);
        }
    }
}
