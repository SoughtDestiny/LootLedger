package loot.ledger;

import loot.ledger.network.LootLedgerPackets;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class LootLedgerEvents {

    private static final Map<String, Map<Integer, ItemStack>> snapshots = new HashMap<>();
    private static final Map<String, Map<Item, Integer>> accumulators = new HashMap<>();
    private static final Map<String, Map<Item, ItemStack>> accumulatorReps = new HashMap<>();

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockEntity be = world.getBlockEntity(pos);
            if (!isTrackedContainer(be)) return InteractionResult.PASS;

            BlockPos trackPos = getCanonicalPos(world, pos, be);
            ServerPlayer serverPlayer = (ServerPlayer) player;

            Container inventory = getInventory(world, pos, be);
            if (inventory == null) return InteractionResult.PASS;

            pendingPos.put(serverPlayer.getStringUUID(), trackPos);
            pendingInventorySize.put(serverPlayer.getStringUUID(), inventory.getContainerSize());

            ServerPlayNetworking.send(
                    serverPlayer,
                    new LootLedgerPackets.ContainerOpenedPayload(trackPos)
            );

            return InteractionResult.PASS;
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (pendingPos.isEmpty()) return;
            for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer player : world.players()) {
                    String uuid = player.getStringUUID();
                    BlockPos pos = pendingPos.get(uuid);
                    if (pos == null) continue;

                    AbstractContainerMenu handler = player.containerMenu;
                    if (handler == player.inventoryMenu) continue;

                    ((ScreenHandlerPos)(Object) handler).lootledger_setPos(pos);

                    String key = snapshotKey(player, pos);
                    if (!snapshots.containsKey(key)) {
                        BlockEntity be = world.getBlockEntity(pos);
                        if (be != null) {
                            Container inventory = getInventory(world, pos, be);
                            if (inventory != null) {
                                takeSnapshot(player, pos, inventory);
                            }
                        }
                    }

                    pendingPos.remove(uuid);
                    pendingInventorySize.remove(uuid);
                }
            }
        });
    }

    private static final Map<String, BlockPos> pendingPos = new HashMap<>();
    private static final Map<String, Integer> pendingInventorySize = new HashMap<>();

    public static BlockPos getTrackedPos(ServerPlayer player, AbstractContainerMenu handler) {
        if (handler == player.inventoryMenu) return null;
        BlockPos pos = ((ScreenHandlerPos)(Object) handler).lootledger_getPos();
        if (pos == null) return null;
        String key = snapshotKey(player, pos);
        if (!snapshots.containsKey(key)) return null;
        return pos;
    }

    public static void accumulate(ServerPlayer player, BlockPos pos,
                                   Map<Item, Integer> diff, Map<Item, ItemStack> reps) {
        String key = snapshotKey(player, pos);
        Map<Item, Integer> acc = accumulators.computeIfAbsent(key, k -> new HashMap<>());
        Map<Item, ItemStack> repAcc = accumulatorReps.computeIfAbsent(key, k -> new HashMap<>());
        for (Map.Entry<Item, Integer> entry : diff.entrySet()) {
            acc.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<Item, ItemStack> entry : reps.entrySet()) {
            repAcc.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public static void flushAccumulator(ServerPlayer player, BlockPos pos) {
        String key = snapshotKey(player, pos);
        Map<Item, Integer> acc = accumulators.remove(key);
        Map<Item, ItemStack> reps = accumulatorReps.remove(key);
        if (acc == null) return;

        String playerName = player.getName().getString();
        for (Map.Entry<Item, Integer> entry : acc.entrySet()) {
            int net = entry.getValue();
            if (net == 0) continue;
            ItemStack sample = reps != null ? reps.get(entry.getKey()) : null;
            if (sample == null) continue;
            sample = sample.copy();
            sample.setCount(Math.abs(net));
            ContainerAccessLog.addEntry(pos, playerName, sample, net < 0);
        }
    }

    public static void onContainerClosed(ServerPlayer player) {
        String uuid = player.getStringUUID();

        AbstractContainerMenu handler = player.containerMenu;
        if (handler != player.inventoryMenu) {
            BlockPos pos = ((ScreenHandlerPos)(Object) handler).lootledger_getPos();
            if (pos != null) {
                flushAccumulator(player, pos);
            }
        }

        pendingPos.remove(uuid);
        pendingInventorySize.remove(uuid);
        snapshots.keySet().removeIf(key -> key.startsWith(uuid));
        accumulators.keySet().removeIf(key -> key.startsWith(uuid));
        accumulatorReps.keySet().removeIf(key -> key.startsWith(uuid));
    }

    public static void updateSnapshotSlotForAll(BlockPos pos, int slotIndex, ItemStack newStack) {
        String posStr = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        for (Map.Entry<String, Map<Integer, ItemStack>> entry : snapshots.entrySet()) {
            if (entry.getKey().contains("@" + posStr)) {
                entry.getValue().put(slotIndex, newStack.copy());
            }
        }
    }

    private static Container getInventory(net.minecraft.world.level.Level world, BlockPos pos, BlockEntity be) {
        if (be instanceof ChestBlockEntity) {
            net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock chestBlock) {
                Container combined = ChestBlock.getContainer(chestBlock, state, world, pos, true);
                if (combined != null) return combined;
            }
        }
        if (be instanceof Container inv) return inv;
        return null;
    }

    private static BlockPos getCanonicalPos(net.minecraft.world.level.Level world, BlockPos pos, BlockEntity be) {
        if (be instanceof ChestBlockEntity) {
            net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock) {
                ChestType chestType = state.getValue(ChestBlock.TYPE);
                if (chestType != ChestType.SINGLE) {
                    net.minecraft.core.Direction facing = state.getValue(ChestBlock.FACING);
                    net.minecraft.core.Direction otherDir =
                            chestType == ChestType.LEFT
                                    ? facing.getClockWise()
                                    : facing.getCounterClockWise();
                    BlockPos otherPos = pos.relative(otherDir);
                    if (otherPos.compareTo(pos) < 0) return otherPos;
                }
            }
        }
        return pos;
    }

    private static boolean isTrackedContainer(BlockEntity be) {
        if (be instanceof net.minecraft.world.level.block.entity.EnderChestBlockEntity) return false;
        return be instanceof Container;
    }

    private static void takeSnapshot(ServerPlayer player, BlockPos pos, Container inventory) {
        String key = snapshotKey(player, pos);
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            snapshot.put(i, inventory.getItem(i).copy());
        }
        snapshots.put(key, snapshot);
    }

    public static String snapshotKey(ServerPlayer player, BlockPos pos) {
        return player.getStringUUID() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
