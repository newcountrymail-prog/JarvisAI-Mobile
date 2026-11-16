package com.jarvis.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.security.AuditStatus
import com.jarvis.assistant.security.DnsInspectionReport
import com.jarvis.assistant.security.NetworkDiagnosticReport
import com.jarvis.assistant.security.PasswordStrengthReport
import com.jarvis.assistant.security.SecurityLab
import com.jarvis.assistant.security.SecurityScorecard
import com.jarvis.assistant.security.SslCertificateReport
import com.jarvis.assistant.ui.theme.MidnightBackground
import com.jarvis.assistant.ui.theme.MidnightBorder
import com.jarvis.assistant.ui.theme.MidnightCardBackground
import com.jarvis.assistant.ui.theme.MidnightElectricBlue
import com.jarvis.assistant.ui.theme.MidnightEmerald
import com.jarvis.assistant.ui.theme.MidnightNeonViolet
import com.jarvis.assistant.ui.theme.MidnightTextPrimary
import com.jarvis.assistant.ui.theme.MidnightTextSecondary
import kotlinx.coroutines.launch

@Composable
fun SecurityLabScreen(
    securityLab: SecurityLab,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var scorecard by remember { mutableStateOf<SecurityScorecard?>(null) }
    var isAuditing by remember { mutableStateOf(false) }

    // Interactive Tool States
    var passwordInput by remember { mutableStateOf("") }
    var passwordReport by remember { mutableStateOf<PasswordStrengthReport?>(null) }

    var domainInput by remember { mutableStateOf("google.com") }
    var dnsReport by remember { mutableStateOf<DnsInspectionReport?>(null) }
    var isInspectingDns by remember { mutableStateOf(false) }

    var networkReport by remember { mutableStateOf<NetworkDiagnosticReport?>(null) }
    var isCheckingNetwork by remember { mutableStateOf(false) }

    var certHostInput by remember { mutableStateOf("github.com") }
    var certReport by remember { mutableStateOf<SslCertificateReport?>(null) }
    var isCheckingCert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scorecard = securityLab.generateSecurityReport()
        networkReport = securityLab.runNetworkDiagnostics()
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 90.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MidnightTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "JARVIS SECURITY LAB",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightElectricBlue
                    )
                    Text(
                        text = "10 Defensive Cybersecurity Tools",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightTextSecondary
                    )
                }

                IconButton(onClick = {
                    scope.launch {
                        isAuditing = true
                        scorecard = securityLab.generateSecurityReport()
                        networkReport = securityLab.runNetworkDiagnostics()
                        isAuditing = false
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MidnightElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // MASTER SECURITY SCORECARD CARD
            scorecard?.let { card ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MidnightCardBackground)
                        .border(1.2.dp, Color(card.riskLevel.colorHex).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SECURITY POSTURE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MidnightTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = card.riskLevel.label,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(card.riskLevel.colorHex)
                                )
                            }

                            // Score Circle
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(card.riskLevel.colorHex).copy(alpha = 0.15f))
                                    .border(2.dp, Color(card.riskLevel.colorHex), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${card.overallScore}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(card.riskLevel.colorHex)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = card.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MidnightTextPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScoreChip("${card.checksPassed} Passed", MidnightEmerald)
                            ScoreChip("${card.checksWarning} Warnings", Color(0xFFFFB74D))
                            ScoreChip("${card.checksFailed} Risks", Color(0xFFFF5252))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. NETWORK DIAGNOSTICS
            SecuritySectionHeader("Network Diagnostics", Icons.Default.NetworkCheck)
            networkReport?.let { net ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MidnightCardBackground)
                        .border(1.dp, MidnightBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Connection Type", color = MidnightTextSecondary, fontSize = 12.sp)
                            Text(net.connectionType, color = MidnightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Local IP Address", color = MidnightTextSecondary, fontSize = 12.sp)
                            Text(net.localIp, color = MidnightElectricBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Gateway / Resolver", color = MidnightTextSecondary, fontSize = 12.sp)
                            Text(net.gateway, color = MidnightTextPrimary, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("TCP Ping Latency (8.8.8.8)", color = MidnightTextSecondary, fontSize = 12.sp)
                            Text(net.pingMs?.let { "${it}ms" } ?: "Timeout", color = MidnightEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("DNS Lookup Latency", color = MidnightTextSecondary, fontSize = 12.sp)
                            Text(net.dnsLookupMs?.let { "${it}ms" } ?: "Failed", color = MidnightElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. LOCAL PASSWORD ENTROPY & STRENGTH EVALUATOR
            SecuritySectionHeader("Password Strength Evaluator (Local/Zero-Cloud)", Icons.Default.VpnKey)
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    passwordReport = securityLab.evaluatePasswordStrength(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type password to calculate Shannon entropy...", color = MidnightTextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightElectricBlue,
                    unfocusedBorderColor = MidnightBorder,
                    focusedContainerColor = MidnightCardBackground,
                    unfocusedContainerColor = MidnightCardBackground,
                    focusedTextColor = MidnightTextPrimary,
                    unfocusedTextColor = MidnightTextPrimary
                )
            )

            passwordReport?.let { pr ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCardBackground)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Rating: ${pr.strengthLabel}", fontWeight = FontWeight.Bold, color = if (pr.score >= 3) MidnightEmerald else Color(0xFFFFB74D))
                            Text(String.format("%.1f Entropy Bits", pr.entropyBits), color = MidnightElectricBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Crack Time: ${pr.crackTimeEstimate}", color = MidnightTextSecondary, fontSize = 12.sp)
                        if (pr.feedback.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            pr.feedback.forEach { fb ->
                                Text("• $fb", color = Color(0xFFFFAB91), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. DNS & DOMAIN INSPECTOR
            SecuritySectionHeader("DNS & Domain Inspector", Icons.Default.Dns)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. google.com", color = MidnightTextSecondary) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightElectricBlue,
                        unfocusedBorderColor = MidnightBorder,
                        focusedContainerColor = MidnightCardBackground,
                        unfocusedContainerColor = MidnightCardBackground,
                        focusedTextColor = MidnightTextPrimary,
                        unfocusedTextColor = MidnightTextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isInspectingDns = true
                            dnsReport = securityLab.inspectDns(domainInput)
                            isInspectingDns = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightElectricBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isInspectingDns) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("Lookup", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            dnsReport?.let { dns ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCardBackground)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resolved IPs: ${dns.resolvedIps.joinToString(", ")}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MidnightTextPrimary)
                        Text("Canonical Host: ${dns.canonicalName}", fontSize = 11.sp, color = MidnightTextSecondary)
                        Text("Lookup Latency: ${dns.lookupLatencyMs}ms • Port 443 Reachable: ${dns.isReachable}", fontSize = 11.sp, color = MidnightEmerald)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. HTTPS CERTIFICATE VIEWER
            SecuritySectionHeader("TLS / HTTPS Certificate Viewer", Icons.Default.Shield)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = certHostInput,
                    onValueChange = { certHostInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. github.com", color = MidnightTextSecondary) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightElectricBlue,
                        unfocusedBorderColor = MidnightBorder,
                        focusedContainerColor = MidnightCardBackground,
                        unfocusedContainerColor = MidnightCardBackground,
                        focusedTextColor = MidnightTextPrimary,
                        unfocusedTextColor = MidnightTextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isCheckingCert = true
                            val res = securityLab.inspectTlsCertificate(certHostInput)
                            certReport = res.getOrNull()
                            isCheckingCert = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightNeonViolet),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isCheckingCert) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Verify", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            certReport?.let { cert ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCardBackground)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Issuer: ${cert.issuerDn}", fontSize = 11.sp, color = MidnightTextPrimary)
                        Text("Validity: ${cert.validFrom} to ${cert.validUntil}", fontSize = 11.sp, color = if (cert.isExpired) Color(0xFFFF5252) else MidnightEmerald)
                        Text("Protocol: ${cert.tlsVersion} • Cipher: ${cert.cipherSuite}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MidnightElectricBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. AUDIT DETAILS LIST
            SecuritySectionHeader("System Security Parameters", Icons.Default.Security)
            scorecard?.items?.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MidnightCardBackground)
                        .border(1.dp, MidnightBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        val iconColor = when (item.status) {
                            AuditStatus.PASSED -> MidnightEmerald
                            AuditStatus.WARNING -> Color(0xFFFFB74D)
                            AuditStatus.FAILED -> Color(0xFFFF5252)
                            AuditStatus.INFO -> MidnightElectricBlue
                        }
                        Icon(
                            imageVector = when (item.status) {
                                AuditStatus.PASSED -> Icons.Default.CheckCircle
                                AuditStatus.WARNING -> Icons.Default.Warning
                                AuditStatus.FAILED -> Icons.Default.Warning
                                AuditStatus.INFO -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(item.title, fontWeight = FontWeight.Bold, color = MidnightTextPrimary, fontSize = 13.sp)
                                Text(item.category, color = MidnightTextSecondary, fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.detail, color = MidnightTextSecondary, fontSize = 12.sp)
                            item.recommendation?.let { rec ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tip: $rec", color = MidnightElectricBlue, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecuritySectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MidnightElectricBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MidnightTextPrimary
        )
    }
}
