package com.jarvis.assistant.terminal

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalExecutionEngine(
    private val context: Context,
    val bridge: TermuxBridge = TermuxBridge(context),
    val planner: AutonomousCommandPlanner = AutonomousCommandPlanner(),
    val toolRegistry: ToolDiscoveryRegistry = ToolDiscoveryRegistry(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _dashboardState = MutableStateFlow(
        TerminalDashboardState(
            isTermuxInstalled = bridge.isTermuxInstalled(),
            isTermuxPermissionGranted = bridge.hasRunCommandPermission()
        )
    )
    val dashboardState: StateFlow<TerminalDashboardState> = _dashboardState.asStateFlow()

    private var currentPlan: ExecutionPlan? = null
    private var pendingCommands = mutableListOf<TerminalCommand>()
    private var lastFailedCommand: TerminalCommand? = null

    fun executeAutonomousTask(
        userPrompt: String,
        workingDir: String? = null
    ) {
        scope.launch {
            // 1. Planning state
            updateState(
                currentTask = "Analyzing intent: \"$userPrompt\"",
                taskState = TerminalTaskState.PLANNING
            )
            appendLog("🧠 [AI Planner] Analyzing natural language: \"$userPrompt\"")
            delay(300)

            val plan = planner.planFromNaturalLanguage(userPrompt)
            currentPlan = plan
            pendingCommands = plan.commands.toMutableList()

            appendLog("📋 [Plan Generated] Task: ${plan.taskTitle} (${plan.commands.size} operations)")
            if (plan.requiredTools.isNotEmpty()) {
                updateState(taskState = TerminalTaskState.DISCOVERING_TOOLS)
                appendLog("🔍 [Tool Discovery] Required utilities: ${plan.requiredTools.joinToString { it.name }}")
                delay(200)
            }

            processNextCommand(workingDir)
        }
    }

    private suspend fun processNextCommand(workingDir: String?) {
        if (pendingCommands.isEmpty()) {
            updateState(
                taskState = TerminalTaskState.COMPLETED,
                verificationReport = "All operations executed and verified successfully."
            )
            appendLog("✅ [Task Complete] All planned operations verified.")
            return
        }

        val cmd = pendingCommands.removeAt(0)

        // Security check Level 3
        if (cmd.riskLevel == RiskLevel.LEVEL_3_RESTRICTED) {
            updateState(
                taskState = TerminalTaskState.FAILED,
                stderr = "Security Block: Command '${cmd.rawCommand}' classified as Level 3 Restricted."
            )
            appendLog("🛑 [Security Violation] Blocked potentially destructive command: ${cmd.rawCommand}")
            return
        }

        // Level 2 requires user approval
        if (cmd.riskLevel == RiskLevel.LEVEL_2_APPROVAL_REQUIRED) {
            updateState(
                currentCommand = cmd.rawCommand,
                taskState = TerminalTaskState.WAITING_FOR_APPROVAL,
                pendingApprovalCommand = cmd
            )
            appendLog("⚠️ [Approval Required] Operation '${cmd.rawCommand}' (${cmd.explanation}) awaits authorization.")
            return
        }

        // Level 1: Execute automatically
        runCommandInternal(cmd, workingDir)
    }

    fun approveCommand(workingDir: String? = null) {
        val cmd = _dashboardState.value.pendingApprovalCommand ?: return
        scope.launch {
            _dashboardState.value = _dashboardState.value.copy(pendingApprovalCommand = null)
            appendLog("🔓 [Authorized] Operator approved execution: ${cmd.rawCommand}")
            runCommandInternal(cmd, workingDir)
        }
    }

    fun rejectCommand() {
        val cmd = _dashboardState.value.pendingApprovalCommand
        scope.launch {
            _dashboardState.value = _dashboardState.value.copy(
                pendingApprovalCommand = null,
                taskState = TerminalTaskState.CANCELLED
            )
            appendLog("❌ [Rejected] Operator declined execution: ${cmd?.rawCommand ?: "Unknown"}")
        }
    }

    private suspend fun runCommandInternal(cmd: TerminalCommand, workingDir: String?) {
        updateState(
            currentCommand = cmd.rawCommand,
            taskState = TerminalTaskState.EXECUTING,
            activeToolName = cmd.executable
        )
        appendLog("🚀 [Exec] Running: ${cmd.rawCommand}")

        val result = bridge.executeShellCommand(
            command = cmd.rawCommand,
            workingDir = workingDir,
            timeoutSec = 45L
        )

        updateState(
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode,
            elapsedMs = result.durationMs,
            taskState = TerminalTaskState.ANALYZING
        )

        if (result.stdout.isNotBlank()) {
            appendLog("📄 [Output] ${result.stdout.lines().take(3).joinToString(" | ")}")
        }

        if (result.isSuccess) {
            appendLog("✔ [Verified] Command succeeded (exit code 0 in ${result.durationMs}ms)")
            delay(200)
            processNextCommand(workingDir)
        } else {
            lastFailedCommand = cmd
            appendLog("❌ [Failed] Exit code ${result.exitCode}: ${result.stderr.take(80)}")

            // Error Recovery attempt
            attemptErrorRecovery(cmd, result, workingDir)
        }
    }

    private suspend fun attemptErrorRecovery(
        failedCmd: TerminalCommand,
        result: ExecutionResult,
        workingDir: String?
    ) {
        updateState(taskState = TerminalTaskState.RECOVERING)
        appendLog("🔧 [Recovery Engine] Analyzing error pattern...")

        val err = (result.stderr + " " + result.stdout).lowercase()

        // Check if missing binary
        val matchingTool = toolRegistry.findToolByBinary(failedCmd.executable)
        if (err.contains("not found") && matchingTool != null) {
            appendLog("💡 [Diagnosis] Missing binary '${failedCmd.executable}' found in tool catalogue (package: '${matchingTool.packageName}')")
            val recoveryCmd = TerminalCommand(
                rawCommand = "pkg install -y ${matchingTool.packageName}",
                executable = "pkg",
                arguments = listOf("install", "-y", matchingTool.packageName),
                riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
                explanation = "Install missing prerequisite tool '${matchingTool.name}' for failed operation.",
                isPackageInstall = true,
                targetPackage = matchingTool.packageName
            )
            pendingCommands.add(0, failedCmd) // Re-queue original after fix
            updateState(
                taskState = TerminalTaskState.WAITING_FOR_APPROVAL,
                pendingApprovalCommand = recoveryCmd,
                currentCommand = recoveryCmd.rawCommand
            )
            return
        }

        updateState(
            taskState = TerminalTaskState.FAILED,
            verificationReport = "Command failed without autonomous recovery: ${result.errorDiagnosis ?: result.stderr}"
        )
    }

    fun cancelActiveTask() {
        pendingCommands.clear()
        updateState(
            taskState = TerminalTaskState.CANCELLED,
            pendingApprovalCommand = null
        )
        appendLog("⏹ [Aborted] Task cancelled by operator.")
    }

    fun retryLastCommand(workingDir: String? = null) {
        val cmd = lastFailedCommand ?: return
        scope.launch {
            appendLog("🔁 [Retry] Retrying: ${cmd.rawCommand}")
            runCommandInternal(cmd, workingDir)
        }
    }

    fun clearLog() {
        _dashboardState.value = _dashboardState.value.copy(
            executionLog = emptyList(),
            stdout = "",
            stderr = "",
            exitCode = null
        )
    }

    private fun appendLog(line: String) {
        val updated = _dashboardState.value.executionLog + line
        _dashboardState.value = _dashboardState.value.copy(executionLog = updated.takeLast(100))
    }

    private fun updateState(
        currentTask: String? = null,
        taskState: TerminalTaskState? = null,
        currentCommand: String? = null,
        activeToolName: String? = null,
        stdout: String? = null,
        stderr: String? = null,
        exitCode: Int? = null,
        elapsedMs: Long? = null,
        pendingApprovalCommand: TerminalCommand? = null,
        verificationReport: String? = null
    ) {
        _dashboardState.value = _dashboardState.value.copy(
            currentTask = currentTask ?: _dashboardState.value.currentTask,
            taskState = taskState ?: _dashboardState.value.taskState,
            currentCommand = currentCommand ?: _dashboardState.value.currentCommand,
            activeToolName = activeToolName ?: _dashboardState.value.activeToolName,
            stdout = stdout ?: _dashboardState.value.stdout,
            stderr = stderr ?: _dashboardState.value.stderr,
            exitCode = exitCode ?: _dashboardState.value.exitCode,
            elapsedMs = elapsedMs ?: _dashboardState.value.elapsedMs,
            pendingApprovalCommand = pendingApprovalCommand,
            verificationReport = verificationReport ?: _dashboardState.value.verificationReport
        )
    }
}
