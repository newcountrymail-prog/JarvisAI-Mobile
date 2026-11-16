package com.jarvis.assistant.terminal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class TermuxBridge(private val context: Context) {

    companion object {
        const val TERMUX_PACKAGE_NAME = "com.termux"
        const val TERMUX_RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    }

    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getTermuxVersion(): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(TERMUX_PACKAGE_NAME, 0)
            pInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    fun hasRunCommandPermission(): Boolean {
        val perm = ContextCompat.checkSelfPermission(context, TERMUX_RUN_COMMAND_PERMISSION)
        return perm == PackageManager.PERMISSION_GRANTED
    }

    fun launchTermux(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE_NAME)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            false
        }
    }

    fun dispatchTermuxIntent(
        commandPath: String,
        arguments: Array<String> = emptyArray(),
        workDir: String = "/data/data/com.termux/files/home",
        inBackground: Boolean = true
    ): Boolean {
        if (!isTermuxInstalled()) return false

        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE_NAME, "com.termux.app.RunCommandService")
            putExtra(EXTRA_COMMAND_PATH, commandPath)
            putExtra(EXTRA_ARGUMENTS, arguments)
            putExtra(EXTRA_WORKDIR, workDir)
            putExtra(EXTRA_BACKGROUND, inBackground)
            putExtra(EXTRA_SESSION_ACTION, "0")
        }

        return try {
            context.startService(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executeShellCommand(
        command: String,
        workingDir: String? = null,
        timeoutSec: Long = 30L
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutSb = StringBuilder()
        val stderrSb = StringBuilder()

        try {
            val processBuilder = ProcessBuilder("/system/bin/sh", "-c", command)
            if (workingDir != null) {
                val dirFile = File(workingDir)
                if (dirFile.exists() && dirFile.isDirectory) {
                    processBuilder.directory(dirFile)
                }
            }

            // Clean environment variables for consistent execution
            val env = processBuilder.environment()
            env["PATH"] = (env["PATH"] ?: "") + ":/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin"
            env["HOME"] = workingDir ?: context.filesDir.absolutePath
            env["PREFIX"] = "/data/data/com.termux/files/usr"
            env["LANG"] = "en_US.UTF-8"

            val process = processBuilder.start()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutThread = Thread {
                try {
                    var line: String?
                    while (stdoutReader.readLine().also { line = it } != null) {
                        stdoutSb.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    var line: String?
                    while (stderrReader.readLine().also { line = it } != null) {
                        stderrSb.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val exitedInTime = process.waitFor(timeoutSec, TimeUnit.SECONDS)

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val duration = System.currentTimeMillis() - startTime

            if (!exitedInTime) {
                process.destroyForcibly()
                return@withContext ExecutionResult(
                    command = command,
                    exitCode = -1,
                    stdout = stdoutSb.toString().trim(),
                    stderr = "Command execution timed out after ${timeoutSec}s.",
                    durationMs = duration,
                    isSuccess = false,
                    errorDiagnosis = "Execution timeout reached.",
                    suggestedFix = "Run command in background mode or verify network/resource availability."
                )
            }

            val exitCode = process.exitValue()
            val isSuccess = exitCode == 0

            var diagnosis: String? = null
            var fix: String? = null

            if (!isSuccess) {
                val err = stderrSb.toString().lowercase()
                when {
                    err.contains("not found") || err.contains("no such file") -> {
                        diagnosis = "Executable or file path was not found in the current environment."
                        fix = "Verify tool installation status via Package Manager or check spelling."
                    }
                    err.contains("permission denied") -> {
                        diagnosis = "Android sandboxing restricted execution permissions for this path."
                        fix = "Check storage access permissions or execute inside Termux internal files space."
                    }
                    err.contains("network") || err.contains("could not resolve host") -> {
                        diagnosis = "Network connectivity issue prevented remote package or repository access."
                        fix = "Verify internet connection or DNS diagnostic resolution."
                    }
                    else -> {
                        diagnosis = "Process terminated with non-zero exit status $exitCode."
                    }
                }
            }

            ExecutionResult(
                command = command,
                exitCode = exitCode,
                stdout = stdoutSb.toString().trim(),
                stderr = stderrSb.toString().trim(),
                durationMs = duration,
                isSuccess = isSuccess,
                errorDiagnosis = diagnosis,
                suggestedFix = fix
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            ExecutionResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = e.localizedMessage ?: "Unknown shell process failure.",
                durationMs = duration,
                isSuccess = false,
                errorDiagnosis = "Exception spawned during process invocation.",
                suggestedFix = "Verify shell availability (/system/bin/sh) and Android permissions."
            )
        }
    }

    fun getSetupInstructions(): String {
        return """
        Termux Integration Setup:
        1. Install Termux from F-Droid or official release.
        2. In Termux, run: pkg update && pkg upgrade
        3. If using Termux:Tasker or RUN_COMMAND, grant 'Allow external apps to run commands' in ~/.termux/termux.properties:
           echo "allow-external-apps = true" >> ~/.termux/termux.properties
        4. JARVIS X automatically bridges local shell commands and verifies all output.
        """.trimIndent()
    }
}
