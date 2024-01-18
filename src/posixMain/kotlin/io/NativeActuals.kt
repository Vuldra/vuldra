package io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import platform.posix.*

actual suspend fun findExecutable(executable: String): String =
    executable

/**
 * https://stackoverflow.com/questions/57123836/kotlin-native-execute-command-and-get-the-output
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun executeExternalCommandAndCaptureOutput(
    command: List<String>, // "find . -name .git"
    options: ExecuteCommandOptions
): String {
    chdir(options.directory)
    val commandToExecute = command.joinToString(separator = " ") { arg ->
        if (arg.contains(" ")) "'$arg'" else arg
    }
    val redirect = if (options.redirectStderr) " 2>&1 " else ""
    val fp = popen("$commandToExecute $redirect", "r") ?: error("Failed to run command: $command")

    val stdout = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
            append(input.toKString())
        }
    }

    val status = pclose(fp)
    if (status != 0 && options.abortOnError) {
        println(stdout)
        throw Exception("Command `$command` failed with status $status${if (options.redirectStderr) ": $stdout" else ""}")
    }

    return if (options.trim) stdout.trim() else stdout
}

actual suspend fun pwd(options: ExecuteCommandOptions): String {
    return executeExternalCommandAndCaptureOutput(listOf("pwd"), options).trim()
}

actual fun getEnvironmentVariable(name: String): String? =
    getenv(name)?.toKString()

actual fun localUserConfigDirectory(): String =
    getEnvironmentVariable("HOME") ?: error("HOME environment variable not defined")

actual fun runBlocking(block: suspend () -> Unit) =
    runBlocking { block() }