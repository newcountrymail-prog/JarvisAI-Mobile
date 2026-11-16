package com.jarvis.assistant.security

data class SecurityScorecard(
    val overallScore: Int, // 0 - 100
    val riskLevel: SecurityRiskGrade,
    val checksPassed: Int,
    val checksWarning: Int,
    val checksFailed: Int,
    val summary: String,
    val items: List<SecurityAuditItem>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SecurityRiskGrade(val label: String, val colorHex: Long) {
    EXCELLENT("Secure / Hardened", 0xFF00E676),
    GOOD("Good Security", 0xFF00B0FF),
    MODERATE("Moderate Risk", 0xFFFFD700),
    HIGH_RISK("High Risk Exposure", 0xFFFF3B30),
    CRITICAL("Critical Vulnerability", 0xFFFF0055)
}

data class SecurityAuditItem(
    val title: String,
    val category: String,
    val status: AuditStatus,
    val detail: String,
    val recommendation: String? = null
)

enum class AuditStatus {
    PASSED,
    WARNING,
    FAILED,
    INFO
}

data class PasswordStrengthReport(
    val score: Int, // 0 to 4
    val entropyBits: Double,
    val strengthLabel: String,
    val crackTimeEstimate: String,
    val feedback: List<String>,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigits: Boolean,
    val hasSymbols: Boolean,
    val length: Int
)

data class HashResult(
    val fileName: String,
    val fileSize: Long,
    val sha256: String,
    val sha1: String,
    val md5: String,
    val matchVerified: Boolean? = null
)

data class NetworkDiagnosticReport(
    val localIp: String,
    val isConnected: Boolean,
    val pingMs: Long?,
    val dnsLookupMs: Long?,
    val gateway: String,
    val connectionType: String,
    val dnsServers: List<String>
)

data class DnsInspectionReport(
    val domain: String,
    val resolvedIps: List<String>,
    val canonicalName: String,
    val lookupLatencyMs: Long,
    val isReachable: Boolean
)

data class SslCertificateReport(
    val host: String,
    val subjectDn: String,
    val issuerDn: String,
    val validFrom: String,
    val validUntil: String,
    val isExpired: Boolean,
    val sigAlgName: String,
    val cipherSuite: String,
    val tlsVersion: String
)

data class MetadataReport(
    val fileName: String,
    val mimeType: String,
    val fileSizeFormatted: String,
    val lastModified: String,
    val magicHeaderHex: String,
    val exifTags: Map<String, String> = emptyMap()
)
