package loot.ledger.mixin.client;

import loot.ledger.LootLedgerClient;
import loot.ledger.LootLedgerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientContainerOpenMixin {

    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            if (LootLedgerClient.pendingPos != null
                    && client.screen instanceof AbstractContainerScreen<?> screen
                    && screen instanceof LootLedgerScreen lootScreen) {
                lootScreen.setLootLedgerPos(LootLedgerClient.pendingPos);
            }
        });
    }
}
