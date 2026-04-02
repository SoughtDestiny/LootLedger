package loot.ledger;

import net.minecraft.core.BlockPos;

public interface ScreenHandlerPos {
    void lootledger_setPos(BlockPos pos);
    BlockPos lootledger_getPos();
}
