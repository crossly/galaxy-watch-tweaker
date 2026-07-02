package dev.ricky.galaxywatchtweaker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public final class LocalizationResourceTest {
    private static final Pattern STRING_NAME = Pattern.compile("<string name=\"([^\"]+)\"");

    @Test
    public void defaultResourcesRemainEnglishForNonChineseLocales() throws Exception {
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(strings.contains("<string name=\"tab_hooks\">Hooks</string>"));
        assertTrue(strings.contains("<string name=\"version_unknown\">Not detected</string>"));
        assertFalse(strings.contains("重启目标应用"));
    }

    @Test
    public void chineseResourcesTranslateEveryUserFacingString() throws Exception {
        String english = read("app/src/main/res/values/strings.xml");
        String chinese = read("app/src/main/res/values-zh/strings.xml");

        Matcher matcher = STRING_NAME.matcher(english);
        while (matcher.find()) {
            assertTrue(chinese.contains("<string name=\"" + matcher.group(1) + "\">"));
        }

        assertTrue(chinese.contains("<string name=\"tab_hooks\">功能</string>"));
        assertTrue(chinese.contains("<string name=\"version_unknown\">未检测到</string>"));
    }

    @Test
    public void chineseResourcesUseLocalizedConciseCopy() throws Exception {
        String chinese = read("app/src/main/res/values-zh/strings.xml");

        assertTrue(chinese.contains("<string name=\"shm_patch_title\">解锁血压和心电</string>"));
        assertTrue(chinese.contains("<string name=\"connection_recovery_title\">自动恢复连接</string>"));
        assertFalse(chinese.contains("钩子"));
        assertFalse(chinese.contains("能力交换"));
        assertFalse(chinese.contains("伴侣"));
        assertFalse(chinese.contains("轻推"));
        assertFalse(chinese.contains("矩阵"));
    }

    @Test
    public void composeUiReadsHeadingsFromStringResources() throws Exception {
        String hooksPage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/HooksPage.kt");
        String profilePage = read("app/src/main/java/dev/ricky/galaxywatchtweaker/ui/ProfilePage.kt");

        assertFalse(hooksPage.contains("SettingsGroup(title = \""));
        assertFalse(hooksPage.contains("title = \"Companion Identity\""));
        assertFalse(profilePage.contains("SettingsGroup(title = \""));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
