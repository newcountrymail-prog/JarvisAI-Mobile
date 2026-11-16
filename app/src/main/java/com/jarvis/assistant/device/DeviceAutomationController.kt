package com.jarvis.assistant.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.net.URLEncoder

data class ContactMatch(
    val name: String,
    val alias: String,
    val phoneNumber: String,
    val isWhatsAppAvailable: Boolean = true
)

data class DeviceTelemetry(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val isCellularConnected: Boolean,
    val networkName: String
)

class DeviceAutomationController(private val context: Context) {

    // Pre-configured known contact aliases as specified in WhatsApp & Phone specifications
    private val contactAliases = listOf(
        ContactMatch("Sarah Jenkins", "Sarah", "+15550199", isWhatsAppAvailable = true),
        ContactMatch("Dev Team Core", "Dev Team", "+15550142", isWhatsAppAvailable = true),
        ContactMatch("Dr. Marcus Vance", "Doctor", "+15550188", isWhatsAppAvailable = true),
        ContactMatch("Elena Rostova", "Elena", "+15550177", isWhatsAppAvailable = true)
    )

    fun resolveContact(query: String): ContactMatch? {
        val clean = query.trim().lowercase()
        return contactAliases.firstOrNull {
            it.alias.lowercase() == clean || it.name.lowercase().contains(clean)
        }
    }

    fun getAllKnownContacts(): List<ContactMatch> = contactAliases

    // --- WHATSAPP AUTOMATION ---
    fun openWhatsApp(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com/"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
            true
        }
    }

    fun sendWhatsAppMessage(phone: String, message: String): Boolean {
        val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to browser WhatsApp Web
            val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        }
    }

    // --- YOUTUBE AUTOMATION ---
    fun openYouTube(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        }
    }

    fun searchYouTube(query: String): Boolean {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    // --- SPOTIFY & MEDIA AUTOMATION ---
    fun searchSpotify(songOrArtist: String): Boolean {
        val encoded = URLEncoder.encode(songOrArtist, "UTF-8")
        val spotifyUri = Uri.parse("spotify:search:$encoded")
        val intent = Intent(Intent.ACTION_VIEW, spotifyUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            true
        }
    }

    // --- PHONE CALL AUTOMATION ---
    fun placePhoneCall(phoneNumber: String): Boolean {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val cleanPhone = phoneNumber.replace(" ", "")
        val intent = Intent(action, Uri.parse("tel:$cleanPhone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- SYSTEM & DEVICE TELEMETRY ---
    fun getDeviceTelemetry(): DeviceTelemetry {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val capabilities = connManager?.getNetworkCapabilities(activeNetwork)

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netName = if (isWifi) "High-Speed Wi-Fi" else if (isCellular) "Cellular 5G/LTE" else "Offline Protocol"

        return DeviceTelemetry(
            batteryLevel = level,
            isCharging = isCharging,
            isWifiConnected = isWifi,
            isCellularConnected = isCellular,
            networkName = netName
        )
    }

    fun openSystemSettings(): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }

    fun openDisplaySettings(): Boolean {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return true
    }
}
