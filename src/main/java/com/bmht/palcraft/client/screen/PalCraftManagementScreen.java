package com.bmht.palcraft.client.screen;

import com.bmht.palcraft.client.PalCraftClientState;
import com.bmht.palcraft.client.network.PalCraftClientNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PalCraftManagementScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 224;
    private static final int COLOR_PANEL = 0xF00E1418;
    private static final int COLOR_HEADER = 0xFF1B2A31;
    private static final int COLOR_SECTION = 0xFF142026;
    private static final int COLOR_BORDER = 0xFF35515D;
    private static final int COLOR_ACCENT = 0xFF49BCD0;
    private static final int COLOR_MUTED = 0xFFA8B0B4;
    private static final int COLOR_GOOD = 0xFF79D36A;

    private final Mode mode;
    private String selectedBaseUuid;
    private BaseTab selectedTab = BaseTab.OVERVIEW;
    private int selectedSlot = -1;
    private int selectedBasePalSlot = -1;
    private TextFieldWidget nameField;

    private PalCraftManagementScreen(Mode mode, String selectedBaseUuid) {
        super(mode == Mode.BASE ? Text.translatable("screen.palcraft.base_management") : Text.translatable("screen.palcraft.player_pals"));
        this.mode = mode;
        this.selectedBaseUuid = selectedBaseUuid == null ? "" : selectedBaseUuid;
    }

    public static PalCraftManagementScreen player() {
        return new PalCraftManagementScreen(Mode.PLAYER, "");
    }

    public static PalCraftManagementScreen base(String baseUuid) {
        return new PalCraftManagementScreen(Mode.BASE, baseUuid);
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
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;

        if (mode == Mode.PLAYER) {
            rebuildPlayerWidgets(panelX, panelY);
        } else {
            rebuildBaseWidgets(panelX, panelY);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.refresh"), button -> PalCraftClientNetworking.requestState())
                .dimensions(panelX + PANEL_WIDTH - 72, panelY + PANEL_HEIGHT - 26, 60, 18)
                .build());
    }

    private void rebuildPlayerWidgets(int panelX, int panelY) {
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        if (selectedSlot < 0 && !state.pals().isEmpty()) {
            selectedSlot = state.pals().get(0).slot();
        }

        int leftX = panelX + 14;
        int listY = panelY + 48;
        for (int i = 0; i < state.carryLimit(); i++) {
            PalCraftClientState.PalSummary pal = i < state.pals().size() ? state.pals().get(i) : null;
            int slot = i;
            Text label = pal == null
                    ? Text.translatable("screen.palcraft.empty_slot", i + 1)
                    : Text.literal((i + 1) + ". " + displayName(pal));
            ButtonWidget button = ButtonWidget.builder(label, ignored -> {
                        if (pal != null) {
                            selectedSlot = slot;
                            rebuildWidgets();
                        }
                    })
                    .dimensions(leftX, listY + i * 24, 124, 20)
                    .build();
            button.active = pal != null;
            addDrawableChild(button);
        }

        int actionX = panelX + 158;
        int actionY = panelY + 132;
        PalCraftClientState.PalSummary selectedPal = selectedPal();
        boolean hasSelected = selectedPal != null;
        ButtonWidget summonButton = ButtonWidget.builder(Text.translatable("screen.palcraft.summon"), button -> {
                    if (selectedPal() != null) {
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_SUMMON, selectedPal().slot());
                    }
                })
                .dimensions(actionX, actionY, 74, 18)
                .build();
        summonButton.active = hasSelected && !selectedPal.active() && !selectedPal.fainted();
        addDrawableChild(summonButton);

        ButtonWidget recallButton = ButtonWidget.builder(Text.translatable("screen.palcraft.recall"), button ->
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_RECALL, -1))
                .dimensions(actionX + 82, actionY, 74, 18)
                .build();
        recallButton.active = hasSelected && selectedPal.active();
        addDrawableChild(recallButton);

        nameField = new TextFieldWidget(textRenderer, actionX, actionY + 26, 156, 18, Text.translatable("screen.palcraft.name"));
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
                .dimensions(actionX, actionY + 50, 156, 18)
                .build();
        renameButton.active = hasSelected;
        addDrawableChild(renameButton);
    }

    private void rebuildBaseWidgets(int panelX, int panelY) {
        PalCraftClientState.BaseSummary base = selectedBase();
        if (base == null && !PalCraftClientState.latestState().bases().isEmpty()) {
            base = PalCraftClientState.latestState().bases().get(0);
            selectedBaseUuid = base.baseUuid();
        }

        int tabX = panelX + 12;
        for (BaseTab tab : BaseTab.values()) {
            ButtonWidget tabButton = ButtonWidget.builder(Text.translatable(tab.translationKey), button -> {
                        selectedTab = tab;
                        rebuildWidgets();
                    })
                    .dimensions(tabX, panelY + 29, 78, 18)
                    .build();
            tabButton.active = selectedTab != tab;
            addDrawableChild(tabButton);
            tabX += 84;
        }

        if (base == null) {
            return;
        }

        List<PalCraftClientState.BasePalSummary> visiblePals = visibleBasePals(base);
        if ((selectedBasePalSlot < 0 || visiblePals.stream().noneMatch(pal -> pal.slot() == selectedBasePalSlot)) && !visiblePals.isEmpty()) {
            selectedBasePalSlot = visiblePals.get(0).slot();
        }

        if (selectedTab == BaseTab.WORK || selectedTab == BaseTab.PETS) {
            int listX = panelX + 14;
            int listY = panelY + 60;
            List<PalCraftClientState.BasePalSummary> pals = visiblePals;
            for (int i = 0; i < Math.min(6, pals.size()); i++) {
                PalCraftClientState.BasePalSummary pal = pals.get(i);
                Text label = Text.literal((pal.slot() + 1) + ". " + displayName(pal) + (pal.deployed() ? " *" : ""));
                addDrawableChild(ButtonWidget.builder(label, ignored -> {
                            selectedBasePalSlot = pal.slot();
                            rebuildWidgets();
                        })
                        .dimensions(listX, listY + i * 22, 132, 18)
                        .build());
            }

            PalCraftClientState.BasePalSummary selectedPal = selectedBasePal(base);
            if (selectedTab == BaseTab.WORK && selectedPal != null) {
                addWorkButtons(base, selectedPal, panelX + 164, panelY + 72);
            }
            if (selectedTab == BaseTab.PETS && selectedPal != null) {
                String baseUuid = base.baseUuid();
                String palUuid = selectedPal.instanceUuid();
                if (selectedPal.deployed()) {
                    addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.recall_to_storage"), button ->
                                    PalCraftClientNetworking.sendBaseRecall(baseUuid, palUuid))
                            .dimensions(panelX + 164, panelY + 144, 132, 18)
                            .build());
                } else {
                    addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.deploy_to_base"), button ->
                                    PalCraftClientNetworking.sendBaseDeploy(baseUuid, selectedPal.slot()))
                            .dimensions(panelX + 164, panelY + 144, 132, 18)
                            .build());
                }
                ButtonWidget unassignButton = ButtonWidget.builder(Text.translatable("screen.palcraft.unassign"), button ->
                                PalCraftClientNetworking.sendBaseUnassign(baseUuid, palUuid))
                        .dimensions(panelX + 164, panelY + 168, 132, 18)
                        .build();
                unassignButton.active = selectedPal.assigned();
                addDrawableChild(unassignButton);
            }
        }
    }

    private void addWorkButtons(PalCraftClientState.BaseSummary base, PalCraftClientState.BasePalSummary pal, int x, int y) {
        String[] workTypes = {"mining", "logging", "planting", "hauling", "manufacturing"};
        for (int i = 0; i < workTypes.length; i++) {
            String workType = workTypes[i];
            ButtonWidget button = ButtonWidget.builder(Text.translatable("work.palcraft." + workType), ignored ->
                            PalCraftClientNetworking.sendBaseAssign(base.baseUuid(), pal.slot(), workType))
                    .dimensions(x + (i % 2) * 76, y + (i / 2) * 24, 68, 18)
                    .build();
            button.active = pal.deployed() && !pal.assigned() && !"hauling".equals(workType) && !"manufacturing".equals(workType);
            addDrawableChild(button);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        drawPanel(context, panelX, panelY);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 9, 0xFFEAD78A);
        if (mode == Mode.PLAYER) {
            renderPlayer(context, panelX, panelY);
        } else {
            renderBase(context, panelX, panelY);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext context, int panelX, int panelY) {
        context.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 24, COLOR_HEADER);
        context.fill(panelX, panelY + 24, panelX + PANEL_WIDTH, panelY + 25, COLOR_ACCENT);
        context.fill(panelX + 10, panelY + 44, panelX + 148, panelY + 196, COLOR_SECTION);
        context.fill(panelX + 154, panelY + 44, panelX + 346, panelY + 196, COLOR_SECTION);
    }

    private void renderPlayer(DrawContext context, int panelX, int panelY) {
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.carried_count", state.pals().size(), state.carryLimit()).formatted(Formatting.AQUA), panelX + 14, panelY + 31, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.details").formatted(Formatting.YELLOW), panelX + 158, panelY + 31, 0xFFFFFF);
        renderSelectedPal(context, panelX + 164, panelY + 55);
    }

    private void renderBase(DrawContext context, int panelX, int panelY) {
        PalCraftClientState.BaseSummary base = selectedBase();
        if (base == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_bases"), panelX + 14, panelY + 58, COLOR_MUTED);
            return;
        }

        if (selectedTab == BaseTab.OVERVIEW) {
            renderBaseOverview(context, base, panelX + 18, panelY + 58);
        } else if (selectedTab == BaseTab.WORK) {
            renderBaseWork(context, base, panelX, panelY);
        } else {
            renderBasePets(context, base, panelX, panelY);
        }
    }

    private void renderBaseOverview(DrawContext context, PalCraftClientState.BaseSummary base, int x, int y) {
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_position", base.position()), x, y, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_radius", base.radius()), x, y + 16, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_storage_blocks", base.storageBlockCount()), x, y + 32, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_pal_counts", base.storedCount(), base.assignedCount()), x, y + 48, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_tasks", base.queuedTasks()), x, y + 64, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.base_work_summary", localizedSummary(base.assignments())), x, y + 80, 0xC8F0FF);
    }

    private void renderBaseWork(DrawContext context, PalCraftClientState.BaseSummary base, int panelX, int panelY) {
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.pet_storage").formatted(Formatting.AQUA), panelX + 14, panelY + 49, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.work_assignment").formatted(Formatting.YELLOW), panelX + 164, panelY + 49, 0xFFFFFF);
        PalCraftClientState.BasePalSummary pal = selectedBasePal(base);
        if (pal == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_deployed_pals"), panelX + 164, panelY + 72, COLOR_MUTED);
            return;
        }
        renderBasePalDetails(context, pal, panelX + 164, panelY + 104);
    }

    private void renderBasePets(DrawContext context, PalCraftClientState.BaseSummary base, int panelX, int panelY) {
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.pet_storage").formatted(Formatting.AQUA), panelX + 14, panelY + 49, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.details").formatted(Formatting.YELLOW), panelX + 164, panelY + 49, 0xFFFFFF);
        PalCraftClientState.BasePalSummary pal = selectedBasePal(base);
        if (pal == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_base_pals"), panelX + 164, panelY + 72, COLOR_MUTED);
            return;
        }
        renderBasePalDetails(context, pal, panelX + 164, panelY + 72);
    }

    private void renderSelectedPal(DrawContext context, int x, int y) {
        PalCraftClientState.PalSummary pal = selectedPal();
        if (pal == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("screen.palcraft.no_pals"), x, y, COLOR_MUTED);
            return;
        }

        int healthColor = pal.fainted() ? 0xFF5555 : pal.health() <= pal.maxHealth() * 0.35F ? 0xFFFF55 : 0x55FF55;
        context.drawTextWithShadow(textRenderer, displayName(pal) + (pal.active() ? " " + I18n.translate("screen.palcraft.active_suffix") : ""), x, y, 0x55FFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.level_value", pal.level()) + "  " + localizedElement(pal.element()), x, y + 14, 0xFFFF55);
        drawBar(context, x, y + 30, 156, 6, pal.experience(), pal.nextExperience(), 0xFF23343A, COLOR_GOOD);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.exp_value", pal.experience(), pal.nextExperience()), x, y + 40, 0xA8EFA0);
        drawBar(context, x, y + 58, 156, 6, Math.max(0, pal.health()), pal.maxHealth(), 0xFF3A2222, healthColor | 0xFF000000);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.hp_value", Math.round(pal.health()), Math.round(pal.maxHealth())), x, y + 68, healthColor);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.stats_value", oneDecimal(pal.attack()), oneDecimal(pal.defense())), x, y + 82, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.skills_value", localizedSkills(pal.skills())), x, y + 96, 0xD0D0D0);
    }

    private void renderBasePalDetails(DrawContext context, PalCraftClientState.BasePalSummary pal, int x, int y) {
        int healthColor = pal.health() <= 0.0F ? 0xFF5555 : pal.health() <= pal.maxHealth() * 0.35F ? 0xFFFF55 : 0x55FF55;
        context.drawTextWithShadow(textRenderer, displayName(pal), x, y, 0x55FFFF);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.level_value", pal.level()) + "  " + localizedElement(pal.element()), x, y + 14, 0xFFFF55);
        drawBar(context, x, y + 30, 156, 6, Math.max(0, pal.health()), pal.maxHealth(), 0xFF3A2222, healthColor | 0xFF000000);
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.hp_value", Math.round(pal.health()), Math.round(pal.maxHealth())), x, y + 40, healthColor);
        String status = pal.assigned() ? I18n.translate("work.palcraft." + pal.workType()) : I18n.translate("screen.palcraft.unassigned");
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.assignment_status", status), x, y + 56, 0xFFFFFF);
        String deployed = pal.deployed() ? I18n.translate("screen.palcraft.deployed") : I18n.translate("screen.palcraft.in_storage");
        context.drawTextWithShadow(textRenderer, I18n.translate("screen.palcraft.deploy_status", deployed), x, y + 70, 0xFFFFFF);
    }

    private PalCraftClientState.PalSummary selectedPal() {
        for (PalCraftClientState.PalSummary pal : PalCraftClientState.latestState().pals()) {
            if (pal.slot() == selectedSlot) {
                return pal;
            }
        }
        return null;
    }

    private PalCraftClientState.BaseSummary selectedBase() {
        for (PalCraftClientState.BaseSummary base : PalCraftClientState.latestState().bases()) {
            if (base.baseUuid().equals(selectedBaseUuid)) {
                return base;
            }
        }
        return null;
    }

    private PalCraftClientState.BasePalSummary selectedBasePal(PalCraftClientState.BaseSummary base) {
        for (PalCraftClientState.BasePalSummary pal : visibleBasePals(base)) {
            if (pal.slot() == selectedBasePalSlot) {
                return pal;
            }
        }
        return null;
    }

    private List<PalCraftClientState.BasePalSummary> visibleBasePals(PalCraftClientState.BaseSummary base) {
        if (selectedTab != BaseTab.WORK) {
            return base.storedPals();
        }
        return base.storedPals().stream()
                .filter(PalCraftClientState.BasePalSummary::deployed)
                .toList();
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

    private String displayName(PalCraftClientState.BasePalSummary pal) {
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

    private enum Mode {
        PLAYER,
        BASE
    }

    private enum BaseTab {
        OVERVIEW("screen.palcraft.tab_overview"),
        WORK("screen.palcraft.tab_work"),
        PETS("screen.palcraft.tab_pets");

        private final String translationKey;

        BaseTab(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
