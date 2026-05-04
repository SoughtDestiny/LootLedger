package loot.ledger.mixin.client;

import loot.ledger.LootLedgerClient;
import loot.ledger.LootLedgerScreen;
import loot.ledger.network.LootLedgerPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin<T extends AbstractContainerMenu> extends Screen implements LootLedgerScreen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    private Button lootLedgerButton;
    private BlockPos lootLedgerPos;

    protected ContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if ((Object) this instanceof CraftingScreen) return;

        lootLedgerPos = null;

        if (LootLedgerClient.pendingPos != null) {
            lootLedgerPos = LootLedgerClient.pendingPos;
            LootLedgerClient.pendingPos = null;
        }

        lootLedgerButton = Button.builder(Component.empty(), btn -> {
                    if (lootLedgerPos != null) {
                        ClientPlayNetworking.send(new LootLedgerPackets.RequestLogPayload(lootLedgerPos));
                    }
                })
                .bounds(0, 0, 20, 20)
                .tooltip(Tooltip.create(Component.literal("Show Container History")))
                .build();

        lootLedgerButton.visible = lootLedgerPos != null;
        this.addRenderableWidget(lootLedgerButton);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof CraftingScreen) return;
        if (lootLedgerButton == null) return;

        lootLedgerButton.setX(this.leftPos + this.imageWidth + 4);
        lootLedgerButton.setY(this.topPos + this.imageHeight - 76);

        if (lootLedgerPos != null) {
            lootLedgerButton.visible = true;
            ctx.item(
                    new ItemStack(Items.WRITABLE_BOOK),
                    lootLedgerButton.getX() + 2,
                    lootLedgerButton.getY() + 2
            );
        } else {
            lootLedgerButton.visible = false;
        }
    }

    @Override
    public void setLootLedgerPos(BlockPos pos) {
        this.lootLedgerPos = pos;
        if (lootLedgerButton != null) {
            lootLedgerButton.visible = true;
        }
    }
}
