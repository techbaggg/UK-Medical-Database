package com.drgenz;

import static org.junit.Assert.*;

import org.junit.Test;

public class AppUpdateHelperTest {

    @Test
    public void parseValidVersionConfig() {
        String json = "{\n" +
                "  \"latestVersionCode\": 51,\n" +
                "  \"minRequiredVersionCode\": 40,\n" +
                "  \"versionName\": \"1.8.30\",\n" +
                "  \"updateMessage\": \"New UK medicine records updated.\",\n" +
                "  \"playStoreUrl\": \"https://play.google.com/store/apps/details?id=com.drgenz.ukmeddb\",\n" +
                "  \"websiteUrl\": \"https://drgenz.info\"\n" +
                "}";

        AppUpdateHelper.VersionConfig cfg = AppUpdateHelper.parseVersionConfig(json);
        assertNotNull(cfg);
        assertEquals(51, cfg.latestVersionCode);
        assertEquals(40, cfg.minRequiredVersionCode);
        assertEquals("1.8.30", cfg.versionName);
        assertEquals("New UK medicine records updated.", cfg.updateMessage);
        assertEquals("https://play.google.com/store/apps/details?id=com.drgenz.ukmeddb", cfg.playStoreUrl);
        assertEquals("https://drgenz.info", cfg.websiteUrl);
    }

    @Test
    public void parseEmptyOrCorruptReturnsNull() {
        assertNull(AppUpdateHelper.parseVersionConfig(null));
        assertNull(AppUpdateHelper.parseVersionConfig(""));
        assertNull(AppUpdateHelper.parseVersionConfig("{invalid-json"));
    }

    @Test
    public void forceUpdateConditions() {
        AppUpdateHelper.VersionConfig cfg = new AppUpdateHelper.VersionConfig(
                51, 40, "1.8.30", "Update now", null, null
        );

        // Version 39 < min 40 -> Force update
        assertTrue(AppUpdateHelper.shouldForceUpdate(39, cfg));

        // Version 40 == min 40 -> Do not force
        assertFalse(AppUpdateHelper.shouldForceUpdate(40, cfg));

        // Version 45 > min 40 -> Do not force
        assertFalse(AppUpdateHelper.shouldForceUpdate(45, cfg));
    }

    @Test
    public void suggestUpdateConditions() {
        AppUpdateHelper.VersionConfig cfg = new AppUpdateHelper.VersionConfig(
                51, 40, "1.8.30", "Update available", null, null
        );

        // Version 45 < latest 51 and >= min 40 -> Suggest update
        assertTrue(AppUpdateHelper.shouldSuggestUpdate(45, cfg));
        assertTrue(AppUpdateHelper.shouldSuggestUpdate(50, cfg));

        // Version 51 == latest 51 -> No suggestion needed
        assertFalse(AppUpdateHelper.shouldSuggestUpdate(51, cfg));

        // Version 39 < min 40 -> Should force, not merely suggest
        assertFalse(AppUpdateHelper.shouldSuggestUpdate(39, cfg));
    }

    @Test
    public void defaultFallbackUrls() {
        AppUpdateHelper.VersionConfig cfg = new AppUpdateHelper.VersionConfig(
                51, 40, "1.8.30", null, null, null
        );
        assertTrue(cfg.playStoreUrl.contains("play.google.com"));
        assertTrue(cfg.websiteUrl.contains("drgenz.info"));
        assertNotNull(cfg.updateMessage);
    }
}
