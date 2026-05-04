package loot.ledger.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

import loot.ledger.LootLedgerClient;
import loot.ledger.LootLedgerConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryOverlayScreen extends Screen {

    private final List<ClientLogEntry> entries;
    private final BlockPos pos;

    private static final int PANEL_WIDTH     = 320;
    private static final int PANEL_HEIGHT    = 260;
    private static final int ENTRY_HEIGHT    = 26;
    private static final int VISIBLE_ENTRIES = 8;

    private int scrollOffset = 0;
    private int hoveredEntry = -1;

    public HistoryOverlayScreen(List<ClientLogEntry> entries, BlockPos pos) {
        super(Component.literal("LootLedger"));
        this.entries = entries;
        this.pos     = pos;
    }

    @Override
    protected void init() {
        int panelX = (this.width  - PANEL_WIDTH)  / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("X"), btn -> this.onClose())
                .bounds(panelX + PANEL_WIDTH - 20, panelY + 4, 16, 16)
                .build());

        this.addRenderableWidget(Button.builder(Component.empty(), btn -> {
            this.minecraft.setScreen(new SettingsScreen(this));
        }).bounds(panelX + PANEL_WIDTH - 40, panelY + 4, 16, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("\u25B2"), btn -> {
            if (scrollOffset > 0) scrollOffset--;
        }).bounds(panelX + PANEL_WIDTH - 20, panelY + 40, 16, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("\u25BC"), btn -> {
            if (scrollOffset < Math.max(0, entries.size() - VISIBLE_ENTRIES)) scrollOffset++;
        }).bounds(panelX + PANEL_WIDTH - 20, panelY + PANEL_HEIGHT - 24, 16, 16).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int panelX = (this.width  - PANEL_WIDTH)  / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0101020);

        ctx.fill(panelX,                   panelY,                    panelX + PANEL_WIDTH, panelY + 1,            0xFF5865F2);
        ctx.fill(panelX,                   panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,  0xFF5865F2);
        ctx.fill(panelX,                   panelY,                    panelX + 1,            panelY + PANEL_HEIGHT, 0xFF5865F2);
        ctx.fill(panelX + PANEL_WIDTH - 1, panelY,                    panelX + PANEL_WIDTH,  panelY + PANEL_HEIGHT, 0xFF5865F2);

        ctx.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 20, 0xFF1E1E3A);
        ctx.text(this.font, "Container History", panelX + 8, panelY + 6, 0xFFFFFFFF, true);

        ctx.fill(panelX + 1, panelY + 20, panelX + PANEL_WIDTH - 1, panelY + 32, 0xFF16162A);
        ctx.text(this.font,
                "Pos: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                panelX + 8, panelY + 23, 0xFFAAAAAA, true);

        String limitLabel = "Limit: " + LootLedgerConfig.getLabel(LootLedgerClient.currentMaxEntries);
        int limitWidth = this.font.width(limitLabel);
        ctx.text(this.font, limitLabel,
                panelX + PANEL_WIDTH - limitWidth - 24, panelY + 23, 0xFF666688, true);

        ctx.fill(panelX + 1, panelY + 32, panelX + PANEL_WIDTH - 1, panelY + 33, 0xFF5865F2);

        hoveredEntry = -1;
        int entryStartY = panelY + 36;

        if (entries.isEmpty()) {
            ctx.text(this.font,
                    "No entries yet.",
                    panelX + 10, entryStartY + 10, 0xFFAAAAAA, true);
        }

        for (int i = 0; i < VISIBLE_ENTRIES; i++) {
            int idx = i + scrollOffset;
            if (idx >= entries.size()) break;

            ClientLogEntry entry = entries.get(idx);
            int entryY = entryStartY + i * ENTRY_HEIGHT;

            int rowBg = (i % 2 == 0) ? 0x33FFFFFF : 0x1AFFFFFF;
            ctx.fill(panelX + 4, entryY, panelX + PANEL_WIDTH - 22, entryY + ENTRY_HEIGHT - 1, rowBg);

            if (mouseX >= panelX + 4 && mouseX <= panelX + PANEL_WIDTH - 22
                    && mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                hoveredEntry = idx;
                ctx.fill(panelX + 4, entryY, panelX + PANEL_WIDTH - 22, entryY + ENTRY_HEIGHT - 1, 0x44FFFFFF);
            }

            ctx.text(this.font,
                    entry.removed ? "-" : "+",
                    panelX + 8, entryY + 9,
                    entry.removed ? 0xFFFF4444 : 0xFF44FF44, true);

            ItemStack icon = getItemStack(entry.itemId);
            if (!icon.isEmpty()) {
                ctx.item(icon, panelX + 18, entryY + 4);
            }

            ctx.text(this.font,
                    entry.playerName,
                    panelX + 38, entryY + 4,
                    0xFF55FFFF, true);

            String localizedName = getLocalizedName(entry.itemId, entry.itemName);
            ctx.text(this.font,
                    entry.count + "x " + localizedName,
                    panelX + 38, entryY + 14,
                    0xFFFFDD44, true);

            renderPlayerAvatar(ctx, entry.playerName, panelX + PANEL_WIDTH - 80, entryY + 4, 16);

            ctx.text(this.font,
                    formatTime(entry.timestamp),
                    panelX + PANEL_WIDTH - 60, entryY + 9,
                    0xFF888888, true);
        }

        if (!entries.isEmpty()) {
            int from = scrollOffset + 1;
            int to = Math.min(scrollOffset + VISIBLE_ENTRIES, entries.size());
            String counter = from + "-" + to + " / " + entries.size();
            int counterWidth = this.font.width(counter);
            ctx.text(this.font, counter,
                    panelX + PANEL_WIDTH / 2 - counterWidth / 2,
                    panelY + PANEL_HEIGHT - 10, 0xFF666688, true);
        }

        if (hoveredEntry >= 0) {
            ClientLogEntry entry = entries.get(hoveredEntry);
            String localizedName = getLocalizedName(entry.itemId, entry.itemName);
            List<Component> tooltip = List.of(
                    Component.literal("\u00A7bItem: \u00A7f"   + localizedName),
                    Component.literal("\u00A7bID: \u00A77"     + entry.itemId),
                    Component.literal("\u00A7bAmount: \u00A7f" + entry.count),
                    Component.literal("\u00A7bPlayer: \u00A7f" + entry.playerName),
                    Component.literal("\u00A7bAction: "   + (entry.removed ? "\u00A7cRemoved" : "\u00A7aAdded")),
                    Component.literal("\u00A7bTime: \u00A77"   + formatTimeFull(entry.timestamp))
            );
            ctx.setTooltipForNextFrame(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);

        renderGearIcon(ctx, panelX + PANEL_WIDTH - 40, panelY + 4);
    }

    private void renderGearIcon(GuiGraphicsExtractor ctx, int x, int y) {
        int c = 0xFFCCCCDD;
        int hole = 0xFF1E1E3A;
        int accent = 0xFF5865F2;

        // Body (two overlapping rects → rounded shape)
        ctx.fill(x + 5, y + 4, x + 11, y + 12, c);
        ctx.fill(x + 4, y + 5, x + 12, y + 11, c);

        // Cardinal teeth (N, S, E, W)
        ctx.fill(x + 6, y + 2, x + 10, y + 5, c);
        ctx.fill(x + 6, y + 11, x + 10, y + 14, c);
        ctx.fill(x + 2, y + 6, x + 5, y + 10, c);
        ctx.fill(x + 11, y + 6, x + 14, y + 10, c);

        // Diagonal teeth (NW, NE, SW, SE)
        ctx.fill(x + 3, y + 3, x + 6, y + 6, c);
        ctx.fill(x + 10, y + 3, x + 13, y + 6, c);
        ctx.fill(x + 3, y + 10, x + 6, y + 13, c);
        ctx.fill(x + 10, y + 10, x + 13, y + 13, c);

        // Center hole
        ctx.fill(x + 6, y + 6, x + 10, y + 10, hole);
        ctx.fill(x + 7, y + 7, x + 9, y + 9, accent);
    }

    private void renderPlayerAvatar(GuiGraphicsExtractor ctx, String playerName, int x, int y, int size) {
        int color = getPlayerColor(playerName);
        ctx.fill(x, y, x + size, y + size, color);
        ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, darken(color));
        if (!playerName.isEmpty()) {
            ctx.text(this.font,
                    String.valueOf(playerName.charAt(0)).toUpperCase(),
                    x + 4, y + 4,
                    0xFFFFFFFF, true);
        }
    }

    private int getPlayerColor(String playerName) {
        int[] colors = {
                0xFF5865F2, 0xFF57F287, 0xFFFEE75C,
                0xFFEB459E, 0xFFED4245, 0xFF3BA55D,
                0xFF4752C4, 0xFF2D7D46
        };
        int idx = Math.abs(playerName.hashCode()) % colors.length;
        return colors[idx];
    }

    private int darken(int color) {
        int r = ((color >> 16) & 0xFF) / 2;
        int g = ((color >> 8)  & 0xFF) / 2;
        int b = ((color)       & 0xFF) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private String getLocalizedName(String itemId, String fallback) {
        try {
            return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(
                            itemId.contains(":") ? itemId.split(":")[0] : "minecraft",
                            itemId.contains(":") ? itemId.split(":")[1] : itemId))
                    .map(item -> item.getDefaultInstance().getHoverName().getString())
                    .orElse(fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private ItemStack getItemStack(String itemId) {
        try {
            return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(
                            itemId.contains(":") ? itemId.split(":")[0] : "minecraft",
                            itemId.contains(":") ? itemId.split(":")[1] : itemId))
                    .map(item -> new ItemStack(item.getDefaultInstance().getItem()))
                    .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount < 0) {
            if (scrollOffset < Math.max(0, entries.size() - VISIBLE_ENTRIES)) scrollOffset++;
        } else {
            if (scrollOffset > 0) scrollOffset--;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (this.minecraft != null) {
            if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE
                    || this.minecraft.options.keyInventory.matches(keyEvent)) {
                this.onClose();
                if (this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm").format(new Date(timestamp));
    }

    private String formatTimeFull(long timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(timestamp));
    }

    public record ClientLogEntry(
            String playerName,
            String itemId,
            String itemName,
            int count,
            boolean removed,
            long timestamp
    ) {}
}
