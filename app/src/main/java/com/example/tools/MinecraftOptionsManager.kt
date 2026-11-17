package com.example.tools

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.core.LeviLogger
import com.example.model.GraphicsPreset
import com.example.model.MinecraftGraphicsOptions
import java.io.OutputStreamWriter

object MinecraftOptionsManager {

    private const val KEY_RENDER_DISTANCE = "gfx_renderdistance_new"
    private const val KEY_MAX_FRAMERATE = "gfx_max_framerate"
    private const val KEY_FANCY_GRAPHICS = "gfx_fancygraphics"
    private const val KEY_SMOOTH_LIGHTING = "gfx_smoothlighting"
    private const val KEY_FANCY_SKIES = "gfx_fancyskies"
    private const val KEY_VSYNC = "gfx_vsync"
    private const val KEY_MSAA = "gfx_msaa"
    private const val KEY_FOV = "gfx_field_of_view"
    private const val KEY_DEFERRED_RENDERING = "gfx_deferred_rendering"
    private const val KEY_UI_PROFILE = "ui_profile"

    fun parseOptionsText(
        rawText: String,
        fileUriString: String? = null,
        fileName: String = "options.txt"
    ): MinecraftGraphicsOptions {
        val lines = rawText.lines().filter { it.isNotEmpty() }
        val map = linkedMapOf<String, String>()
        for (line in lines) {
            val sep = line.indexOf(':')
            if (sep > 0) {
                val k = line.substring(0, sep).trim()
                val v = line.substring(sep + 1).trim()
                map[k] = v
            }
        }

        return MinecraftGraphicsOptions(
            fileUriString = fileUriString,
            fileName = fileName,
            renderDistanceChunks = map[KEY_RENDER_DISTANCE]?.toIntOrNull()?.coerceIn(4, 32) ?: 12,
            maxFramerate = map[KEY_MAX_FRAMERATE]?.toIntOrNull()?.coerceIn(20, 240) ?: 60,
            fancyGraphics = parseBool(map[KEY_FANCY_GRAPHICS], true),
            smoothLighting = parseBool(map[KEY_SMOOTH_LIGHTING], true),
            fancySkies = parseBool(map[KEY_FANCY_SKIES], true),
            vsyncEnabled = parseBool(map[KEY_VSYNC], true),
            msaaSamples = map[KEY_MSAA]?.toIntOrNull()?.coerceIn(1, 8) ?: 2,
            fieldOfView = map[KEY_FOV]?.toFloatOrNull()?.coerceIn(30f, 110f) ?: 60f,
            deferredRendering = parseBool(map[KEY_DEFERRED_RENDERING], false),
            uiProfileClassic = (map[KEY_UI_PROFILE]?.toIntOrNull() ?: 1) == 0,
            rawLineCount = lines.size,
            rawLines = lines
        )
    }

    fun serializeOptionsText(options: MinecraftGraphicsOptions): String {
        val updates = linkedMapOf(
            KEY_RENDER_DISTANCE to options.renderDistanceChunks.toString(),
            KEY_MAX_FRAMERATE to options.maxFramerate.toString(),
            KEY_FANCY_GRAPHICS to if (options.fancyGraphics) "1" else "0",
            KEY_SMOOTH_LIGHTING to if (options.smoothLighting) "1" else "0",
            KEY_FANCY_SKIES to if (options.fancySkies) "1" else "0",
            KEY_VSYNC to if (options.vsyncEnabled) "1" else "0",
            KEY_MSAA to options.msaaSamples.toString(),
            KEY_FOV to String.format(java.util.Locale.US, "%.1f", options.fieldOfView),
            KEY_DEFERRED_RENDERING to if (options.deferredRendering) "1" else "0",
            KEY_UI_PROFILE to if (options.uiProfileClassic) "0" else "1"
        )

        val seenKeys = mutableSetOf<String>()
        val outLines = mutableListOf<String>()

        for (line in options.rawLines) {
            val sep = line.indexOf(':')
            if (sep > 0) {
                val key = line.substring(0, sep).trim()
                val replacement = updates[key]
                if (replacement != null) {
                    outLines.add("$key:$replacement")
                    seenKeys.add(key)
                    continue
                }
            }
            outLines.add(line)
        }

        for ((k, v) in updates) {
            if (k !in seenKeys) {
                outLines.add("$k:$v")
            }
        }

        return outLines.joinToString("\n") + "\n"
    }

    fun applyPreset(
        current: MinecraftGraphicsOptions,
        preset: GraphicsPreset
    ): MinecraftGraphicsOptions {
        return when (preset) {
            GraphicsPreset.BATTERY_SAVER -> current.copy(
                renderDistanceChunks = 6,
                maxFramerate = 30,
                fancyGraphics = false,
                smoothLighting = false,
                fancySkies = false,
                vsyncEnabled = true,
                msaaSamples = 1,
                deferredRendering = false
            )
            GraphicsPreset.BALANCED -> current.copy(
                renderDistanceChunks = 12,
                maxFramerate = 60,
                fancyGraphics = true,
                smoothLighting = true,
                fancySkies = true,
                vsyncEnabled = true,
                msaaSamples = 2,
                deferredRendering = false
            )
            GraphicsPreset.COMPETITIVE_120FPS -> current.copy(
                renderDistanceChunks = 10,
                maxFramerate = 120,
                fancyGraphics = false,
                smoothLighting = true,
                fancySkies = false,
                vsyncEnabled = false,
                msaaSamples = 1,
                deferredRendering = false
            )
            GraphicsPreset.ULTRA_VISUALS -> current.copy(
                renderDistanceChunks = 20,
                maxFramerate = 60,
                fancyGraphics = true,
                smoothLighting = true,
                fancySkies = true,
                vsyncEnabled = true,
                msaaSamples = 4,
                deferredRendering = true
            )
        }
    }

    fun loadFromUri(
        context: Context,
        uri: Uri,
        logger: LeviLogger? = null
    ): Result<MinecraftGraphicsOptions> {
        return runCatching {
            val fileName = queryDisplayName(context, uri) ?: "options.txt"
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: error("Unable to open options.txt stream")
            val parsed = parseOptionsText(text, uri.toString(), fileName)
            logger?.success(
                "GRAPHICS",
                "Loaded $fileName (${parsed.rawLineCount} lines, render=${parsed.renderDistanceChunks}, fps=${parsed.maxFramerate})"
            )
            parsed
        }.onFailure { err ->
            logger?.error("GRAPHICS", "Failed loading options.txt: ${err.message}", err)
        }
    }

    fun saveToUri(
        context: Context,
        uri: Uri,
        options: MinecraftGraphicsOptions,
        logger: LeviLogger? = null
    ): Result<Unit> {
        return runCatching {
            val serialized = serializeOptionsText(options)
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Unable to open output stream for options.txt")
            out.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(serialized)
                    writer.flush()
                }
            }
            logger?.success(
                "GRAPHICS",
                "Saved options.txt (render=${options.renderDistanceChunks}, fps=${options.maxFramerate}, msaa=${options.msaaSamples})"
            )
            Unit
        }.onFailure { err ->
            logger?.error("GRAPHICS", "Failed saving options.txt: ${err.message}", err)
        }
    }

    private fun parseBool(raw: String?, default: Boolean): Boolean {
        return when (raw?.trim()?.lowercase()) {
            "1", "true" -> true
            "0", "false" -> false
            else -> default
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }
}
