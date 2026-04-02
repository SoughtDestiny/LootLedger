package loot.ledger.gui;

import loot.ledger.LootLedgerClient;
import loot.ledger.LootLedgerConfig;
import loot.ledger.network.LootLedgerPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {

    private final Screen parent;
    private final boolean canEdit;
    private int currentMaxEntries;

    private static final int PANEL_WIDTH  = 260;
    private static final int PANEL_HEIGHT = 150;

    public SettingsScreen(Screen parent) {
        super(Component.literal("LootLedger Settings"));
        this.parent = parent;
        this.currentMaxEntries = LootLedgerClient.currentMaxEntries;
        this.canEdit = LootLedgerClient.canEditConfig;
    }

    @Override
    protected void init() {
        int panelX = (this.width  - PANEL_WIDTH)  / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int btnY = panelY + 58;
        int centerX = panelX + PANEL_WIDTH / 2;

        if (canEdit) {
            // Left arrow
            this.addRenderableWidget(Button.builder(Component.literal("\u25C0"), btn -> {
                currentMaxEntries = LootLedgerConfig.prevOption(currentMaxEntries);
                LootLedgerClient.currentMaxEntries = currentMaxEntries;
                ClientPlayNetworking.send(new LootLedgerPackets.ConfigUpdatePayload(currentMaxEntries));
            }).bounds(centerX - 70, btnY, 20, 20).build());

            // Right arrow
            this.addRenderableWidget(Button.builder(Component.literal("\u25B6"), btn -> {
                currentMaxEntries = LootLedgerConfig.nextOption(currentMaxEntries);
                LootLedgerClient.currentMaxEntries = currentMaxEntries;
                ClientPlayNetworking.send(new LootLedgerPackets.ConfigUpdatePayload(currentMaxEntries));
            }).bounds(centerX + 50, btnY, 20, 20).build());
        }

        // Back button
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 40, panelY + PANEL_HEIGHT - 30, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int panelX = (this.width  - PANEL_WIDTH)  / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Panel background
        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xF0101020);

        // Border
        ctx.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFF5865F2);
        ctx.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF5865F2);
        ctx.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFF5865F2);
        ctx.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF5865F2);

        // Header
        ctx.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 20, 0xFF1E1E3A);
        ctx.text(this.font, "LootLedger Settings", panelX + 8, panelY + 6, 0xFFFFFFFF, true);
        ctx.fill(panelX + 1, panelY + 20, panelX + PANEL_WIDTH - 1, panelY + 21, 0xFF5865F2);

        // Label
        ctx.text(this.font, "Max Entries per Container:", panelX + 12, panelY + 36, 0xFFCCCCCC, true);

        // Current value display
        int centerX = panelX + PANEL_WIDTH / 2;
        int btnY = panelY + 58;
        String label = LootLedgerConfig.getLabel(currentMaxEntries);
        int labelWidth = this.font.width(label);
        ctx.text(this.font, label, centerX - labelWidth / 2, btnY + 6, 0xFFFFFFFF, true);

        // Warning for unlimited
        if (currentMaxEntries == -1) {
            String warn1 = "\u26A0 Warning: High memory usage!";
            String warn2 = "May cause lag on large servers.";
            int w1 = this.font.width(warn1);
            int w2 = this.font.width(warn2);
            ctx.text(this.font, warn1, centerX - w1 / 2, btnY + 26, 0xFFFF4444, true);
            ctx.text(this.font, warn2, centerX - w2 / 2, btnY + 38, 0xFF888888, true);
        }

        // Read-only hint for non-operators on multiplayer
        if (!canEdit) {
            String hint = "Configured by server operator";
            int hintWidth = this.font.width(hint);
            ctx.text(this.font, hint, centerX - hintWidth / 2, btnY + 26, 0xFF666688, true);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
