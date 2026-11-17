package com.example.core

import android.content.Context
import com.example.model.LogEntry
import com.example.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class LeviLogger private constructor(private val logFile: File) {

    private val lock = Any()
    private val nextId = AtomicLong(1L)
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    val filePath: String
        get() = logFile.absolutePath

    init {
        logFile.parentFile?.mkdirs()
        if (!logFile.exists()) {
            logFile.createNewFile()
        } else {
            loadExistingTail()
        }
    }

    private fun loadExistingTail() {
        synchronized(lock) {
            try {
                val lines = logFile.readLines(StandardCharsets.UTF_8).takeLast(300)
                val parsed = lines.mapNotNull { parseLine(it) }
                _entries.value = parsed
            } catch (_: Throwable) {
                // Ignore read issues during init
            }
        }
    }

    private fun parseLine(line: String): LogEntry? {
        if (line.isBlank()) return null
        val match = Regex("""^\[([^\]]+)] \[([A-Z]+)] \[([^\]]+)] (.*)$""").find(line)
        return if (match != null) {
            val (ts, lvl, stage, msg) = match.destructured
            val level = runCatching { LogLevel.valueOf(lvl) }.getOrDefault(LogLevel.INFO)
            LogEntry(
                id = nextId.getAndIncrement(),
                timestamp = ts,
                level = level,
                stage = stage,
                message = msg
            )
        } else {
            LogEntry(
                id = nextId.getAndIncrement(),
                timestamp = "--",
                level = LogLevel.INFO,
                stage = "RAW",
                message = line
            )
        }
    }

    fun log(level: LogLevel, stage: String, message: String, throwable: Throwable? = null) {
        val ts = synchronized(lock) { dateFormat.format(Date()) }
        val fullMessage = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            "$message\n${sw.toString().trimEnd()}"
        } else {
            message
        }
        val entry = LogEntry(
            id = nextId.getAndIncrement(),
            timestamp = ts,
            level = level,
            stage = stage,
            message = fullMessage
        )
        val formattedLine = "[$ts] [${level.name}] [$stage] ${fullMessage.replace("\n", " | ")}"
        synchronized(lock) {
            try {
                logFile.parentFile?.mkdirs()
                FileOutputStream(logFile, true).use { fos ->
                    OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                        writer.write(formattedLine)
                        writer.write("\n")
                        writer.flush()
                    }
                    fos.fd.sync()
                }
            } catch (_: Throwable) {
                // Best effort disk flush
            }
            val updated = (_entries.value + entry).takeLast(400)
            _entries.value = updated
        }
    }

    fun info(stage: String, message: String) = log(LogLevel.INFO, stage, message)
    fun success(stage: String, message: String) = log(LogLevel.SUCCESS, stage, message)
    fun warn(stage: String, message: String) = log(LogLevel.WARN, stage, message)
    fun error(stage: String, message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, stage, message, throwable)

    fun clear() {
        synchronized(lock) {
            try {
                logFile.writeText("", StandardCharsets.UTF_8)
            } catch (_: Throwable) {
            }
            _entries.value = emptyList()
        }
    }

    fun readFullFileText(): String {
        synchronized(lock) {
            return try {
                if (logFile.exists()) {
                    logFile.readText(StandardCharsets.UTF_8)
                } else {
                    ""
                }
            } catch (t: Throwable) {
                _entries.value.joinToString("\n") {
                    "[${it.timestamp}] [${it.level.name}] [${it.stage}] ${it.message}"
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: LeviLogger? = null

        @Volatile
        private var crashHandlerInstalled = false

        fun getInstance(context: Context): LeviLogger {
            return instance ?: synchronized(this) {
                instance ?: LeviLogger(
                    File(File(context.filesDir, "logs"), "levilab_session.log")
                ).also { created ->
                    instance = created
                    installCrashHandler(created)
                }
            }
        }

        private fun installCrashHandler(logger: LeviLogger) {
            if (crashHandlerInstalled) return
            crashHandlerInstalled = true
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                logger.error(
                    stage = "CRASH",
                    message = "Uncaught exception on thread ${thread.name}: ${throwable.javaClass.simpleName}: ${throwable.message}",
                    throwable = throwable
                )
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
