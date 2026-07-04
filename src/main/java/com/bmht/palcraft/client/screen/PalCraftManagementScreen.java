package com.bmht.palcraft.client.screen;

import com.bmht.palcraft.client.PalCraftClientState;
import com.bmht.palcraft.client.network.PalCraftClientNetworking;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PalCraftManagementScreen extends Screen {
    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 218;
    private static final int COLOR_PANEL = 0xF011171C;
    private static final int COLOR_HEADER = 0xFF22313A;
    private static final int COLOR_SECTION = 0xFF182229;
    private static final int COLOR_BORDER = 0xFF365461;
    private static final int COLOR_ACCENT = 0xFF46C2D8;
    private static final int COLOR_WARNING = 0xFFFFC857;
    private static final int COLOR_GOOD = 0xFF7DD66C;

    private int selectedSlot = -1;
    private TextFieldWidget nameField;

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

        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        int leftX = panelX + 12;
        int topY = panelY + 36;

        List<PalCraftClientState.PalSummary> pals = state.pals();
        int visible = Math.min(5, pals.size());
        for (int i = 0; i < visible; i++) {
            PalCraftClientState.PalSummary pal = pals.get(i);
            int y = topY + i * 22;
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("[" + pal.slot() + "] " + displayName(pal)),
                            button -> {
                                selectedSlot = pal.slot();
                                rebuildWidgets();
                            })
                    .dimensions(leftX, y, 118, 20)
                    .build());
        }

        int actionX = panelX + 142;
        int actionY = panelY + 124;
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

        nameField = new TextFieldWidget(textRenderer, actionX, actionY + 48, 148, 18, Text.translatable("screen.palcraft.name"));
        nameField.setMaxLength(32);
        nameField.setText(hasSelected ? selectedPal.customName() : "");
        nameField.setPlaceholder(Text.translatable("screen.palcraft.name_placeholder"));
        nameField.active = hasSelected;
        addDrawableChild(nameField);

        ButtonWidget renameButton = ButtonWidget.builder(Text.translatable("screen.palcraft.rename"), button -> {
                    if (selectedPal() != null && nameField != null) {
                        PalCraftClientNetworking.sendRename(selectedPal().slot(), nameField.getText());
                    }
                })
                .dimensions(actionX, actionY + 70, 148, 20)
                .build();
        renameButton.active = hasSelected;
        addDrawableChild(renameButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.refresh"), button -> PalCraftClientNetworking.requestState())
                .dimensions(panelX + 258, panelY + 190, 62, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        drawPanel(context, panelX, panelY);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 8, 0xFFEAD78A);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.pals").formatted(Formatting.AQUA), panelX + 12, panelY + 18, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.details").formatted(Formatting.YELLOW), panelX + 142, panelY + 18, 0xFFFFFF);

        renderSelectedPal(context, panelX + 142, panelY + 34);
        renderBases(context, panelX + 12, panelY + 164);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext context, int panelX, int panelY) {
        context.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 24, COLOR_HEADER);
        context.fill(panelX + 10, panelY + 30, panelX + 132, panelY + 154, COLOR_SECTION);
        context.fill(panelX + 138, panelY + 30, panelX + 322, panelY + 154, COLOR_SECTION);
        context.fill(panelX + 10, panelY + 160, panelX + 322, panelY + 210, COLOR_SECTION);
        context.fill(panelX, panelY + 24, panelX + PANEL_WIDTH, panelY + 25, COLOR_ACCENT);
    }

    private void renderSelectedPal(DrawContext context, int x, int y) {
        PalCraftClientState.PalSummary pal = selectedPal();
        if (pal == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_pals"), x, y, 0xA0A0A0);
            return;
        }

        int healthColor = pal.fainted() ? 0xFF5555 : pal.health() <= pal.maxHealth() * 0.35F ? 0xFFFF55 : 0x55FF55;
        context.drawTextWithShadow(textRenderer, displayName(pal) + (pal.active() ? " " + I18n.translate("screen.palcraft.active_suffix") : ""), x, y, 0x55FFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.level_value", pal.level()) + "  " + localizedElement(pal.element()), x, y + 12, 0xFFFF55);
        drawBar(context, x, y + 25, 148, 6, pal.experience(), pal.nextExperience(), 0xFF23343A, COLOR_GOOD);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.exp_value", pal.experience(), pal.nextExperience()), x, y + 34, 0xA8EFA0);
        drawBar(context, x, y + 47, 148, 6, Math.max(0, pal.health()), pal.maxHealth(), 0xFF3A2222, healthColor | 0xFF000000);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.hp_value", Math.round(pal.health()), Math.round(pal.maxHealth())), x, y + 56, healthColor);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.stats_value", oneDecimal(pal.attack()), oneDecimal(pal.defense())), x, y + 68, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.skills_value", localizedSkills(pal.skills())), x, y + 80, 0xD0D0D0);
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
            context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_line", base.position(), base.radius(), base.assignedCount(), base.queuedTasks()), x, rowY, 0xFFFFFF);
            context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_stock_line", localizedSummary(base.assignments()), localizedSummary(base.stock())), x, rowY + 11, 0xC070FF);
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

    private void drawBar(DrawContext context, int x, int y, int width, int height, float value, float maxValue, int backgroundColor, int fillColor) {
        context.fill(x, y, x + width, y + height, backgroundColor);
        int filled = maxValue <= 0.0F ? 0 : Math.min(width, Math.round(width * (value / maxValue)));
        context.fill(x, y, x + filled, y + height, fillColor);
    }

    private String displayName(PalCraftClientState.PalSummary pal) {
        if (!pal.customName().isBlank()) {
            return pal.customName();
        }
        return I18n.translate(pal.speciesTranslationKey());
    }

    private String localizedElement(String element) {
        return I18n.translate("element.palcraft." + element);
    }

    private String localizedSkills(String skills) {
        if (skills == null || skills.isBlank() || "none".equals(skills)) {
            return I18n.translate("screen.palcraft.none");
        }
        String[] ids = skills.split(", ");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String id : ids) {
            names.add(I18n.translate("skill.palcraft." + id));
        }
        return String.join(", ", names);
    }

    private String localizedSummary(String summary) {
        if (summary == null || summary.isBlank() || "none".equals(summary)) {
            return I18n.translate("screen.palcraft.none");
        }
        String[] entries = summary.split(", ");
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                names.add(I18n.translate("work.palcraft." + parts[0]) + ":" + parts[1]);
            }
        }
        return names.isEmpty() ? I18n.translate("screen.palcraft.none") : String.join(", ", names);
    }
}
