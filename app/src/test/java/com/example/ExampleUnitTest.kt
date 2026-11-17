package com.example

import com.example.engine.Elf64Parser
import com.example.engine.ElfValidator
import com.example.engine.MinecraftPackageDiscovery
import com.example.model.GraphicsPreset
import com.example.model.InstalledMinecraftPackage
import com.example.tools.BedrockServerPinger
import com.example.tools.MinecraftOptionsManager
import com.example.tools.WorldBackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ExampleUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `text file named libminecraftpe_so is rejected even when larger than 1MB`() {
        val fakeLib = File(tempFolder.root, "libminecraftpe.so")
        val textBlock = "This is plain text pretending to be libminecraftpe.so\n".repeat(25_000)
        fakeLib.writeText(textBlock, Charsets.UTF_8)
        assertTrue(fakeLib.length() > ElfValidator.MIN_MAIN_LIBRARY_BYTES)

        val result = ElfValidator.validateLibrary(fakeLib, requireMinOneMb = true)
        assertFalse(result.hasElfMagic)
        assertFalse(result.isValid)
        assertFalse(ElfValidator.isValidMinecraftMainLibrary(fakeLib))
    }

    @Test
    fun `elf file named libminecraftpe_so smaller than 1MB is rejected`() {
        val smallElf = File(tempFolder.root, "libminecraftpe.so")
        val bytes = ByteArray(1024)
        writeElf64AArch64Header(bytes)
        smallElf.writeBytes(bytes)

        val result = ElfValidator.validateLibrary(smallElf, requireMinOneMb = true)
        assertTrue(result.hasElfMagic)
        assertFalse(result.meetsMinSize)
        assertFalse(result.isValid)
    }

    @Test
    fun `valid elf64 file named libminecraftpe_so larger than 1MB is accepted`() {
        val validElf = File(tempFolder.root, "libminecraftpe.so")
        val totalSize = ElfValidator.MIN_MAIN_LIBRARY_BYTES + 4096L
        RandomAccessFile(validElf, "rw").use { raf ->
            raf.setLength(totalSize)
            val header = ByteArray(64)
            writeElf64AArch64Header(header)
            raf.seek(0L)
            raf.write(header)
        }

        val result = ElfValidator.validateLibrary(validElf, requireMinOneMb = true)
        assertTrue(result.hasElfMagic)
        assertTrue(result.isElf64)
        assertTrue(result.isAArch64)
        assertTrue(result.meetsMinSize)
        assertTrue(result.isValid)
        assertTrue(ElfValidator.isValidMinecraftMainLibrary(validElf))
    }

    @Test
    fun `elf64 parser extracts dt_needed and resolves topological load order`() {
        val elfFile = File(tempFolder.root, "libminecraftpe.so")
        val dynstrText = "\u0000libfmod.so\u0000libc++_shared.so\u0000liblog.so\u0000"
        val dynstrBytes = dynstrText.toByteArray(Charsets.UTF_8)
        val dynstrOffset = 0x200L
        val dynamicOffset = 0x300L
        val fileSize = 0x1000L

        val buf = ByteBuffer.allocate(fileSize.toInt()).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, 0x7F.toByte())
        buf.put(1, 'E'.code.toByte())
        buf.put(2, 'L'.code.toByte())
        buf.put(3, 'F'.code.toByte())
        buf.put(4, 2.toByte())
        buf.put(5, 1.toByte())
        buf.put(6, 1.toByte())
        buf.putShort(16, 3.toShort())
        buf.putShort(18, 0xB7.toShort())
        buf.putLong(32, 64L)
        buf.putShort(54, 56.toShort())
        buf.putShort(56, 2.toShort())

        val ph0 = 64
        buf.putInt(ph0 + 0, 1)
        buf.putLong(ph0 + 8, 0L)
        buf.putLong(ph0 + 16, 0L)
        buf.putLong(ph0 + 32, fileSize)

        val ph1 = 64 + 56
        buf.putInt(ph1 + 0, 2)
        buf.putLong(ph1 + 8, dynamicOffset)
        buf.putLong(ph1 + 16, dynamicOffset)
        buf.putLong(ph1 + 32, 6L * 16L)

        System.arraycopy(dynstrBytes, 0, buf.array(), dynstrOffset.toInt(), dynstrBytes.size)

        var dOff = dynamicOffset.toInt()
        fun putDyn(tag: Long, value: Long) {
            buf.putLong(dOff, tag)
            buf.putLong(dOff + 8, value)
            dOff += 16
        }
        putDyn(5L, dynstrOffset)
        putDyn(10L, dynstrBytes.size.toLong())
        putDyn(1L, dynstrText.indexOf("libfmod.so").toLong())
        putDyn(1L, dynstrText.indexOf("libc++_shared.so").toLong())
        putDyn(1L, dynstrText.indexOf("liblog.so").toLong())
        putDyn(0L, 0L)

        elfFile.writeBytes(buf.array())

        val info = Elf64Parser.parseDynamicInfo(elfFile)
        assertNull(info.parseError)
        assertEquals(listOf("libfmod.so", "libc++_shared.so", "liblog.so"), info.dtNeeded)

        val fmodFile = File(tempFolder.root, "libfmod.so").apply { writeBytes(ByteArray(64)) }
        val cppFile = File(tempFolder.root, "libc++_shared.so").apply { writeBytes(ByteArray(64)) }
        val resolution = Elf64Parser.resolveLoadOrder(
            mainLibraryName = "libminecraftpe.so",
            extractedFilesByName = mapOf(
                "libminecraftpe.so" to elfFile,
                "libfmod.so" to fmodFile,
                "libc++_shared.so" to cppFile
            )
        )
        assertEquals(listOf("libfmod.so", "libc++_shared.so", "libminecraftpe.so"), resolution.bundledLoadOrder)
        assertEquals(listOf("liblog.so"), resolution.systemDependencies)
        assertTrue(resolution.missingDependencies.isEmpty())
    }

    @Test
    fun `version matching and package selection prioritize preview 1_26_70_24`() {
        assertTrue(MinecraftPackageDiscovery.isTargetVersionMatch("1.26.70.24"))
        assertTrue(MinecraftPackageDiscovery.isTargetVersionMatch("1.26.70.24-preview"))
        assertFalse(MinecraftPackageDiscovery.isTargetVersionMatch("1.21.50.07"))

        val releasePkg = InstalledMinecraftPackage(
            packageName = "com.mojang.minecraftpe",
            appLabel = "Minecraft",
            versionName = "1.26.70.24",
            versionCode = 100L,
            sourceDir = "",
            splitSourceDirs = emptyList(),
            nativeLibraryDir = "",
            supportedAbis = listOf("arm64-v8a"),
            matchesTargetVersion = true,
            isPreviewPackage = false
        )
        val previewPkg = InstalledMinecraftPackage(
            packageName = "com.mojang.minecraftpe.preview",
            appLabel = "Minecraft Preview",
            versionName = "1.26.70.24",
            versionCode = 101L,
            sourceDir = "",
            splitSourceDirs = emptyList(),
            nativeLibraryDir = "",
            supportedAbis = listOf("arm64-v8a"),
            matchesTargetVersion = true,
            isPreviewPackage = true
        )
        val best = MinecraftPackageDiscovery.selectBestPackage(listOf(releasePkg, previewPkg))
        assertEquals("com.mojang.minecraftpe.preview", best?.packageName)
    }

    @Test
    fun `options txt parser and serializer preserve unknown keys and apply presets`() {
        val raw = """
            mp_username:Steve
            gfx_renderdistance_new:8
            gfx_max_framerate:60
            gfx_fancygraphics:1
            custom_unknown_flag:42
        """.trimIndent()

        val parsed = MinecraftOptionsManager.parseOptionsText(raw)
        assertEquals(8, parsed.renderDistanceChunks)
        assertEquals(60, parsed.maxFramerate)
        assertTrue(parsed.fancyGraphics)

        val tuned = MinecraftOptionsManager.applyPreset(parsed, GraphicsPreset.COMPETITIVE_120FPS)
        assertEquals(120, tuned.maxFramerate)
        assertEquals(10, tuned.renderDistanceChunks)
        assertFalse(tuned.vsyncEnabled)

        val serialized = MinecraftOptionsManager.serializeOptionsText(tuned)
        assertTrue(serialized.contains("mp_username:Steve"))
        assertTrue(serialized.contains("custom_unknown_flag:42"))
        assertTrue(serialized.contains("gfx_max_framerate:120"))
        assertTrue(serialized.contains("gfx_vsync:0"))
    }

    @Test
    fun `zip path sanitizer blocks traversal and accepts valid world entries`() {
        assertNull(WorldBackupManager.sanitizeZipRelativePath("../evil/level.dat"))
        assertNull(WorldBackupManager.sanitizeZipRelativePath("db/../../etc/passwd"))
        assertNull(WorldBackupManager.sanitizeZipRelativePath("C:/bad/path"))
        assertNull(WorldBackupManager.sanitizeZipRelativePath(""))

        val valid = WorldBackupManager.sanitizeZipRelativePath("db/000001.ldb")
        assertNotNull(valid)
        assertEquals(listOf("db", "000001.ldb"), valid)
    }

    @Test
    fun `raknet unconnected pong parser extracts server metadata`() {
        val motdPayload = "MCPE;LeviLab Test;748;1.26.70;12;100;12345;Survival World;Survival;1;19132;19133;"
        val strBytes = motdPayload.toByteArray(Charsets.UTF_8)
        val packet = ByteBuffer.allocate(35 + strBytes.size).order(ByteOrder.BIG_ENDIAN).apply {
            put(0x1C.toByte())
            putLong(1000L)
            putLong(9999L)
            put(ByteArray(16))
            putShort(strBytes.size.toShort())
            put(strBytes)
        }.array()

        val result = BedrockServerPinger.parseUnconnectedPong("s1", packet, packet.size, 24L)
        assertTrue(result.isOnline)
        assertEquals("LeviLab Test", result.motd)
        assertEquals("1.26.70", result.versionName)
        assertEquals(12, result.onlinePlayers)
        assertEquals(100, result.maxPlayers)
        assertEquals("Survival", result.gameMode)
        assertEquals(24L, result.latencyMs)
    }

    private fun writeElf64AArch64Header(bytes: ByteArray) {
        bytes[0] = 0x7F.toByte()
        bytes[1] = 'E'.code.toByte()
        bytes[2] = 'L'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        bytes[4] = 2.toByte()
        bytes[5] = 1.toByte()
        bytes[6] = 1.toByte()
        bytes[18] = 0xB7.toByte()
        bytes[19] = 0x00.toByte()
    }
}
