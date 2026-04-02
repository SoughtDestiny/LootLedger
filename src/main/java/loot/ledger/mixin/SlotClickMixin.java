package loot.ledger.mixin;

import loot.ledger.LootLedgerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(AbstractContainerMenu.class)
public class SlotClickMixin {

    private Map<Integer, ItemStack> lootledger_snapshot = new HashMap<>();

    @Inject(method = "doClick", at = @At("HEAD"))
    private void onSlotClickHead(int slotIndex, int button, ContainerInput actionType,
                                 Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        AbstractContainerMenu handler = (AbstractContainerMenu) (Object) this;
        if (handler == serverPlayer.inventoryMenu) return;

        lootledger_snapshot.clear();
        for (Slot slot : handler.slots) {
            if (!(slot.container instanceof Inventory)) {
                lootledger_snapshot.put(handler.slots.indexOf(slot), slot.getItem().copy());
            }
        }
    }

    @Inject(method = "doClick", at = @At("TAIL"))
    private void onSlotClickTail(int slotIndex, int button, ContainerInput actionType,
                                 Player player, CallbackInfo ci) {
        if (lootledger_snapshot.isEmpty()) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        AbstractContainerMenu handler = (AbstractContainerMenu) (Object) this;
        if (handler == serverPlayer.inventoryMenu) return;

        for (Slot slot : handler.slots) {
            if (slot.container instanceof Inventory) continue;
            int idx = handler.slots.indexOf(slot);
            ItemStack before = lootledger_snapshot.getOrDefault(idx, ItemStack.EMPTY);
            ItemStack after = slot.getItem().copy();
            if (!ItemStack.matches(before, after)) {
                LootLedgerEvents.afterSlotClick(serverPlayer, handler, idx, before, after);
            }
        }

        lootledger_snapshot.clear();
    }
}
