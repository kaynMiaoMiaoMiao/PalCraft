package com.bmht.palcraft.client.screen;

import com.bmht.palcraft.client.PalCraftClientState;
import com.bmht.palcraft.client.network.PalCraftClientNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PalCraftManagementScreen extends Screen {
    private int selectedSlot = -1;

    public PalCraftManagementScreen() {
        super(Text.translatable("screen.palcraft.management"));
    }

    @Override
    protected void init() {
        rebuildWidgets();
        PalCraftClientNetworking.requestState();
    }

    public void refreshFromState() {
        if (client != null && client.currentScreen == this) {
            rebuildWidgets();
        }
    }

    private void rebuildWidgets() {
        clearChildren();
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        if (selectedSlot < 0 && !state.pals().isEmpty()) {
            selectedSlot = state.pals().get(0).slot();
        }

        int panelX = width / 2 - 150;
        int panelY = height / 2 - 92;
        int leftX = panelX + 12;
        int topY = panelY + 28;

        List<PalCraftClientState.PalSummary> pals = state.pals();
        int visible = Math.min(7, pals.size());
        for (int i = 0; i < visible; i++) {
            PalCraftClientState.PalSummary pal = pals.get(i);
            int y = topY + i * 22;
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("[" + pal.slot() + "] " + pal.displayName()),
                            button -> {
                                selectedSlot = pal.slot();
                                rebuildWidgets();
                            })
                    .dimensions(leftX, y, 118, 20)
                    .build());
        }

        int actionX = panelX + 142;
        int actionY = panelY + 130;
        PalCraftClientState.PalSummary selectedPal = selectedPal();
        boolean hasSelected = selectedPal != null;
        ButtonWidget summonButton = ButtonWidget.builder(Text.translatable("screen.palcraft.summon"), button -> {
                    if (selectedPal() != null) {
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_SUMMON, selectedPal().slot());
                    }
                })
                .dimensions(actionX, actionY, 70, 20)
                .build();
        summonButton.active = hasSelected && !selectedPal.active() && !selectedPal.fainted();
        addDrawableChild(summonButton);

        ButtonWidget recallButton = ButtonWidget.builder(Text.translatable("screen.palcraft.recall"), button ->
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_RECALL, -1))
                .dimensions(actionX + 78, actionY, 70, 20)
                .build();
        recallButton.active = hasSelected && selectedPal.active();
        addDrawableChild(recallButton);

        ButtonWidget assignButton = ButtonWidget.builder(Text.translatable("screen.palcraft.assign"), button -> {
                    if (selectedPal() != null) {
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_ASSIGN_AUTO, selectedPal().slot());
                    }
                })
                .dimensions(actionX, actionY + 24, 148, 20)
                .build();
        assignButton.active = hasSelected && !selectedPal.active();
        addDrawableChild(assignButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.refresh"), button -> PalCraftClientNetworking.requestState())
                .dimensions(panelX + 226, panelY + 170, 62, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelX = width / 2 - 150;
        int panelY = height / 2 - 92;
        context.fill(panelX, panelY, panelX + 300, panelY + 196, 0xE6101418);
        context.fill(panelX, panelY, panelX + 300, panelY + 24, 0xFF1E2B32);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 8, 0xFFD76B);

        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.pals").formatted(Formatting.AQUA), panelX + 12, panelY + 32 - 16, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.details").formatted(Formatting.YELLOW), panelX + 142, panelY + 32 - 16, 0xFFFFFF);

        renderSelectedPal(context, panelX + 142, panelY + 34);
        renderBases(context, panelX + 12, panelY + 188 - 46);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSelectedPal(DrawContext context, int x, int y) {
        PalCraftClientState.PalSummary pal = selectedPal();
        if (pal == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_pals"), x, y, 0xA0A0A0);
            return;
        }

        int healthColor = pal.fainted() ? 0xFF5555 : pal.health() <= pal.maxHealth() * 0.35F ? 0xFFFF55 : 0x55FF55;
        context.drawTextWithShadow(textRenderer, pal.displayName() + (pal.active() ? " *" : ""), x, y, 0x55FFFF);
        context.drawTextWithShadow(textRenderer, "Lv." + pal.level() + "  " + pal.element(), x, y + 12, 0xFFFF55);
        context.drawTextWithShadow(textRenderer, "EXP " + pal.experience() + "/" + pal.nextExperience(), x, y + 24, 0x55FF55);
        context.drawTextWithShadow(textRenderer, "HP " + Math.round(pal.health()) + "/" + Math.round(pal.maxHealth()), x, y + 36, healthColor);
        context.drawTextWithShadow(textRenderer, "ATK " + oneDecimal(pal.attack()) + "  DEF " + oneDecimal(pal.defense()), x, y + 48, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Skills: " + pal.skills(), x, y + 60, 0xD0D0D0);
    }

    private void renderBases(DrawContext context, int x, int y) {
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.bases").formatted(Formatting.LIGHT_PURPLE), x, y, 0xFFFFFF);
        List<PalCraftClientState.BaseSummary> bases = PalCraftClientState.latestState().bases();
        if (bases.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_bases"), x, y + 12, 0xA0A0A0);
            return;
        }

        int count = Math.min(2, bases.size());
        for (int i = 0; i < count; i++) {
            PalCraftClientState.BaseSummary base = bases.get(i);
            int rowY = y + 12 + i * 24;
            context.drawTextWithShadow(textRenderer, base.position() + "  R" + base.radius() + "  Pals " + base.assignedCount() + "  Q " + base.queuedTasks(), x, rowY, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, "Work " + base.assignments() + "  Stock " + base.stock(), x, rowY + 11, 0xC070FF);
        }
    }

    private PalCraftClientState.PalSummary selectedPal() {
        for (PalCraftClientState.PalSummary pal : PalCraftClientState.latestState().pals()) {
            if (pal.slot() == selectedSlot) {
                return pal;
            }
        }
        return null;
    }

    private static String oneDecimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
