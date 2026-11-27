package ru.eclipse.client.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Асинхронная проверка обновлений ECLIPSE CLIENT через GitHub Raw JSON.
 */
public final class UpdateChecker {

    public static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/vendetta139/eclipse-client/main/version.json";

    private static final long PERIODIC_CHECK_MINUTES = 3L;

    private static final AtomicBoolean CHECK_IN_FLIGHT = new AtomicBoolean(false);
    private static final AtomicBoolean SCHEDULER_STARTED = new AtomicBoolean(false);
    private static final ExecutorService HTTP_EXECUTOR = new ThreadPoolExecutor(
            0, 2, 15L, TimeUnit.SECONDS, new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "Eclipse-UpdateHttp");
                t.setDaemon(true);
                return t;
            }
    );
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .executor(HTTP_EXECUTOR)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Eclipse-UpdateChecker");
        t.setDaemon(true);
        return t;
    });

    private static volatile UpdateInfo pendingUpdate = null;
    private static volatile String lastManifestJson = null;
    private static volatile boolean lastBetaChannel = false;
    private static volatile String lastLoggedVersion = null;

    public record UpdateInfo(String currentVersion, String latestVersion, String downloadUrl, List<String> changelog) {}

    private UpdateChecker() {}

    public static String getCurrentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(EclipseClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("2.0.0");
    }

    public static UpdateInfo getPendingUpdate() {
        VisualConfig cfg = VisualConfig.getInstance();
        if (cfg != null && !cfg.checkUpdates) {
            return null;
        }
        boolean betaEnabled = cfg != null && cfg.betaChannel;
        if (lastManifestJson != null && betaEnabled != lastBetaChannel) {
            lastBetaChannel = betaEnabled;
            pendingUpdate = parseManifestIfNewer(lastManifestJson, getCurrentModVersion(), betaEnabled);
        }
        return pendingUpdate;
    }

    public static void checkAsync() {
        startPeriodicChecksIfNeeded();

        VisualConfig cfg = VisualConfig.getInstance();
        if (cfg != null && !cfg.checkUpdates) {
            return;
        }
        if (!CHECK_IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }

        String currentVersion = getCurrentModVersion();

        // Опциональный тестовый режим через -Declipse.updateTestFile=path/to/version.json
        String testFileProp = System.getProperty("eclipse.updateTestFile");
        if (testFileProp != null && !testFileProp.isBlank()) {
            CompletableFuture.runAsync(() -> {
                try {
                    String json = Files.readString(Path.of(testFileProp), StandardCharsets.UTF_8);
                    handleManifestResponse(json, currentVersion);
                } catch (Throwable ignored) {
                } finally {
                    CHECK_IN_FLIGHT.set(false);
                }
            });
            return;
        }

        try {
            URI uri = URI.create(UPDATE_JSON_URL);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .header("User-Agent", "EclipseClient/" + currentVersion)
                    .GET()
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .whenComplete((response, ex) -> {
                        try {
                            if (ex == null && response != null && response.statusCode() == 200 && response.body() != null) {
                                handleManifestResponse(response.body(), currentVersion);
                            }
                        } finally {
                            CHECK_IN_FLIGHT.set(false);
                        }
                    });
        } catch (Throwable ignored) {
            CHECK_IN_FLIGHT.set(false);
        }
    }

    private static void startPeriodicChecksIfNeeded() {
        if (SCHEDULER_STARTED.compareAndSet(false, true)) {
            SCHEDULER.scheduleAtFixedRate(
                    UpdateChecker::checkAsync,
                    PERIODIC_CHECK_MINUTES,
                    PERIODIC_CHECK_MINUTES,
                    TimeUnit.MINUTES
            );
        }
    }

    private static void handleManifestResponse(String body, String currentVersion) {
        VisualConfig cfg = VisualConfig.getInstance();
        boolean betaEnabled = cfg != null && cfg.betaChannel;
        lastManifestJson = body;
        lastBetaChannel = betaEnabled;

        UpdateInfo info = parseManifestIfNewer(body, currentVersion, betaEnabled);
        pendingUpdate = info;
        if (info != null && !info.latestVersion().equals(lastLoggedVersion)) {
            lastLoggedVersion = info.latestVersion();
            EclipseClient.LOGGER.info("Доступно обновление ECLIPSE CLIENT: v{} -> v{}",
                    info.currentVersion(), info.latestVersion());
        }
    }

    public static UpdateInfo parseManifestIfNewer(String jsonBody, String currentVersion) {
        VisualConfig cfg = VisualConfig.getInstance();
        boolean betaChannelEnabled = cfg != null && cfg.betaChannel;
        return parseManifestIfNewer(jsonBody, currentVersion, betaChannelEnabled);
    }

    public static UpdateInfo parseManifestIfNewer(String jsonBody, String currentVersion, boolean betaChannelEnabled) {
        if (jsonBody == null || jsonBody.isBlank()) return null;
        try {
            JsonElement parsed = JsonParser.parseString(jsonBody);
            if (parsed == null || !parsed.isJsonObject()) return null;
            JsonObject root = parsed.getAsJsonObject();

            String rootUrl = getStringOrEmpty(root, "download_url");
            List<String> rootChanges = readChangelog(root);

            UpdateInfo stableCandidate = extractCandidate(root, currentVersion, rootUrl, rootChanges);
            UpdateInfo best = stableCandidate;

            if (betaChannelEnabled && root.has("beta") && root.get("beta").isJsonObject()) {
                JsonObject betaObj = root.getAsJsonObject("beta");
                UpdateInfo betaCandidate = extractCandidate(betaObj, currentVersion, rootUrl, rootChanges);
                if (betaCandidate != null && (best == null || isNewerVersion(best.latestVersion(), betaCandidate.latestVersion()))) {
                    best = betaCandidate;
                }
            }

            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UpdateInfo extractCandidate(JsonObject obj, String currentVersion, String fallbackUrl, List<String> fallbackChanges) {
        if (obj == null || !obj.has("version") || !obj.get("version").isJsonPrimitive()) {
            return null;
        }
        String remoteVersion = obj.get("version").getAsString().trim();
        if (remoteVersion.isEmpty() || !isNewerVersion(currentVersion, remoteVersion)) {
            return null;
        }
        String url = getStringOrEmpty(obj, "download_url");
        if (url.isEmpty()) {
            url = fallbackUrl;
        }
        List<String> changes = readChangelog(obj);
        if (changes.isEmpty() && fallbackChanges != null) {
            changes = fallbackChanges;
        }
        return new UpdateInfo(stripPrefix(currentVersion), stripPrefix(remoteVersion), url, changes);
    }

    private static String getStringOrEmpty(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive()
                ? obj.get(key).getAsString().trim()
                : "";
    }

    private static List<String> readChangelog(JsonObject obj) {
        List<String> changes = new ArrayList<>();
        if (obj.has("changelog") && obj.get("changelog").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("changelog");
            for (JsonElement el : arr) {
                if (el != null && el.isJsonPrimitive()) {
                    String line = el.getAsString().trim();
                    if (!line.isEmpty()) {
                        changes.add(line);
                    }
                }
            }
        }
        return changes;
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

    public static URI parseHttpUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
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
            if (!thisPre) return 1;  // 2.0.1 > 2.0.1-beta.1
            if (!otherPre) return -1;

            String[] a = this.preRelease.split("\\.");
            String[] b = other.preRelease.split("\\.");
            int len = Math.min(a.length, b.length);
            for (int i = 0; i < len; i++) {
                boolean aNum = isDigitsOnly(a[i]);
                boolean bNum = isDigitsOnly(b[i]);
                if (aNum && bNum) {
                    int cmp = compareNumericStrings(a[i], b[i]);
                    if (cmp != 0) return cmp;
                } else if (aNum != bNum) {
                    // В SemVer числовые идентификаторы имеют меньший приоритет, чем буквенные
                    return aNum ? -1 : 1;
                } else {
                    int cmp = a[i].compareToIgnoreCase(b[i]);
                    if (cmp != 0) return cmp;
                }
            }
            return Integer.compare(a.length, b.length);
        }

        private static boolean isDigitsOnly(String s) {
            if (s == null || s.isEmpty()) return false;
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isDigit(s.charAt(i))) return false;
            }
            return true;
        }

        private static int compareNumericStrings(String a, String b) {
            try {
                return Long.compare(Long.parseLong(a), Long.parseLong(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        }
    }
}
