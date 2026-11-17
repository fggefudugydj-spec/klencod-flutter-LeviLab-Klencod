package com.example.engine

import com.example.model.ElfInspectionResult
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ElfValidator {

    const val MIN_MAIN_LIBRARY_BYTES: Long = 1_048_576L // 1 MiB (1024 * 1024)
    private const val MIN_ELF_HEADER_BYTES = 20

    fun isElfMagic(header: ByteArray): Boolean {
        return header.size >= 4 &&
            header[0] == 0x7F.toByte() &&
            header[1] == 0x45.toByte() && // 'E'
            header[2] == 0x4C.toByte() && // 'L'
            header[3] == 0x46.toByte()    // 'F'
    }

    fun validateLibrary(
        file: File,
        sourceOrigin: String = "extracted",
        requireMinOneMb: Boolean = file.name == "libminecraftpe.so"
    ): ElfInspectionResult {
        val isMain = file.name == "libminecraftpe.so"
        if (!file.exists() || !file.isFile) {
            return ElfInspectionResult(
                fileName = file.name,
                filePath = file.absolutePath,
                sourceOrigin = sourceOrigin,
                sizeBytes = 0L,
                hasElfMagic = false,
                isElf64 = false,
                isAArch64 = false,
                machineName = "MISSING",
                meetsMinSize = false,
                isMainLibrary = isMain,
                isValid = false,
                validationNote = "File does not exist"
            )
        }

        val sizeBytes = file.length()
        val meetsMinSize = if (requireMinOneMb) {
            sizeBytes > MIN_MAIN_LIBRARY_BYTES
        } else {
            sizeBytes > MIN_ELF_HEADER_BYTES.toLong()
        }

        val header = ByteArray(MIN_ELF_HEADER_BYTES)
        val bytesRead = try {
            RandomAccessFile(file, "r").use { raf ->
                raf.read(header)
            }
        } catch (t: Throwable) {
            return ElfInspectionResult(
                fileName = file.name,
                filePath = file.absolutePath,
                sourceOrigin = sourceOrigin,
                sizeBytes = sizeBytes,
                hasElfMagic = false,
                isElf64 = false,
                isAArch64 = false,
                machineName = "READ_ERROR",
                meetsMinSize = meetsMinSize,
                isMainLibrary = isMain,
                isValid = false,
                validationNote = "Failed to read ELF header: ${t.message}"
            )
        }

        if (bytesRead < MIN_ELF_HEADER_BYTES) {
            return ElfInspectionResult(
                fileName = file.name,
                filePath = file.absolutePath,
                sourceOrigin = sourceOrigin,
                sizeBytes = sizeBytes,
                hasElfMagic = false,
                isElf64 = false,
                isAArch64 = false,
                machineName = "TRUNCATED",
                meetsMinSize = meetsMinSize,
                isMainLibrary = isMain,
                isValid = false,
                validationNote = "File smaller than 20-byte ELF header ($bytesRead bytes)"
            )
        }

        val hasMagic = isElfMagic(header)
        if (!hasMagic) {
            return ElfInspectionResult(
                fileName = file.name,
                filePath = file.absolutePath,
                sourceOrigin = sourceOrigin,
                sizeBytes = sizeBytes,
                hasElfMagic = false,
                isElf64 = false,
                isAArch64 = false,
                machineName = "NOT_ELF",
                meetsMinSize = meetsMinSize,
                isMainLibrary = isMain,
                isValid = false,
                validationNote = "Invalid magic bytes (expected 7F 45 4C 46)"
            )
        }

        val eiClass = header[4].toInt() and 0xFF
        val eiData = header[5].toInt() and 0xFF
        val isElf64 = eiClass == 2
        val byteOrder = if (eiData == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        val machine = ByteBuffer.wrap(header, 18, 2).order(byteOrder).short.toInt() and 0xFFFF
        val isAArch64 = machine == 0xB7
        val machineName = when (machine) {
            0xB7 -> "AArch64 (arm64-v8a)"
            0x28 -> "ARM (armeabi-v7a)"
            0x3E -> "x86_64"
            0x03 -> "x86"
            else -> "0x${machine.toString(16)}"
        }

        val valid = hasMagic && meetsMinSize && (!isMain || isElf64)
        val note = when {
            requireMinOneMb && !meetsMinSize ->
                "Rejected: size $sizeBytes bytes <= 1 MB ($MIN_MAIN_LIBRARY_BYTES bytes)"
            isMain && !isElf64 ->
                "Rejected: not ELF64 ($machineName)"
            else ->
                "Valid ${if (isElf64) "ELF64" else "ELF32"} $machineName"
        }

        return ElfInspectionResult(
            fileName = file.name,
            filePath = file.absolutePath,
            sourceOrigin = sourceOrigin,
            sizeBytes = sizeBytes,
            hasElfMagic = hasMagic,
            isElf64 = isElf64,
            isAArch64 = isAArch64,
            machineName = machineName,
            meetsMinSize = meetsMinSize,
            isMainLibrary = isMain,
            isValid = valid,
            validationNote = note
        )
    }

    fun isValidMinecraftMainLibrary(file: File): Boolean {
        return validateLibrary(file = file, requireMinOneMb = true).isValid
    }
}
