package com.jarvis.assistant.terminal

class ToolDiscoveryRegistry {

    private val toolCatalogue = listOf(
        DiscoveredTool(
            name = "Python 3",
            binaryName = "python3",
            packageName = "python",
            description = "Interpreted high-level programming language for scripting and AI development.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("python3 script.py", "python3 -m pip list", "python3 -c ...")
        ),
        DiscoveredTool(
            name = "Git Version Control",
            binaryName = "git",
            packageName = "git",
            description = "Fast, distributed version control system for tracking project changes.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("git status", "git log", "git diff", "git branch", "git commit")
        ),
        DiscoveredTool(
            name = "Node.js JavaScript Runtime",
            binaryName = "node",
            packageName = "nodejs",
            description = "V8 JavaScript runtime engine for building network and frontend tooling.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("node app.js", "npm list", "npm test")
        ),
        DiscoveredTool(
            name = "OpenSSH Client",
            binaryName = "ssh",
            packageName = "openssh",
            description = "Secure network protocol for encrypted remote machine administration.",
            riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
            supportedOperations = listOf("ssh user@host", "scp file user@host:", "ssh-keygen")
        ),
        DiscoveredTool(
            name = "cURL Network Client",
            binaryName = "curl",
            packageName = "curl",
            description = "Command-line tool for transferring data with URLs using HTTP/HTTPS.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("curl -I URL", "curl -s URL | jq", "curl -O URL")
        ),
        DiscoveredTool(
            name = "Wget Retriever",
            binaryName = "wget",
            packageName = "wget",
            description = "Utility for non-interactive download of files from the web.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("wget URL", "wget -c URL")
        ),
        DiscoveredTool(
            name = "jq JSON Processor",
            binaryName = "jq",
            packageName = "jq",
            description = "Lightweight and flexible command-line JSON processor and filter.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("jq '.' file.json", "jq '.data' ...")
        ),
        DiscoveredTool(
            name = "Zip Compression",
            binaryName = "zip",
            packageName = "zip",
            description = "Package and compress files into standard PKZIP archives.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("zip -r archive.zip folder", "unzip -l archive.zip")
        ),
        DiscoveredTool(
            name = "Unzip Decompressor",
            binaryName = "unzip",
            packageName = "unzip",
            description = "Extract compressed files in a ZIP archive.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("unzip file.zip", "unzip -t file.zip")
        ),
        DiscoveredTool(
            name = "SQLite 3 Engine",
            binaryName = "sqlite3",
            packageName = "sqlite",
            description = "Self-contained, serverless, zero-configuration relational SQL database.",
            riskLevel = RiskLevel.LEVEL_1_SAFE_AUTO,
            supportedOperations = listOf("sqlite3 db.sqlite \".tables\"", "sqlite3 db.sqlite \".schema\"")
        ),
        DiscoveredTool(
            name = "Clang / LLVM Compiler",
            binaryName = "clang",
            packageName = "clang",
            description = "C, C++, and Objective-C compiler with LLVM backend optimization.",
            riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
            supportedOperations = listOf("clang -O2 file.c -o file", "clang++ main.cpp")
        ),
        DiscoveredTool(
            name = "CMake Build System",
            binaryName = "cmake",
            packageName = "cmake",
            description = "Cross-platform build system generator managing compilation processes.",
            riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
            supportedOperations = listOf("cmake -B build", "cmake --build build")
        ),
        DiscoveredTool(
            name = "OpenJDK 17",
            binaryName = "javac",
            packageName = "openjdk-17",
            description = "Java Development Kit compiler and runtime environment.",
            riskLevel = RiskLevel.LEVEL_2_APPROVAL_REQUIRED,
            supportedOperations = listOf("javac Main.java", "java Main", "jar -tf file.jar")
        )
    )

    fun getAllTools(): List<DiscoveredTool> = toolCatalogue

    fun findToolByPackage(pkg: String): DiscoveredTool? {
        return toolCatalogue.firstOrNull { it.packageName.equals(pkg, ignoreCase = true) }
    }

    fun findToolByBinary(binary: String): DiscoveredTool? {
        return toolCatalogue.firstOrNull {
            it.binaryName.equals(binary, ignoreCase = true) ||
            binary.startsWith(it.binaryName)
        }
    }

    suspend fun auditInstalledTools(bridge: TermuxBridge): List<DiscoveredTool> {
        return toolCatalogue.map { tool ->
            val checkRes = bridge.executeShellCommand("which ${tool.binaryName}", timeoutSec = 5)
            val isInstalled = checkRes.isSuccess && checkRes.stdout.isNotBlank()
            var ver: String? = null
            if (isInstalled) {
                val verRes = bridge.executeShellCommand("${tool.binaryName} --version", timeoutSec = 5)
                if (verRes.isSuccess) {
                    ver = verRes.stdout.lines().firstOrNull()?.take(40)
                }
            }
            tool.copy(isInstalled = isInstalled, version = ver)
        }
    }
}
