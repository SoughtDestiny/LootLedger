package loot.ledger;

import loot.ledger.network.LootLedgerPackets;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LootLedger implements ModInitializer {
	public static final String MOD_ID = "lootledger";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final LootLedgerServerConfig CONFIG = new LootLedgerServerConfig();

	@Override
	public void onInitialize() {
		LOGGER.info("[LootLedger] Initialized.");
		CONFIG.load();
		LootLedgerEvents.register();
		LootLedgerPackets.registerServer();
		LootLedgerSaveData.register();
	}
}
