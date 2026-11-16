package com.jarvis.assistant.terminal

enum class RiskLevel(val label: String, val description: String) {
    LEVEL_1_SAFE_AUTO(
        "Safe / Automatic",
        "Read-only system inspections, directory listing, dependency status, and environment checks."
    ),
    LEVEL_2_APPROVAL_REQUIRED(
        "Approval Required",
        "Package installations, project builds, file creations/modifications, or service execution."
    ),
    LEVEL_3_RESTRICTED(
        "Restricted / Blocked",
        "Destructive deletions, credential access, unauthorized scanning, or protected system tampering."
    );

    companion object {
        val LEVEL_1_SAFE: RiskLevel = LEVEL_1_SAFE_AUTO
    }
}

enum class TerminalTaskState(val label: String) {
    IDLE("Idle"),
    PLANNING("Planning Task"),
    DISCOVERING_TOOLS("Discovering Tools"),
    WAITING_FOR_APPROVAL("Waiting for User Approval"),
    AWAITING_APPROVAL("Waiting for User Approval"),
    EXECUTING("Executing Command"),
    ANALYZING("Analyzing Terminal Output"),
    RECOVERING("Attempting Error Recovery"),
    VERIFYING("Verifying Result"),
    COMPLETED("Task Completed"),
    FAILED("Task Failed"),
    CANCELLED("Execution Cancelled")
}

data class DiscoveredTool(
    val name: String,
    val binaryName: String,
    val packageName: String,
    val description: String,
    val version: String? = null,
    val isInstalled: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
    val supportedOperations: List<String> = emptyList(),
    val binaryPath: String? = null
) {
    val isAvailable: Boolean get() = isInstalled
    val pkgName: String get() = packageName
}

data class TerminalCommand(
    val rawCommand: String,
    val executable: String,
    val arguments: List<String>,
    val workingDir: String = "/data/data/com.termux/files/home",
    val riskLevel: RiskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
    val explanation: String,
    val isPackageInstall: Boolean = false,
    val targetPackage: String? = null
)

data class ExecutionResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val isSuccess: Boolean,
    val errorDiagnosis: String? = null,
    val suggestedFix: String? = null
)

data class ExecutionPlan(
    val taskTitle: String,
    val naturalLanguageQuery: String,
    val requiredTools: List<DiscoveredTool>,
    val commands: List<TerminalCommand>,
    val estimatedDurationSec: Int,
    val highestRiskLevel: RiskLevel,
    val userIntent: String = taskTitle
)

data class TerminalDashboardState(
    val currentTask: String = "Standing by for natural language terminal instruction...",
    val taskState: TerminalTaskState = TerminalTaskState.IDLE,
    val currentCommand: String = "",
    val activeToolName: String = "",
    val executionLog: List<String> = emptyList(),
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val elapsedMs: Long = 0L,
    val isTermuxInstalled: Boolean = true,
    val isTermuxPermissionGranted: Boolean = true,
    val pendingApprovalCommand: TerminalCommand? = null,
    val verificationReport: String? = null,
    val discoveredTools: List<DiscoveredTool> = emptyList(),
    val activePlan: ExecutionPlan? = null
) {
    val consoleLogs: List<String> get() = executionLog
    val hasRunCommandPermission: Boolean get() = isTermuxPermissionGranted
}
