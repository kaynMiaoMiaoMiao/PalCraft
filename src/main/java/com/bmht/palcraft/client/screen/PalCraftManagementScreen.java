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
    private static final int PANEL_WIDTH = 432;
    private static final int PANEL_HEIGHT = 258;
    private static final int MARGIN = 14;
    private static final int GAP = 12;
    private static final int LIST_WIDTH = 150;
    private static final int PLAYER_CONTENT_TOP = 48;
    private static final int BASE_CONTENT_TOP = 62;
    private static final int SECTION_BOTTOM_MARGIN = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int ROW_GAP = 4;
    private static final int REFRESH_WIDTH = 60;

    private static final int COLOR_BACKDROP = 0x9A000000;
    private static final int COLOR_SHADOW = 0xA0000000;
    private static final int COLOR_PANEL = 0xF2131A1F;
    private static final int COLOR_HEADER = 0xFF1B2830;
    private static final int COLOR_HEADER_DARK = 0xFF132027;
    private static final int COLOR_SECTION = 0xFF18232A;
    private static final int COLOR_SECTION_ALT = 0xFF111B21;
    private static final int COLOR_BORDER = 0xFF3C5864;
    private static final int COLOR_BORDER_SOFT = 0xFF263941;
    private static final int COLOR_ACCENT = 0xFF4BC6D6;
    private static final int COLOR_ACCENT_DIM = 0x664BC6D6;
    private static final int COLOR_TEXT = 0xFFE8EEF0;
    private static final int COLOR_TEXT_MUTED = 0xFF9BA8AD;
    private static final int COLOR_GOLD = 0xFFE5C86B;
    private static final int COLOR_GOOD = 0xFF7DD56D;
    private static final int COLOR_WARN = 0xFFFFC85A;
    private static final int COLOR_BAD = 0xFFFF6961;
    private static final int COLOR_EXP = 0xFF8FEA86;

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
        nameField = null;

        int panelX = panelX();
        int panelY = panelY();

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.refresh"), button -> PalCraftClientNetworking.requestState())
                .dimensions(panelX + PANEL_WIDTH - MARGIN - REFRESH_WIDTH, panelY + 8, REFRESH_WIDTH, 18)
                .build());

        if (mode == Mode.PLAYER) {
            rebuildPlayerWidgets(panelX, panelY);
        } else {
            rebuildBaseWidgets(panelX, panelY);
        }
    }

    private void rebuildPlayerWidgets(int panelX, int panelY) {
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        if (selectedSlot < 0 && !state.pals().isEmpty()) {
            selectedSlot = state.pals().get(0).slot();
        }

        int listX = listInnerX(panelX);
        int listY = panelY + PLAYER_CONTENT_TOP + 28;
        int rowWidth = listInnerWidth();
        for (int i = 0; i < state.carryLimit(); i++) {
            PalCraftClientState.PalSummary pal = i < state.pals().size() ? state.pals().get(i) : null;
            int slot = i;
            Text label = pal == null
                    ? Text.translatable("screen.palcraft.empty_slot", i + 1)
                    : Text.literal(listLabel(i + 1, displayName(pal), pal.level(), pal.active(), rowWidth - 14));
            ButtonWidget button = ButtonWidget.builder(label, ignored -> {
                        if (pal != null) {
                            selectedSlot = slot;
                            rebuildWidgets();
                        }
                    })
                    .dimensions(listX, listY + i * (ROW_HEIGHT + ROW_GAP), rowWidth, ROW_HEIGHT)
                    .build();
            button.active = pal != null;
            addDrawableChild(button);
        }

        PalCraftClientState.PalSummary selectedPal = selectedPal();
        boolean hasSelected = selectedPal != null;
        int rightX = detailInnerX(panelX);
        int rightY = panelY + 208;
        int rightWidth = detailInnerWidth();

        ButtonWidget summonButton = ButtonWidget.builder(Text.translatable("screen.palcraft.summon"), button -> {
                    PalCraftClientState.PalSummary pal = selectedPal();
                    if (pal != null) {
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_SUMMON, pal.slot());
                    }
                })
                .dimensions(rightX, rightY, 72, 18)
                .build();
        summonButton.active = hasSelected && !selectedPal.active() && !selectedPal.fainted();
        addDrawableChild(summonButton);

        ButtonWidget recallButton = ButtonWidget.builder(Text.translatable("screen.palcraft.recall"), button ->
                        PalCraftClientNetworking.sendAction(PalCraftClientNetworking.ACTION_RECALL, -1))
                .dimensions(rightX + 78, rightY, 72, 18)
                .build();
        recallButton.active = hasSelected && selectedPal.active();
        addDrawableChild(recallButton);

        nameField = new TextFieldWidget(textRenderer, rightX, rightY + 24, rightWidth - 84, 18, Text.translatable("screen.palcraft.name"));
        nameField.setMaxLength(32);
        nameField.setText(hasSelected ? selectedPal.customName() : "");
        nameField.setPlaceholder(Text.translatable("screen.palcraft.name_placeholder"));
        nameField.active = hasSelected;
        addDrawableChild(nameField);

        ButtonWidget renameButton = ButtonWidget.builder(Text.translatable("screen.palcraft.rename"), button -> {
                    PalCraftClientState.PalSummary pal = selectedPal();
                    if (pal != null && nameField != null) {
                        PalCraftClientNetworking.sendRename(pal.slot(), nameField.getText());
                    }
                })
                .dimensions(rightX + rightWidth - 78, rightY + 24, 78, 18)
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

        int tabX = panelX + MARGIN;
        int tabY = panelY + 34;
        for (BaseTab tab : BaseTab.values()) {
            ButtonWidget tabButton = ButtonWidget.builder(Text.translatable(tab.translationKey), button -> {
                        selectedTab = tab;
                        rebuildWidgets();
                    })
                    .dimensions(tabX, tabY, 86, 18)
                    .build();
            tabButton.active = selectedTab != tab;
            addDrawableChild(tabButton);
            tabX += 92;
        }

        if (base == null) {
            return;
        }

        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        if (state.pals().isEmpty()) {
            selectedSlot = -1;
        } else if (selectedSlot < 0 || state.pals().stream().noneMatch(pal -> pal.slot() == selectedSlot)) {
            selectedSlot = state.pals().get(0).slot();
        }

        List<PalCraftClientState.BasePalSummary> visiblePals = visibleBasePals(base);
        if ((selectedBasePalSlot < 0 || visiblePals.stream().noneMatch(pal -> pal.slot() == selectedBasePalSlot)) && !visiblePals.isEmpty()) {
            selectedBasePalSlot = visiblePals.get(0).slot();
        }

        if (selectedTab == BaseTab.WORK || selectedTab == BaseTab.PETS) {
            int listX = listInnerX(panelX);
            int listY = panelY + BASE_CONTENT_TOP + 28;
            int rowWidth = listInnerWidth();
            for (int i = 0; i < Math.min(6, visiblePals.size()); i++) {
                PalCraftClientState.BasePalSummary pal = visiblePals.get(i);
                Text label = Text.literal(baseListLabel(pal, rowWidth - 14));
                addDrawableChild(ButtonWidget.builder(label, ignored -> {
                            selectedBasePalSlot = pal.slot();
                            rebuildWidgets();
                        })
                        .dimensions(listX, listY + i * (ROW_HEIGHT + ROW_GAP), rowWidth, ROW_HEIGHT)
                        .build());
            }

            PalCraftClientState.BasePalSummary selectedPal = selectedBasePal(base);
            if (selectedTab == BaseTab.WORK && selectedPal != null) {
                addWorkButtons(base, selectedPal, detailInnerX(panelX), panelY + BASE_CONTENT_TOP + 30);
            }
            if (selectedTab == BaseTab.PETS && selectedPal != null) {
                addBasePetButtons(base, selectedPal, detailInnerX(panelX), panelY + 184, detailInnerWidth());
            }
        }
    }

    private void addBasePetButtons(PalCraftClientState.BaseSummary base, PalCraftClientState.BasePalSummary selectedPal, int x, int y, int width) {
        String baseUuid = base.baseUuid();
        String palUuid = selectedPal.instanceUuid();
        int halfWidth = (width - 8) / 2;
        if (selectedPal.deployed()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.palcraft.recall_to_storage"), button ->
                            PalCraftClientNetworking.sendBaseRecall(baseUuid, palUuid))
                    .dimensions(x, y, halfWidth, 18)
                    .build());
        } else {
            ButtonWidget deployButton = ButtonWidget.builder(Text.translatable("screen.palcraft.deploy_to_base"), button ->
                            PalCraftClientNetworking.sendBaseDeploy(baseUuid, selectedPal.slot()))
                    .dimensions(x, y, halfWidth, 18)
                    .build();
            deployButton.active = selectedPal.health() > 0.0F;
            addDrawableChild(deployButton);
        }
        ButtonWidget takeButton = ButtonWidget.builder(Text.translatable("screen.palcraft.take_to_player"), button ->
                        PalCraftClientNetworking.sendTakeBasePal(baseUuid, selectedPal.slot()))
                .dimensions(x + halfWidth + 8, y, halfWidth, 18)
                .build();
        takeButton.active = PalCraftClientState.latestState().pals().size() < PalCraftClientState.latestState().carryLimit();
        addDrawableChild(takeButton);

        ButtonWidget unassignButton = ButtonWidget.builder(Text.translatable("screen.palcraft.unassign"), button ->
                        PalCraftClientNetworking.sendBaseUnassign(baseUuid, palUuid))
                .dimensions(x, y + 24, halfWidth, 18)
                .build();
        unassignButton.active = selectedPal.assigned();
        addDrawableChild(unassignButton);

        addCarriedSlotTransferButtons(baseUuid, x, y + 46, width);
    }

    private void addCarriedSlotTransferButtons(String baseUuid, int x, int y, int width) {
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        int slotWidth = 24;
        int slotGap = 4;
        for (int i = 0; i < state.carryLimit(); i++) {
            PalCraftClientState.PalSummary pal = i < state.pals().size() ? state.pals().get(i) : null;
            int slot = i;
            ButtonWidget slotButton = ButtonWidget.builder(Text.literal(String.valueOf(i + 1)), button -> {
                        if (pal != null) {
                            selectedSlot = slot;
                            rebuildWidgets();
                        }
                    })
                    .dimensions(x + i * (slotWidth + slotGap), y, slotWidth, 18)
                    .build();
            slotButton.active = pal != null && selectedSlot != slot;
            addDrawableChild(slotButton);
        }

        int storeX = x + state.carryLimit() * (slotWidth + slotGap) + 4;
        ButtonWidget storeButton = ButtonWidget.builder(Text.translatable("screen.palcraft.store_in_base"), button -> {
                    PalCraftClientState.PalSummary pal = selectedPal();
                    if (pal != null) {
                        PalCraftClientNetworking.sendStorePlayerPalInBase(baseUuid, pal.slot());
                    }
                })
                .dimensions(storeX, y, Math.max(56, x + width - storeX), 18)
                .build();
        storeButton.active = selectedPal() != null;
        addDrawableChild(storeButton);
    }

    private void addWorkButtons(PalCraftClientState.BaseSummary base, PalCraftClientState.BasePalSummary pal, int x, int y) {
        String[] workTypes = {"mining", "logging", "planting", "hauling", "manufacturing"};
        int buttonWidth = (detailInnerWidth() - 8) / 2;
        for (int i = 0; i < workTypes.length; i++) {
            String workType = workTypes[i];
            ButtonWidget button = ButtonWidget.builder(Text.translatable("work.palcraft." + workType), ignored ->
                            PalCraftClientNetworking.sendBaseAssign(base.baseUuid(), pal.slot(), workType))
                    .dimensions(x + (i % 2) * (buttonWidth + 8), y + (i / 2) * 24, buttonWidth, 18)
                    .build();
            button.active = pal.deployed() && !pal.assigned() && !"hauling".equals(workType) && !"manufacturing".equals(workType);
            addDrawableChild(button);
        }

        ButtonWidget unassignButton = ButtonWidget.builder(Text.translatable("screen.palcraft.unassign"), button ->
                        PalCraftClientNetworking.sendBaseUnassign(base.baseUuid(), pal.instanceUuid()))
                .dimensions(x + buttonWidth + 8, y + 48, buttonWidth, 18)
                .build();
        unassignButton.active = pal.assigned();
        addDrawableChild(unassignButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(0, 0, width, height, COLOR_BACKDROP);

        int panelX = panelX();
        int panelY = panelY();
        drawPanel(context, panelX, panelY);

        drawTitle(context, panelX, panelY);
        if (mode == Mode.PLAYER) {
            renderPlayer(context, panelX, panelY);
        } else {
            renderBase(context, panelX, panelY);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext context, int panelX, int panelY) {
        context.fill(panelX + 4, panelY + 5, panelX + PANEL_WIDTH + 4, panelY + PANEL_HEIGHT + 5, COLOR_SHADOW);
        context.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, COLOR_BORDER);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 31, COLOR_HEADER);
        context.fill(panelX, panelY + 31, panelX + PANEL_WIDTH, panelY + 32, COLOR_ACCENT);
        context.fill(panelX, panelY + 32, panelX + PANEL_WIDTH, panelY + 58, COLOR_HEADER_DARK);

        int contentTop = mode == Mode.BASE ? BASE_CONTENT_TOP : PLAYER_CONTENT_TOP;
        int contentBottom = panelY + PANEL_HEIGHT - SECTION_BOTTOM_MARGIN;
        if (mode == Mode.BASE && selectedTab == BaseTab.OVERVIEW) {
            drawBox(context, panelX + MARGIN, panelY + contentTop, PANEL_WIDTH - MARGIN * 2, contentBottom - (panelY + contentTop), COLOR_SECTION_ALT);
            return;
        }

        drawBox(context, panelX + MARGIN, panelY + contentTop, LIST_WIDTH, contentBottom - (panelY + contentTop), COLOR_SECTION_ALT);
        drawBox(context, detailSectionX(panelX), panelY + contentTop, detailSectionWidth(), contentBottom - (panelY + contentTop), COLOR_SECTION);
    }

    private void drawTitle(DrawContext context, int panelX, int panelY) {
        context.drawTextWithShadow(textRenderer, title.copy().formatted(Formatting.BOLD), panelX + MARGIN, panelY + 11, COLOR_GOLD);
        String modeText = mode == Mode.BASE ? I18n.translate("screen.palcraft.bases") : I18n.translate("screen.palcraft.pals");
        drawBadge(context, panelX + MARGIN + textRenderer.getWidth(title) + 10, panelY + 8, modeText, COLOR_ACCENT_DIM, COLOR_ACCENT);
    }

    private void renderPlayer(DrawContext context, int panelX, int panelY) {
        PalCraftClientState.UiState state = PalCraftClientState.latestState();
        int listX = listSectionX(panelX);
        int listY = panelY + PLAYER_CONTENT_TOP;
        int detailX = detailSectionX(panelX);
        int detailY = panelY + PLAYER_CONTENT_TOP;

        drawSectionTitle(context, Text.translatable("screen.palcraft.carried_count", state.pals().size(), state.carryLimit()), listX + 8, listY + 8, LIST_WIDTH - 16, COLOR_ACCENT);
        drawSectionTitle(context, Text.translatable("screen.palcraft.details"), detailX + 8, detailY + 8, detailSectionWidth() - 16, COLOR_GOLD);
        drawPlayerSelectionAccents(context, panelX, panelY, state);
        renderSelectedPal(context, detailX + 8, detailY + 30, detailSectionWidth() - 16);
    }

    private void renderBase(DrawContext context, int panelX, int panelY) {
        drawSelectedTabAccent(context, panelX, panelY);

        PalCraftClientState.BaseSummary base = selectedBase();
        if (base == null) {
            int x = panelX + MARGIN + 10;
            int y = panelY + BASE_CONTENT_TOP + 14;
            drawTextClipped(context, I18n.translate("screen.palcraft.no_bases"), x, y, PANEL_WIDTH - MARGIN * 2 - 20, COLOR_TEXT_MUTED);
            return;
        }

        if (selectedTab == BaseTab.OVERVIEW) {
            renderBaseOverview(context, base, panelX, panelY);
        } else if (selectedTab == BaseTab.WORK) {
            renderBaseWork(context, base, panelX, panelY);
        } else {
            renderBasePets(context, base, panelX, panelY);
        }
    }

    private void renderBaseOverview(DrawContext context, PalCraftClientState.BaseSummary base, int panelX, int panelY) {
        int sectionX = panelX + MARGIN;
        int sectionY = panelY + BASE_CONTENT_TOP;
        int sectionWidth = PANEL_WIDTH - MARGIN * 2;
        int x = sectionX + 10;
        int y = sectionY + 9;

        drawTextClipped(context, I18n.translate("screen.palcraft.base_position", base.position()), x, y, sectionWidth - 20, COLOR_TEXT);

        int cardY = y + 22;
        int cardWidth = (sectionWidth - 28) / 2;
        drawMetricCard(context, x, cardY, cardWidth, I18n.translate("screen.palcraft.base_metric_radius"), String.valueOf(base.radius()), COLOR_GOLD);
        drawMetricCard(context, x + cardWidth + 8, cardY, cardWidth, I18n.translate("screen.palcraft.base_metric_storage"), String.valueOf(base.storageBlockCount()), COLOR_ACCENT);
        drawMetricCard(context, x, cardY + 48, cardWidth, I18n.translate("screen.palcraft.base_metric_pals"), base.storedCount() + " / " + base.assignedCount(), COLOR_GOOD);
        drawMetricCard(context, x + cardWidth + 8, cardY + 48, cardWidth, I18n.translate("screen.palcraft.base_metric_queue"), String.valueOf(base.queuedTasks()), base.queuedTasks() > 0 ? COLOR_WARN : COLOR_TEXT_MUTED);

        int summaryY = cardY + 106;
        drawSummaryLine(context, x, summaryY, sectionWidth - 20, I18n.translate("screen.palcraft.base_work_summary", localizedWorkSummary(base.assignments())), COLOR_ACCENT);
        drawSummaryLine(context, x, summaryY + 18, sectionWidth - 20, I18n.translate("screen.palcraft.base_stock_summary", base.totalStock(), localizedWorkSummary(base.stock())), 0xFFDCA5FF);
    }

    private void renderBaseWork(DrawContext context, PalCraftClientState.BaseSummary base, int panelX, int panelY) {
        int listX = listSectionX(panelX);
        int listY = panelY + BASE_CONTENT_TOP;
        int detailX = detailSectionX(panelX);
        int detailY = panelY + BASE_CONTENT_TOP;

        drawSectionTitle(context, Text.translatable("screen.palcraft.pet_storage"), listX + 8, listY + 8, LIST_WIDTH - 16, COLOR_ACCENT);
        drawSectionTitle(context, Text.translatable("screen.palcraft.work_assignment"), detailX + 8, detailY + 8, detailSectionWidth() - 16, COLOR_GOLD);
        drawBaseSelectionAccents(context, panelX, panelY, base);

        PalCraftClientState.BasePalSummary pal = selectedBasePal(base);
        if (pal == null) {
            drawTextClipped(context, I18n.translate("screen.palcraft.no_deployed_pals"), detailX + 8, detailY + 34, detailSectionWidth() - 16, COLOR_TEXT_MUTED);
            return;
        }

        if (!pal.deployed()) {
            drawTextClipped(context, I18n.translate("screen.palcraft.deploy_first"), detailX + 8, detailY + 100, detailSectionWidth() - 16, COLOR_TEXT_MUTED);
        }
        renderBasePalDetails(context, pal, detailX + 8, detailY + 100, detailSectionWidth() - 16);
    }

    private void renderBasePets(DrawContext context, PalCraftClientState.BaseSummary base, int panelX, int panelY) {
        int listX = listSectionX(panelX);
        int listY = panelY + BASE_CONTENT_TOP;
        int detailX = detailSectionX(panelX);
        int detailY = panelY + BASE_CONTENT_TOP;

        drawSectionTitle(context, Text.translatable("screen.palcraft.pet_storage"), listX + 8, listY + 8, LIST_WIDTH - 16, COLOR_ACCENT);
        drawSectionTitle(context, Text.translatable("screen.palcraft.details"), detailX + 8, detailY + 8, detailSectionWidth() - 16, COLOR_GOLD);
        drawBaseSelectionAccents(context, panelX, panelY, base);

        PalCraftClientState.BasePalSummary pal = selectedBasePal(base);
        if (pal == null) {
            drawTextClipped(context, I18n.translate("screen.palcraft.no_base_pals"), detailX + 8, detailY + 34, detailSectionWidth() - 16, COLOR_TEXT_MUTED);
            return;
        }
        renderBasePalDetails(context, pal, detailX + 8, detailY + 34, detailSectionWidth() - 16);
    }

    private void renderSelectedPal(DrawContext context, int x, int y, int width) {
        PalCraftClientState.PalSummary pal = selectedPal();
        if (pal == null) {
            drawTextClipped(context, I18n.translate("screen.palcraft.no_pals"), x, y, width, COLOR_TEXT_MUTED);
            return;
        }

        int healthColor = pal.fainted() ? COLOR_BAD : pal.health() <= pal.maxHealth() * 0.35F ? COLOR_WARN : COLOR_GOOD;
        drawTextClipped(context, displayName(pal), x, y, pal.active() ? width - 58 : width, COLOR_ACCENT);
        if (pal.active()) {
            drawBadge(context, x + width - 52, y - 3, I18n.translate("screen.palcraft.active_suffix"), COLOR_ACCENT_DIM, COLOR_ACCENT);
        }

        String meta = I18n.translate("screen.palcraft.level_value", pal.level())
                + "  " + localizedElement(pal.element())
                + "  " + I18n.translate("screen.palcraft.talent_value", twoDecimals(pal.talent()));
        drawTextClipped(context, meta, x, y + 14, width, COLOR_GOLD);

        drawMeter(context, x, y + 32, width, I18n.translate("screen.palcraft.exp_value", pal.experience(), pal.nextExperience()), pal.experience(), pal.nextExperience(), 0xFF21333A, COLOR_EXP);
        drawMeter(context, x, y + 60, width, I18n.translate("screen.palcraft.hp_value", Math.round(pal.health()), Math.round(pal.maxHealth())), Math.max(0, pal.health()), pal.maxHealth(), 0xFF3A2222, healthColor);

        drawChip(context, x, y + 88, (width - 6) / 2, I18n.translate("screen.palcraft.stats_value", oneDecimal(pal.attack()), oneDecimal(pal.defense())));
        drawChip(context, x + (width + 6) / 2, y + 88, (width - 6) / 2, I18n.translate("element.palcraft." + pal.element()));
        drawTextClipped(context, I18n.translate("screen.palcraft.skills_value", localizedSkills(pal.skills())), x, y + 114, width, COLOR_TEXT_MUTED);
    }

    private void renderBasePalDetails(DrawContext context, PalCraftClientState.BasePalSummary pal, int x, int y, int width) {
        int healthColor = pal.health() <= 0.0F ? COLOR_BAD : pal.health() <= pal.maxHealth() * 0.35F ? COLOR_WARN : COLOR_GOOD;
        drawTextClipped(context, displayName(pal), x, y, width, COLOR_ACCENT);

        String meta = I18n.translate("screen.palcraft.level_value", pal.level())
                + "  " + localizedElement(pal.element())
                + "  " + I18n.translate("screen.palcraft.talent_value", twoDecimals(pal.talent()));
        drawTextClipped(context, meta, x, y + 14, width, COLOR_GOLD);

        drawMeter(context, x, y + 34, width, I18n.translate("screen.palcraft.hp_value", Math.round(pal.health()), Math.round(pal.maxHealth())), Math.max(0, pal.health()), pal.maxHealth(), 0xFF3A2222, healthColor);

        String status = pal.assigned() ? I18n.translate("work.palcraft." + pal.workType()) : I18n.translate("screen.palcraft.unassigned");
        String deployed = pal.deployed() ? I18n.translate("screen.palcraft.deployed") : I18n.translate("screen.palcraft.in_storage");
        drawChip(context, x, y + 66, (width - 6) / 2, I18n.translate("screen.palcraft.assignment_status", status));
        drawChip(context, x + (width + 6) / 2, y + 66, (width - 6) / 2, I18n.translate("screen.palcraft.deploy_status", deployed));
    }

    private void drawPlayerSelectionAccents(DrawContext context, int panelX, int panelY, PalCraftClientState.UiState state) {
        int listX = listInnerX(panelX);
        int listY = panelY + PLAYER_CONTENT_TOP + 28;
        for (int i = 0; i < state.carryLimit(); i++) {
            PalCraftClientState.PalSummary pal = i < state.pals().size() ? state.pals().get(i) : null;
            if (pal == null) {
                continue;
            }
            int rowY = listY + i * (ROW_HEIGHT + ROW_GAP);
            if (pal.slot() == selectedSlot) {
                context.fill(listX - 4, rowY, listX - 2, rowY + ROW_HEIGHT, COLOR_ACCENT);
            }
            if (pal.active()) {
                context.fill(listX + listInnerWidth() - 4, rowY + 4, listX + listInnerWidth() - 2, rowY + ROW_HEIGHT - 4, COLOR_GOOD);
            }
        }
    }

    private void drawBaseSelectionAccents(DrawContext context, int panelX, int panelY, PalCraftClientState.BaseSummary base) {
        int listX = listInnerX(panelX);
        int listY = panelY + BASE_CONTENT_TOP + 28;
        List<PalCraftClientState.BasePalSummary> pals = visibleBasePals(base);
        for (int i = 0; i < Math.min(6, pals.size()); i++) {
            PalCraftClientState.BasePalSummary pal = pals.get(i);
            int rowY = listY + i * (ROW_HEIGHT + ROW_GAP);
            if (pal.slot() == selectedBasePalSlot) {
                context.fill(listX - 4, rowY, listX - 2, rowY + ROW_HEIGHT, COLOR_ACCENT);
            }
            if (pal.assigned()) {
                context.fill(listX + listInnerWidth() - 4, rowY + 4, listX + listInnerWidth() - 2, rowY + ROW_HEIGHT - 4, COLOR_GOOD);
            }
        }
    }

    private void drawSelectedTabAccent(DrawContext context, int panelX, int panelY) {
        int index = selectedTab.ordinal();
        int x = panelX + MARGIN + index * 92;
        context.fill(x, panelY + 53, x + 86, panelY + 55, COLOR_ACCENT);
    }

    private void drawSectionTitle(DrawContext context, Text text, int x, int y, int width, int color) {
        drawTextClipped(context, text.getString(), x, y, width, color);
        context.fill(x, y + 13, x + width, y + 14, COLOR_BORDER_SOFT);
    }

    private void drawMetricCard(DrawContext context, int x, int y, int width, String label, String value, int valueColor) {
        drawBox(context, x, y, width, 40, COLOR_SECTION);
        drawTextClipped(context, label, x + 8, y + 7, width - 16, COLOR_TEXT_MUTED);
        drawTextClipped(context, value, x + 8, y + 21, width - 16, valueColor);
    }

    private void drawSummaryLine(DrawContext context, int x, int y, int width, String text, int accentColor) {
        context.fill(x, y, x + 3, y + 12, accentColor);
        drawTextClipped(context, text, x + 8, y + 2, width - 8, COLOR_TEXT);
    }

    private void drawChip(DrawContext context, int x, int y, int width, String text) {
        drawBox(context, x, y, width, 19, COLOR_SECTION_ALT);
        drawTextClipped(context, text, x + 6, y + 6, width - 12, COLOR_TEXT_MUTED);
    }

    private void drawMeter(DrawContext context, int x, int y, int width, String label, float value, float maxValue, int backgroundColor, int fillColor) {
        drawTextClipped(context, label, x, y, width, COLOR_TEXT_MUTED);
        drawBar(context, x, y + 12, width, 7, value, maxValue, backgroundColor, fillColor);
    }

    private void drawBar(DrawContext context, int x, int y, int width, int height, float value, float maxValue, int backgroundColor, int fillColor) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, COLOR_BORDER_SOFT);
        context.fill(x, y, x + width, y + height, backgroundColor);
        int filled = maxValue <= 0.0F ? 0 : Math.min(width, Math.max(0, Math.round(width * (value / maxValue))));
        context.fill(x, y, x + filled, y + height, fillColor | 0xFF000000);
        if (filled > 0 && filled < width) {
            context.fill(x + filled, y, x + filled + 1, y + height, 0x55FFFFFF);
        }
    }

    private void drawBadge(DrawContext context, int x, int y, String text, int backgroundColor, int textColor) {
        int width = textRenderer.getWidth(text) + 10;
        context.fill(x, y, x + width, y + 13, backgroundColor);
        context.fill(x, y + 12, x + width, y + 13, textColor);
        context.drawTextWithShadow(textRenderer, text, x + 5, y + 3, textColor);
    }

    private void drawBox(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, COLOR_BORDER_SOFT);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    private void drawTextClipped(DrawContext context, String text, int x, int y, int width, int color) {
        context.drawTextWithShadow(textRenderer, trimToWidth(text, width), x, y, color);
    }

    private String trimToWidth(String text, int width) {
        if (text == null || width <= 0) {
            return "";
        }
        if (textRenderer.getWidth(text) <= width) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = textRenderer.getWidth(suffix);
        if (width <= suffixWidth) {
            return "";
        }
        return textRenderer.trimToWidth(text, width - suffixWidth) + suffix;
    }

    private String listLabel(int index, String name, int level, boolean active, int width) {
        String suffix = active ? "  " + I18n.translate("screen.palcraft.active_suffix") : "";
        String prefix = index + ". ";
        String levelText = "  " + I18n.translate("screen.palcraft.level_value", level);
        int nameWidth = width - textRenderer.getWidth(prefix + levelText + suffix);
        return prefix + trimToWidth(name, nameWidth) + levelText + suffix;
    }

    private String baseListLabel(PalCraftClientState.BasePalSummary pal, int width) {
        String status = pal.assigned() ? "  " + I18n.translate("screen.palcraft.assigned_short") : pal.deployed() ? "  " + I18n.translate("screen.palcraft.deployed_short") : "";
        String prefix = (pal.slot() + 1) + ". ";
        String levelText = "  " + I18n.translate("screen.palcraft.level_value", pal.level());
        int nameWidth = width - textRenderer.getWidth(prefix + levelText + status);
        return prefix + trimToWidth(displayName(pal), nameWidth) + levelText + status;
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

    private int panelX() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int panelY() {
        return height / 2 - PANEL_HEIGHT / 2;
    }

    private int listSectionX(int panelX) {
        return panelX + MARGIN;
    }

    private int listInnerX(int panelX) {
        return listSectionX(panelX) + 6;
    }

    private int listInnerWidth() {
        return LIST_WIDTH - 12;
    }

    private int detailSectionX(int panelX) {
        return panelX + MARGIN + LIST_WIDTH + GAP;
    }

    private int detailSectionWidth() {
        return PANEL_WIDTH - MARGIN * 2 - LIST_WIDTH - GAP;
    }

    private int detailInnerX(int panelX) {
        return detailSectionX(panelX) + 8;
    }

    private int detailInnerWidth() {
        return detailSectionWidth() - 16;
    }

    private static String oneDecimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String twoDecimals(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
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

    private String localizedWorkSummary(String summary) {
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
