package loot.ledger.mixin;

import loot.ledger.LootLedgerEvents;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ContainerCloseMixin {

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void onCloseHandledScreen(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        LootLedgerEvents.onContainerClosed(player);
    }
}
