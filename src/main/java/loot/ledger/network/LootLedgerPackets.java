package loot.ledger.network;

import loot.ledger.ContainerAccessLog;
import loot.ledger.LootLedgerConfig;
import loot.ledger.LootLedgerSaveData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.ArrayList;
import java.util.List;

public class LootLedgerPackets {

    public record ConfigUpdatePayload(int maxEntries) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigUpdatePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lootledger", "config_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigUpdatePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ConfigUpdatePayload::maxEntries,
                ConfigUpdatePayload::new
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ConfigSyncPayload(int maxEntries, boolean canEdit) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lootledger", "config_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ConfigSyncPayload::maxEntries,
                ByteBufCodecs.BOOL, ConfigSyncPayload::canEdit,
                ConfigSyncPayload::new
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestLogPayload(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestLogPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lootledger", "request_log"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestLogPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, RequestLogPayload::pos,
                RequestLogPayload::new
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ContainerOpenedPayload(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ContainerOpenedPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lootledger", "container_opened"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ContainerOpenedPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ContainerOpenedPayload::pos,
                ContainerOpenedPayload::new
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record LogResponsePayload(
            BlockPos pos,
            List<String> players,
            List<String> itemIds,
            List<String> itemNames,
            List<Integer> counts,
            List<Integer> removed,
            List<Long> timestamps
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<LogResponsePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lootledger", "log_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LogResponsePayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,                                            LogResponsePayload::pos,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),           LogResponsePayload::players,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),           LogResponsePayload::itemIds,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),           LogResponsePayload::itemNames,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),              LogResponsePayload::counts,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),              LogResponsePayload::removed,
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()),             LogResponsePayload::timestamps,
                LogResponsePayload::new
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static boolean canPlayerEdit(ServerPlayer player, net.minecraft.server.MinecraftServer server) {
        return server.isSingleplayer()
                || player.permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public static void registerServer() {
        PayloadTypeRegistry.serverboundPlay().register(RequestLogPayload.TYPE, RequestLogPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ContainerOpenedPayload.TYPE, ContainerOpenedPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LogResponsePayload.TYPE, LogResponsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConfigUpdatePayload.TYPE, ConfigUpdatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            sender.sendPacket(new ConfigSyncPayload(
                    LootLedgerConfig.getMaxEntries(), canPlayerEdit(player, server)));
        });

        ServerPlayNetworking.registerGlobalReceiver(ConfigUpdatePayload.TYPE, (payload, context) -> {
            net.minecraft.server.MinecraftServer server = context.server();
            if (canPlayerEdit(context.player(), server)) {
                int value = payload.maxEntries();
                LootLedgerConfig.setMaxEntries(value);
                LootLedgerSaveData.save();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(p, new ConfigSyncPayload(
                            LootLedgerConfig.getMaxEntries(), canPlayerEdit(p, server)));
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestLogPayload.TYPE, (payload, context) -> {
            BlockPos pos = payload.pos();
            List<ContainerAccessLog.LogEntry> entries = ContainerAccessLog.getEntries(pos);

            List<String>  players    = new ArrayList<>();
            List<String>  itemIds    = new ArrayList<>();
            List<String>  itemNames  = new ArrayList<>();
            List<Integer> counts     = new ArrayList<>();
            List<Integer> removed    = new ArrayList<>();
            List<Long>    timestamps = new ArrayList<>();

            for (ContainerAccessLog.LogEntry entry : entries) {
                players.add(entry.playerName);
                itemIds.add(entry.itemId);
                itemNames.add(entry.itemName);
                counts.add(entry.count);
                removed.add(entry.removed ? 1 : 0);
                timestamps.add(entry.timestamp);
            }

            context.responseSender().sendPacket(
                    new LogResponsePayload(pos, players, itemIds, itemNames, counts, removed, timestamps)
            );
        });
    }
}
