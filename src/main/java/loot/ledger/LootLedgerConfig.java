package loot.ledger;

import net.minecraft.nbt.CompoundTag;

public class LootLedgerConfig {

    private static int maxEntriesPerContainer = 128;

    public static final int[] OPTIONS = {32, 64, 128, 256, -1};

    public static int getMaxEntries() {
        return maxEntriesPerContainer;
    }

    public static void setMaxEntries(int value) {
        boolean valid = false;
        for (int opt : OPTIONS) {
            if (opt == value) { valid = true; break; }
        }
        if (valid) {
            maxEntriesPerContainer = value;
        }
    }

    public static String getLabel(int value) {
        if (value == -1) return "Unlimited";
        return String.valueOf(value);
    }

    public static int nextOption(int current) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i] == current) {
                return OPTIONS[(i + 1) % OPTIONS.length];
            }
        }
        return 128;
    }

    public static int prevOption(int current) {
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i] == current) {
                return OPTIONS[(i - 1 + OPTIONS.length) % OPTIONS.length];
            }
        }
        return 128;
    }

    public static CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("maxEntries", maxEntriesPerContainer);
        return tag;
    }

    public static void fromNbt(CompoundTag tag) {
        int loaded = tag.getInt("maxEntries").orElse(128);
        boolean valid = false;
        for (int opt : OPTIONS) {
            if (opt == loaded) { valid = true; break; }
        }
        maxEntriesPerContainer = valid ? loaded : 128;
    }
}
