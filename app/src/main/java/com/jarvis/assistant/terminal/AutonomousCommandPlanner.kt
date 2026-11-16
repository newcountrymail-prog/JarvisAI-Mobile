package com.jarvis.assistant.terminal

class AutonomousCommandPlanner(
    private val toolRegistry: ToolDiscoveryRegistry = ToolDiscoveryRegistry()
) {

    // Dangerous / restricted command signatures for Level 3
    private val restrictedPatterns = listOf(
        "rm -rf /",
        "rm -rf *",
        ":(){ :|:& };:",
        "mkfs",
        "dd if=/dev/zero",
        "> /dev/sda",
        "chmod 777 /",
        "chmod -r 777 /",
        "/data/system/",
        "/data/data/com.android.providers.telephony"
    )

    fun planFromNaturalLanguage(input: String): ExecutionPlan {
        val query = input.trim().lowercase()

        val commands = mutableListOf<TerminalCommand>()
        val requiredTools = mutableListOf<DiscoveredTool>()
        var taskTitle = "Autonomous Terminal Task"
        var estDuration = 5

        when {
            // 1. Python Environment Setup
            query.contains("python") && (query.contains("setup") || query.contains("environment") || query.contains("install")) -> {
                taskTitle = "Python Development Environment Setup"
                estDuration = 25
                val pyTool = toolRegistry.findToolByPackage("python")
                if (pyTool != null) requiredTools.add(pyTool)

                commands.add(
                    TerminalCommand(
                        rawCommand = "pkg update -y",
                        executable = "pkg",
                        arguments = listOf("update", "-y"),
                        riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
                        explanation = "Update Termux repository package mirrors and index.",
                        isPackageInstall = false
                    )
                )
                commands.add(
                    TerminalCommand(
                        rawCommand = "pkg install -y python",
                        executable = "pkg",
                        arguments = listOf("install", "-y", "python"),
                        riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
                        explanation = "Install Python 3 runtime and standard library packages.",
                        isPackageInstall = true,
                        targetPackage = "python"
                    )
                )
                commands.add(
                    TerminalCommand(
                        rawCommand = "python3 --version",
                        executable = "python3",
                        arguments = listOf("--version"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Verify Python installation and report active version."
                    )
                )
            }

            // 2. Git Installation / Setup
            query.contains("git") && (query.contains("install") || query.contains("setup") || query.contains("karna hai")) -> {
                taskTitle = "Git Version Control Installation"
                estDuration = 15
                val gitTool = toolRegistry.findToolByPackage("git")
                if (gitTool != null) requiredTools.add(gitTool)

                commands.add(
                    TerminalCommand(
                        rawCommand = "pkg install -y git",
                        executable = "pkg",
                        arguments = listOf("install", "-y", "git"),
                        riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
                        explanation = "Install Git distributed version control system.",
                        isPackageInstall = true,
                        targetPackage = "git"
                    )
                )
                commands.add(
                    TerminalCommand(
                        rawCommand = "git --version",
                        executable = "git",
                        arguments = listOf("--version"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Verify Git installation version."
                    )
                )
            }

            // 3. Storage and Memory Diagnostics
            query.contains("storage") || query.contains("memory") || query.contains("space") || query.contains("free") -> {
                taskTitle = "Storage & Memory Hardware Diagnostic"
                estDuration = 3
                commands.add(
                    TerminalCommand(
                        rawCommand = "df -h",
                        executable = "df",
                        arguments = listOf("-h"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Inspect filesystem partition usage and available disk storage."
                    )
                )
                commands.add(
                    TerminalCommand(
                        rawCommand = "free -m || cat /proc/meminfo | head -n 5",
                        executable = "sh",
                        arguments = listOf("-c", "cat /proc/meminfo | head -n 5"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Read RAM allocation, active buffers, and cache availability."
                    )
                )
            }

            // 4. Installed Packages & Tools List
            query.contains("installed") || query.contains("package list") || query.contains("packages ki list") || query.contains("tools list") -> {
                taskTitle = "Audit Installed Packages & Binaries"
                estDuration = 4
                commands.add(
                    TerminalCommand(
                        rawCommand = "dpkg -l 2>/dev/null || pkg list-installed 2>/dev/null || which python3 git node curl 2>/dev/null",
                        executable = "pkg",
                        arguments = listOf("list-installed"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Audit package manager database for installed tools and utilities."
                    )
                )
            }

            // 5. Git Status & Project Inspection
            query.contains("git status") || query.contains("project status") || query.contains("dependencies") -> {
                taskTitle = "Project Version Control & Dependency Audit"
                estDuration = 5
                commands.add(
                    TerminalCommand(
                        rawCommand = "pwd && git status --short",
                        executable = "git",
                        arguments = listOf("status", "--short"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Check current working tree and modified project files."
                    )
                )
                commands.add(
                    TerminalCommand(
                        rawCommand = "ls -la",
                        executable = "ls",
                        arguments = listOf("-la"),
                        riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
                        explanation = "Inspect current directory files and permission bits."
                    )
                )
            }

            // 6. Generic or Custom Command Execution
            else -> {
                taskTitle = "Execute Autonomous Command"
                estDuration = 5
                val sanitized = sanitizeCommand(input)
                val risk = classifyRisk(sanitized)
                commands.add(
                    TerminalCommand(
                        rawCommand = sanitized,
                        executable = sanitized.split(" ").firstOrNull() ?: "sh",
                        arguments = sanitized.split(" ").drop(1),
                        riskLevel = risk,
                        explanation = "Execute operator command under autonomous verification policy."
                    )
                )
            }
        }

        val highestRisk = commands.maxByOrNull {
            when (it.riskLevel) {
                RiskLevel.LEVEL_3_RESTRICTED -> 3
                RiskLevel.LEVEL_2_APPROVAL_REQUIRED -> 2
                RiskLevel.LEVEL_1_SAFE_AUTO -> 1
            }
        }?.riskLevel ?: RiskLevel.LEVEL_1_SAFE_AUTO

        return ExecutionPlan(
            taskTitle = taskTitle,
            naturalLanguageQuery = input,
            requiredTools = requiredTools,
            commands = commands,
            estimatedDurationSec = estDuration,
            highestRiskLevel = highestRisk
        )
    }

    fun classifyRisk(command: String): RiskLevel {
        val lower = command.lowercase().trim()

        // 1. Check Restricted Level 3
        for (pattern in restrictedPatterns) {
            if (lower.contains(pattern)) {
                return RiskLevel.LEVEL_3_RESTRICTED
            }
        }

        // 2. Check Level 2 (Modifications, installs, writes, builds)
        val level2Keywords = listOf(
            "install", "pkg in", "pkg up", "apt-get", "npm i", "pip install",
            "git push", "git commit", "mkdir", "touch", "rm ", "cp ", "mv ",
            "chmod", "chown", "gradlew", "make", "cmake", "clang", "gcc",
            ">", ">>", "kill"
        )
        for (kw in level2Keywords) {
            if (lower.contains(kw)) {
                return RiskLevel.LEVEL_2_APPROVAL_REQUIRED
            }
        }

        // 3. Otherwise Level 1 Safe Read-only
        return RiskLevel.LEVEL_1_SAFE_AUTO
    }

    private fun sanitizeCommand(raw: String): String {
        return raw.trim()
            .removePrefix("jarvis,")
            .removePrefix("jarvis")
            .trim()
    }
}
