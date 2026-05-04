package loot.ledger;

import loot.ledger.gui.HistoryOverlayScreen;
import loot.ledger.network.LootLedgerPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class LootLedgerClient implements ClientModInitializer {

	public static List<HistoryOverlayScreen.ClientLogEntry> pendingEntries = new ArrayList<>();
	public static BlockPos pendingPos = null;
	public static int currentMaxEntries = 128;
	public static boolean canEditConfig = true;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(
				LootLedgerPackets.ContainerOpenedPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						pendingPos = null;
						if (context.client().screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
							if (screen instanceof LootLedgerScreen lootScreen) {
								lootScreen.setLootLedgerPos(payload.pos());
							}
						}
						pendingPos = payload.pos();
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				LootLedgerPackets.ConfigSyncPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						currentMaxEntries = payload.maxEntries();
						canEditConfig = payload.canEdit();
					});
				}
		);

		ClientPlayNetworking.registerGlobalReceiver(
				LootLedgerPackets.LogResponsePayload.TYPE,
				(payload, context) -> {
					List<HistoryOverlayScreen.ClientLogEntry> entries = new ArrayList<>();

					for (int i = 0; i < payload.players().size(); i++) {
						entries.add(new HistoryOverlayScreen.ClientLogEntry(
								payload.players().get(i),
								payload.itemIds().get(i),
								payload.itemNames().get(i),
								payload.counts().get(i),
								payload.removed().get(i) == 1,
								payload.timestamps().get(i)
						));
					}

					pendingEntries = entries;
					pendingPos = payload.pos();

					context.client().execute(() ->
							context.client().setScreen(new HistoryOverlayScreen(entries, payload.pos()))
					);
				}
		);
	}
}
