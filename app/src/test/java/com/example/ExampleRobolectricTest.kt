package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.core.AppLanguage
import com.example.core.LeviLogger
import com.example.core.LeviStrings
import com.example.core.stringsFor
import com.example.engine.ElfValidator
import com.example.engine.NativeLibraryManager
import com.example.model.InstalledMinecraftPackage
import com.example.model.LibraryLoadSource
import com.example.ui.LeviApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LeviLab", appName)
    }

    @Test
    fun `arabic and english strings are both populated`() {
        val ar: LeviStrings = stringsFor(AppLanguage.AR)
        val en: LeviStrings = stringsFor(AppLanguage.EN)
        assertTrue(ar.launchPrimaryButton.isNotBlank())
        assertTrue(en.launchPrimaryButton.isNotBlank())
        assertEquals("LeviLab", ar.appTitle)
        assertEquals("LeviLab", en.appTitle)
    }

    @Test
    fun `split apk extraction resolves stored libminecraftpe_so and prunes stale cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logger = LeviLogger.getInstance(context)

        val baseApk = File(tempFolder.root, "base.apk")
        ZipOutputStream(FileOutputStream(baseApk)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(byteArrayOf(1, 2, 3, 4))
            zos.closeEntry()
        }

        // Create split_config.arm64_v8a.apk with STORED uncompressed ELF64 > 1MB libminecraftpe.so
        val splitApk = File(tempFolder.root, "split_config.arm64_v8a.apk")
        val libBytes = ByteArray(ElfValidator.MIN_MAIN_LIBRARY_BYTES.toInt() + 4096)
        libBytes[0] = 0x7F.toByte()
        libBytes[1] = 'E'.code.toByte()
        libBytes[2] = 'L'.code.toByte()
        libBytes[3] = 'F'.code.toByte()
        libBytes[4] = 2.toByte()
        libBytes[5] = 1.toByte()
        libBytes[6] = 1.toByte()
        libBytes[18] = 0xB7.toByte()
        val crc = CRC32().apply { update(libBytes) }.value

        ZipOutputStream(FileOutputStream(splitApk)).use { zos ->
            val entry = ZipEntry("lib/arm64-v8a/libminecraftpe.so").apply {
                method = ZipEntry.STORED
                size = libBytes.size.toLong()
                compressedSize = libBytes.size.toLong()
                this.crc = crc
            }
            zos.putNextEntry(entry)
            zos.write(libBytes)
            zos.closeEntry()
        }

        // Create a stale old cache dir that should get pruned
        val staleDir = File(NativeLibraryManager.cacheRootDir(context), "com.mojang.minecraftpe.preview/999/arm64-v8a")
        staleDir.mkdirs()
        File(staleDir, "old.so").writeText("stale")

        val pkg = InstalledMinecraftPackage(
            packageName = "com.mojang.minecraftpe.preview",
            appLabel = "Minecraft Preview",
            versionName = "1.26.70.24",
            versionCode = 952607024L,
            lastUpdateTime = 1700000000L,
            sourceDir = baseApk.absolutePath,
            splitSourceDirs = listOf(splitApk.absolutePath),
            splitNames = listOf("config.arm64_v8a"),
            nativeLibraryDir = "",
            supportedAbis = listOf("arm64-v8a"),
            matchesTargetVersion = true,
            isPreviewPackage = true,
            extractNativeLibs = false
        )

        val results = NativeLibraryManager.inspectExistingOrExtract(
            context = context,
            pkg = pkg,
            logger = logger,
            forceReextract = true
        )

        assertEquals(1, results.size)
        val mainLib = results.first()
        assertEquals("libminecraftpe.so", mainLib.fileName)
        assertTrue(mainLib.isValid)
        assertTrue(mainLib.isAArch64)
        assertEquals(LibraryLoadSource.APK_DIRECT_ZIP, mainLib.loadSource)
        assertTrue(mainLib.loadablePath.endsWith("split_config.arm64_v8a.apk!/lib/arm64-v8a/libminecraftpe.so"))
        assertTrue(!staleDir.exists())
    }

    @Test
    fun `levi app renders navigation tabs and language toggle`() {
        composeTestRule.setContent {
            LeviApp()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_launch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_probe").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tools").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_logs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_language_button").assertIsDisplayed()
    }
}
