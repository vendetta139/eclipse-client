package ru.eclipse.client.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ru.example.donorprefix.visual.VisualConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Асинхронная проверка обновлений ECLIPSE CLIENT через GitHub Raw JSON.
 */
public final class UpdateChecker {

    public static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/vendetta139/eclipse-client/main/version.json";

    private static final int COLOR_GOLD = 0xF5A65B;
    private static final int COLOR_INDIGO = 0x6A67CE;
    private static final int COLOR_TEXT = 0xEAECEF;
    private static final int COLOR_MUTED = 0x8A8A93;

    private static volatile UpdateInfo pendingUpdate = null;
    private static volatile boolean notifiedThisSession = false;
    private static volatile boolean joinHookRegistered = false;

    public record UpdateInfo(String currentVersion, String latestVersion, String downloadUrl, List<String> changelog) {}

    private UpdateChecker() {}

    public static String getCurrentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(EclipseClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("2.0.0");
    }

    public static void registerChatNotifier() {
        if (joinHookRegistered) return;
        joinHookRegistered = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            UpdateInfo info = pendingUpdate;
            if (info == null || notifiedThisSession) {
                return;
            }
            notifiedThisSession = true;
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(buildChatMessage(info), false);
                }
            });
        });
    }

    public static void checkAsync() {
        VisualConfig cfg = VisualConfig.getInstance();
        if (cfg != null && !cfg.checkUpdates) {
            return;
        }

        String currentVersion = getCurrentModVersion();

        // Опциональный тестовый режим через -Declipse.updateTestFile=path/to/version.json
        String testFileProp = System.getProperty("eclipse.updateTestFile");
        if (testFileProp != null && !testFileProp.isBlank()) {
            CompletableFuture.runAsync(() -> {
                try {
                    String json = Files.readString(Path.of(testFileProp), StandardCharsets.UTF_8);
                    UpdateInfo info = parseManifestIfNewer(json, currentVersion);
                    if (info != null) {
                        pendingUpdate = info;
                        EclipseClient.LOGGER.info("Найдено обновление (тест): v{} -> v{}", info.currentVersion(), info.latestVersion());
                    }
                } catch (Throwable ignored) {}
            });
            return;
        }

        try {
            URI uri = URI.create(UPDATE_JSON_URL);
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("User-Agent", "EclipseClient/" + currentVersion)
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenAccept(response -> {
                        if (response != null && response.statusCode() == 200 && response.body() != null) {
                            UpdateInfo info = parseManifestIfNewer(response.body(), currentVersion);
                            if (info != null) {
                                pendingUpdate = info;
                                EclipseClient.LOGGER.info("Доступно обновление ECLIPSE CLIENT: v{} -> v{}",
                                        info.currentVersion(), info.latestVersion());
                                MinecraftClient mc = MinecraftClient.getInstance();
                                if (mc != null && !notifiedThisSession) {
                                    mc.execute(() -> {
                                        if (mc.player != null && !notifiedThisSession) {
                                            notifiedThisSession = true;
                                            mc.player.sendMessage(buildChatMessage(info), false);
                                        }
                                    });
                                }
                            }
                        }
                    })
                    .exceptionally(ex -> null);
        } catch (Throwable ignored) {
            // Молча игнорируем ошибки сети/URL без спама стектрейсом
        }
    }

    public static UpdateInfo parseManifestIfNewer(String jsonBody, String currentVersion) {
        if (jsonBody == null || jsonBody.isBlank()) return null;
        try {
            JsonElement parsed = JsonParser.parseString(jsonBody);
            if (parsed == null || !parsed.isJsonObject()) return null;
            JsonObject obj = parsed.getAsJsonObject();

            if (!obj.has("version") || !obj.get("version").isJsonPrimitive()) {
                return null;
            }
            String remoteVersion = obj.get("version").getAsString().trim();
            if (remoteVersion.isEmpty() || !isNewerVersion(currentVersion, remoteVersion)) {
                return null;
            }

            String downloadUrl = obj.has("download_url") && obj.get("download_url").isJsonPrimitive()
                    ? obj.get("download_url").getAsString().trim()
                    : "";

            List<String> changes = new ArrayList<>();
            if (obj.has("changelog") && obj.get("changelog").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("changelog");
                for (JsonElement el : arr) {
                    if (el != null && el.isJsonPrimitive()) {
                        String line = el.getAsString().trim();
                        if (!line.isEmpty()) {
                            changes.add(line);
                            if (changes.size() >= 4) break;
                        }
                    }
                }
            }

            return new UpdateInfo(stripPrefix(currentVersion), stripPrefix(remoteVersion), downloadUrl, changes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Сравнение версий по SemVer (major.minor.patch и опциональный pre-release).
     * Возвращает true, только если remote строго новее current.
     */
    public static boolean isNewerVersion(String current, String remote) {
        SemVer curVer = SemVer.parse(current);
        SemVer remVer = SemVer.parse(remote);
        if (curVer == null || remVer == null) return false;
        return remVer.compareTo(curVer) > 0;
    }

    private static String stripPrefix(String v) {
        if (v == null) return "0.0.0";
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        return s;
    }

    public static MutableText buildChatMessage(UpdateInfo info) {
        MutableText root = Text.empty();

        // Верхний разделитель
        root.append(Text.literal("✦ ").setStyle(Style.EMPTY.withColor(COLOR_GOLD).withBold(true)));
        root.append(Text.literal("ECLIPSE CLIENT").setStyle(Style.EMPTY.withColor(COLOR_GOLD).withBold(true)));
        root.append(Text.literal(" • ").setStyle(Style.EMPTY.withColor(COLOR_MUTED)));
        root.append(Text.literal("Доступна новая версия: ").setStyle(Style.EMPTY.withColor(COLOR_TEXT)));
        root.append(Text.literal("v" + info.currentVersion()).setStyle(Style.EMPTY.withColor(COLOR_MUTED)));
        root.append(Text.literal(" → ").setStyle(Style.EMPTY.withColor(COLOR_INDIGO).withBold(true)));
        root.append(Text.literal("v" + info.latestVersion()).setStyle(Style.EMPTY.withColor(COLOR_GOLD).withBold(true)));

        // Список изменений (до 4 пунктов)
        if (info.changelog() != null && !info.changelog().isEmpty()) {
            int limit = Math.min(4, info.changelog().size());
            for (int i = 0; i < limit; i++) {
                root.append(Text.literal("\n  • ").setStyle(Style.EMPTY.withColor(COLOR_INDIGO)));
                root.append(Text.literal(info.changelog().get(i)).setStyle(Style.EMPTY.withColor(COLOR_TEXT)));
            }
        }

        // Кликабельная кнопка [ ⬇ Скачать обновление ]
        if (info.downloadUrl() != null && !info.downloadUrl().isBlank()) {
            URI uri = parseHttpUri(info.downloadUrl());
            if (uri != null) {
                MutableText hover = Text.empty()
                        .append(Text.literal("Открыть страницу загрузки:\n").setStyle(Style.EMPTY.withColor(COLOR_TEXT)))
                        .append(Text.literal(info.downloadUrl()).setStyle(Style.EMPTY.withColor(COLOR_GOLD)));

                Style btnStyle = Style.EMPTY
                        .withColor(COLOR_GOLD)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.OpenUrl(uri))
                        .withHoverEvent(new HoverEvent.ShowText(hover));

                root.append(Text.literal("\n"));
                root.append(Text.literal("[ ").setStyle(Style.EMPTY.withColor(COLOR_INDIGO).withBold(true)));
                root.append(Text.literal("⬇ Скачать обновление").setStyle(btnStyle));
                root.append(Text.literal(" ]").setStyle(Style.EMPTY.withColor(COLOR_INDIGO).withBold(true)));
            }
        }

        return root;
    }

    private static URI parseHttpUri(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                return uri;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private record SemVer(int major, int minor, int patch, String preRelease) implements Comparable<SemVer> {
        static SemVer parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String s = stripPrefix(raw);
            int plusIdx = s.indexOf('+');
            if (plusIdx >= 0) {
                s = s.substring(0, plusIdx);
            }
            String pre = "";
            int dashIdx = s.indexOf('-');
            if (dashIdx >= 0) {
                pre = s.substring(dashIdx + 1);
                s = s.substring(0, dashIdx);
            }
            String[] parts = s.split("\\.");
            try {
                int maj = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0]) : 0;
                int min = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0;
                int pat = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : 0;
                return new SemVer(maj, min, pat, pre);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public int compareTo(SemVer other) {
            if (this.major != other.major) return Integer.compare(this.major, other.major);
            if (this.minor != other.minor) return Integer.compare(this.minor, other.minor);
            if (this.patch != other.patch) return Integer.compare(this.patch, other.patch);

            boolean thisPre = this.preRelease != null && !this.preRelease.isEmpty();
            boolean otherPre = other.preRelease != null && !other.preRelease.isEmpty();
            if (!thisPre && !otherPre) return 0;
            if (!thisPre) return 1;  // 2.0.1 > 2.0.1-beta
            if (!otherPre) return -1;
            return this.preRelease.compareToIgnoreCase(other.preRelease);
        }
    }
}
