package dev.ricky.galaxywatchtweaker;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ModuleMetadataTest {
    @Test
    public void modernXposedMetadataDeclaresApi101StaticScope() throws Exception {
        String moduleProp = read("app/src/main/resources/META-INF/xposed/module.prop");
        String scope = read("app/src/main/resources/META-INF/xposed/scope.list");
        String javaInit = read("app/src/main/resources/META-INF/xposed/java_init.list");

        assertTrue(moduleProp.contains("minApiVersion=101"));
        assertTrue(moduleProp.contains("targetApiVersion=101"));
        assertTrue(moduleProp.contains("staticScope=true"));
        assertTrue(scope.contains("android"));
        assertTrue(scope.contains("system"));
        assertTrue(scope.contains("com.google.android.wearable.app.cn"));
        assertTrue(scope.contains("com.samsung.android.app.watchmanager"));
        assertTrue(scope.contains("com.samsung.wearable.watch7plugin"));
        assertTrue(javaInit.contains("dev.ricky.galaxywatchtweaker.ModernHookEntry"));
    }

    @Test
    public void gradlePackagesModernMetadataAndCompilesAgainstApi101() throws Exception {
        String build = read("app/build.gradle");

        assertTrue(build.contains("merges += \"META-INF/xposed/*\""));
        assertTrue(build.contains("io.github.libxposed:api:101.0.1"));
        assertTrue(build.contains("disable += \"NullSafeMutableLiveData\""));
    }

    @Test
    public void manifestDeclaresMergedDefaultScopes() throws Exception {
        String manifest = read("app/src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(manifest.contains("com.google.android.wearable.app.cn"));
        assertTrue(manifest.contains("com.samsung.android.app.watchmanager"));
        assertTrue(manifest.contains("com.samsung.wearable.watch7plugin"));
        assertTrue(manifest.contains("com.samsung.android.shealthmonitor"));
    }

    @Test
    public void manifestDeclaresPackageQueriesForVersionMatrix() throws Exception {
        String manifest = read("app/src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("<queries>"));
        assertTrue(manifest.contains("android:name=\"com.samsung.android.shealthmonitor\""));
        assertTrue(manifest.contains("android:name=\"com.samsung.android.app.watchmanager\""));
        assertTrue(manifest.contains("android:name=\"com.samsung.wearable.watch7plugin\""));
        assertTrue(manifest.contains("android:name=\"com.google.android.wearable.app.cn\""));
    }

    private static String read(String path) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).getParent();
        return new String(Files.readAllBytes(root.resolve(path)), StandardCharsets.UTF_8);
    }
}
