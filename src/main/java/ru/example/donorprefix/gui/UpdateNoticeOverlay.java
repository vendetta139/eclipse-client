package ru.example.donorprefix.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import ru.eclipse.client.core.UpdateChecker;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Компактный монохромный бейдж обновлений и модальное окно списка изменений.
 * Кэширует перенесённые строки, не создаёт лишних объектов на каждом кадре.
 */
public final class UpdateNoticeOverlay {

    public interface Host {
        boolean eclipseHandleUpdateEscape();
    }

    private static final Text MODAL_TITLE = Text.literal("Доступно обновление").formatted(Formatting.BOLD);
    private static final Text BTN_DOWNLOAD = Text.literal("Скачать").formatted(Formatting.BOLD);
    private static final Text BTN_CLOSE = Text.literal("Закрыть");
    private static final Text EMPTY_CHANGELOG = Text.literal("Список изменений не указан.");

    private record WrappedLine(OrderedText text, boolean firstOfEntry, boolean itemGapBefore) {}

    private record ModalLayout(
            int cardX, int cardY, int cardW, int cardH,
            int paddingX, int bulletW, int headerH, int footerH,
            int lineStep, int entryGap,
            int dlX, int closeX, int btnY, int btnW, int btnH
    ) {}

    private boolean modalOpen = false;

    private UpdateChecker.UpdateInfo cachedBadgeInfo = null;
    private String cachedBadgeLabel = "";
    private String cachedSubtitle = "";

    private UpdateChecker.UpdateInfo cachedModalInfo = null;
    private int cachedWrapWidth = -1;
    private final List<WrappedLine> wrappedChangelog = new ArrayList<>();

    public boolean isModalOpen() {
        if (modalOpen && UpdateChecker.getPendingUpdate() == null) {
            modalOpen = false;
        }
        return modalOpen;
    }

    public int getBadgeWidth(TextRenderer tr) {
        UpdateChecker.UpdateInfo info = UpdateChecker.getPendingUpdate();
        if (info == null || tr == null) return 0;
        ensureVersionCache(info);
        return tr.getWidth(cachedBadgeLabel) + 16;
    }

    public void renderBadge(DrawContext context, TextRenderer tr, int x, int y, int w, int h, int mouseX, int mouseY) {
        if (context == null || tr == null) return;
        UpdateChecker.UpdateInfo info = UpdateChecker.getPendingUpdate();
        if (info == null) return;

        ensureVersionCache(info);

        boolean hovered = !modalOpen && isInside(mouseX, mouseY, x, y, w, h);
        int bg = hovered ? EclipseColors.ACCENT_WHITE : EclipseColors.BG_CARD;
        int border = hovered ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        int textCol = hovered ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_PRIMARY;
        boolean drawShadow = !hovered;

        RenderUtils.drawFlatCard(context, x, y, w, h, bg, border);
        int textW = tr.getWidth(cachedBadgeLabel);
        context.drawText(tr, cachedBadgeLabel, x + (w - textW) / 2, y + (h - 8) / 2, textCol, drawShadow);
    }

    public boolean handleBadgeClick(double mouseX, double mouseY, int button, int x, int y, int w, int h) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || modalOpen) return false;
        if (UpdateChecker.getPendingUpdate() == null) return false;
        if (isInside(mouseX, mouseY, x, y, w, h)) {
            modalOpen = true;
            return true;
        }
        return false;
    }

    public void renderModal(DrawContext context, TextRenderer tr, int screenW, int screenH, int mouseX, int mouseY) {
        if (!isModalOpen() || context == null || tr == null) return;
        UpdateChecker.UpdateInfo info = UpdateChecker.getPendingUpdate();
        if (info == null) return;

        ModalLayout l = computeLayout(tr, info, screenW, screenH);

        // Затемнение фона
        context.fill(0, 0, screenW, screenH, 0xB8050508);

        // Карточка окна (#09090B + 1px #24242A через 4 context.fill)
        RenderUtils.drawFlatCard(context, l.cardX, l.cardY, l.cardW, l.cardH, EclipseColors.BG_WINDOW, EclipseColors.BORDER);

        // Заголовок и подпись версий
        int titleX = l.cardX + l.paddingX;
        context.drawText(tr, MODAL_TITLE, titleX, l.cardY + 12, EclipseColors.TEXT_PRIMARY, true);
        context.drawText(tr, cachedSubtitle, titleX, l.cardY + 26, EclipseColors.TEXT_SECONDARY, true);
        context.fill(l.cardX + 1, l.cardY + l.headerH, l.cardX + l.cardW - 1, l.cardY + l.headerH + 1, EclipseColors.BORDER);

        // Список изменений с переносом строк
        int curY = l.cardY + l.headerH + 10;
        int bodyLimitY = l.cardY + l.cardH - l.footerH - 8;

        if (wrappedChangelog.isEmpty()) {
            context.drawText(tr, EMPTY_CHANGELOG, titleX, curY, EclipseColors.TEXT_SECONDARY, true);
        } else {
            for (WrappedLine wl : wrappedChangelog) {
                if (wl.itemGapBefore) {
                    curY += l.entryGap;
                }
                if (curY + l.lineStep > bodyLimitY) {
                    break;
                }
                if (wl.firstOfEntry) {
                    context.drawText(tr, "•", titleX, curY, EclipseColors.TEXT_SECONDARY, true);
                }
                context.drawText(tr, wl.text, titleX + l.bulletW, curY, EclipseColors.TEXT_VERSION, true);
                curY += l.lineStep;
            }
        }

        // Нижние кнопки: Скачать (белая инверсия без тени) и Закрыть (тёмная с рамкой)
        boolean hoverDl = isInside(mouseX, mouseY, l.dlX, l.btnY, l.btnW, l.btnH);
        int dlBg = hoverDl ? 0xFFFFFFFF : EclipseColors.ACCENT_WHITE;
        int dlBorder = hoverDl ? 0xFFFFFFFF : EclipseColors.BORDER_ACTIVE;
        RenderUtils.drawFlatCard(context, l.dlX, l.btnY, l.btnW, l.btnH, dlBg, dlBorder);
        int dlTextW = tr.getWidth(BTN_DOWNLOAD);
        context.drawText(tr, BTN_DOWNLOAD, l.dlX + (l.btnW - dlTextW) / 2, l.btnY + 7, EclipseColors.TEXT_ON_ACCENT, false);

        boolean hoverClose = isInside(mouseX, mouseY, l.closeX, l.btnY, l.btnW, l.btnH);
        int closeBg = hoverClose ? EclipseColors.BG_HOVER : EclipseColors.BG_CARD;
        int closeBorder = hoverClose ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        RenderUtils.drawFlatCard(context, l.closeX, l.btnY, l.btnW, l.btnH, closeBg, closeBorder);
        int closeTextW = tr.getWidth(BTN_CLOSE);
        context.drawText(tr, BTN_CLOSE, l.closeX + (l.btnW - closeTextW) / 2, l.btnY + 7, EclipseColors.TEXT_PRIMARY, true);
    }

    public boolean handleModalClick(double mouseX, double mouseY, int button, int screenW, int screenH, TextRenderer tr) {
        if (!isModalOpen()) return false;
        UpdateChecker.UpdateInfo info = UpdateChecker.getPendingUpdate();
        if (info == null || tr == null) {
            modalOpen = false;
            return true;
        }

        ModalLayout l = computeLayout(tr, info, screenW, screenH);

        // Клик вне окна закрывает модалку
        if (!isInside(mouseX, mouseY, l.cardX, l.cardY, l.cardW, l.cardH)) {
            modalOpen = false;
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isInside(mouseX, mouseY, l.dlX, l.btnY, l.btnW, l.btnH)) {
                openDownloadUrl(info.downloadUrl());
                return true;
            }
            if (isInside(mouseX, mouseY, l.closeX, l.btnY, l.btnW, l.btnH)) {
                modalOpen = false;
                return true;
            }
        }
        return true;
    }

    public boolean handleKeyPressed(int keyCode) {
        if (!isModalOpen()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            modalOpen = false;
            return true;
        }
        return false;
    }

    private ModalLayout computeLayout(TextRenderer tr, UpdateChecker.UpdateInfo info, int screenW, int screenH) {
        ensureVersionCache(info);

        int cardW = Math.min(360, Math.max(260, screenW - 32));
        int paddingX = 16;
        int bulletW = tr.getWidth("• ");
        int textWrapW = Math.max(120, cardW - paddingX * 2 - bulletW);
        ensureWrappedChangelog(tr, info, textWrapW);

        int headerH = 44;
        int footerH = 38;
        int lineStep = 11;
        int entryGap = 4;

        int maxBodyH = Math.max(48, screenH - 32 - headerH - footerH - 16);
        int bodyContentH = computeBodyHeight(wrappedChangelog, lineStep, entryGap, maxBodyH);
        int cardH = headerH + 10 + bodyContentH + 10 + footerH;
        int cardX = (screenW - cardW) / 2;
        int cardY = (screenH - cardH) / 2;

        int btnGap = 8;
        int btnW = (cardW - paddingX * 2 - btnGap) / 2;
        int btnH = 22;
        int btnY = cardY + cardH - btnH - 12;
        int dlX = cardX + paddingX;
        int closeX = dlX + btnW + btnGap;

        return new ModalLayout(cardX, cardY, cardW, cardH, paddingX, bulletW, headerH, footerH, lineStep, entryGap, dlX, closeX, btnY, btnW, btnH);
    }

    private void ensureVersionCache(UpdateChecker.UpdateInfo info) {
        if (cachedBadgeInfo != info) {
            cachedBadgeInfo = info;
            cachedBadgeLabel = "● Обновление v" + info.latestVersion();
            cachedSubtitle = "v" + info.currentVersion() + " → v" + info.latestVersion();
        }
    }

    private void ensureWrappedChangelog(TextRenderer tr, UpdateChecker.UpdateInfo info, int wrapWidth) {
        if (cachedModalInfo == info && cachedWrapWidth == wrapWidth) {
            return;
        }
        cachedModalInfo = info;
        cachedWrapWidth = wrapWidth;
        wrappedChangelog.clear();

        List<String> changes = info.changelog();
        if (changes == null || changes.isEmpty()) return;

        for (String raw : changes) {
            if (raw == null || raw.isBlank()) continue;
            List<OrderedText> lines = tr.wrapLines(Text.literal(raw), wrapWidth);
            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                boolean first = (lineIdx == 0);
                boolean gapBefore = first && !wrappedChangelog.isEmpty();
                wrappedChangelog.add(new WrappedLine(lines.get(lineIdx), first, gapBefore));
            }
        }
    }

    private static int computeBodyHeight(List<WrappedLine> lines, int lineStep, int entryGap, int maxBodyH) {
        if (lines.isEmpty()) {
            return lineStep;
        }
        int total = 0;
        for (WrappedLine wl : lines) {
            int next = total + (wl.itemGapBefore ? entryGap : 0) + lineStep;
            if (next > maxBodyH) {
                break;
            }
            total = next;
        }
        return Math.max(lineStep, total);
    }

    private static void openDownloadUrl(String rawUrl) {
        URI uri = UpdateChecker.parseHttpUri(rawUrl);
        if (uri == null) {
            uri = UpdateChecker.parseHttpUri("https://github.com/vendetta139/eclipse-client/releases/latest");
        }
        if (uri != null) {
            Util.getOperatingSystem().open(uri);
        }
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
