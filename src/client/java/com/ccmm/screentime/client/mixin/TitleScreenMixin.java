package com.ccmm.screentime.client.mixin;

import com.ccmm.screentime.client.ScreenTimeUi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends net.minecraft.client.gui.screens.Screen {
	private static final Component BUTTON_TEXT = Component.translatable("com.ccmm.screentime.title.button");

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void screentime$addButton(CallbackInfo ci) {
		for (GuiEventListener child : this.children()) {
			if (child instanceof Button b && BUTTON_TEXT.equals(b.getMessage())) {
				return;
			}
		}

		int x = this.width / 2 + 128;
		int y = this.height / 4 + 48 + 72 + 12;

		this.addRenderableWidget(Button.builder(BUTTON_TEXT, b -> {
			if (this.minecraft != null) {
				ScreenTimeUi.openSettings(this);
			}
		}).bounds(x, y, 20, 20).build());
	}
}

