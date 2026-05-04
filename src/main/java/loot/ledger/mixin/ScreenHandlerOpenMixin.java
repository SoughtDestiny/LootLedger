package loot.ledger.mixin;

import loot.ledger.ScreenHandlerPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerOpenMixin implements ScreenHandlerPos {

    private BlockPos lootledger_pos = null;

    @Override
    public void lootledger_setPos(BlockPos pos) {
        this.lootledger_pos = pos;
    }

    @Override
    public BlockPos lootledger_getPos() {
        return this.lootledger_pos;
    }
}
