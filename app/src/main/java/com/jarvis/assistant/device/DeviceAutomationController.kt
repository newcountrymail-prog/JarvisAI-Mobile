package com.jarvis.assistant.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
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

    // Pre-configured known contact aliases as fallback / known mappings
    private val contactAliases = listOf(
        ContactMatch("Sarah Jenkins", "Sarah", "+15550199", isWhatsAppAvailable = true),
        ContactMatch("Amit Sharma", "Amit bhaiya", "+919876543210", isWhatsAppAvailable = true),
        ContactMatch("Dev Team Core", "Dev Team", "+15550142", isWhatsAppAvailable = true),
        ContactMatch("Dr. Marcus Vance", "Doctor", "+15550188", isWhatsAppAvailable = true),
        ContactMatch("Elena Rostova", "Elena", "+15550177", isWhatsAppAvailable = true)
    )

    /**
     * Searches Android Contacts Provider with READ_CONTACTS permission,
     * with fallback to saved alias table.
     */
    fun resolveContact(query: String): ContactMatch? {
        val clean = query.trim().lowercase()

        // 1. Try querying real Android Contacts ContentProvider if permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val phoneContacts = queryAndroidContacts(clean)
            if (phoneContacts.isNotEmpty()) {
                return phoneContacts.first()
            }
        }

        // 2. Fallback to Alias Matcher
        return contactAliases.firstOrNull {
            it.alias.lowercase() == clean ||
            it.name.lowercase().contains(clean) ||
            clean.contains(it.alias.lowercase())
        }
    }

    fun searchAllMatchingContacts(query: String): List<ContactMatch> {
        val list = mutableListOf<ContactMatch>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            list.addAll(queryAndroidContacts(query))
        }
        val clean = query.trim().lowercase()
        val aliasMatches = contactAliases.filter {
            it.alias.lowercase().contains(clean) || it.name.lowercase().contains(clean)
        }
        list.addAll(aliasMatches)
        return list.distinctBy { it.phoneNumber }
    }

    private fun queryAndroidContacts(query: String): List<ContactMatch> {
        val results = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && results.size < 5) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        results.add(ContactMatch(name, name, number, isWhatsAppAvailable = true))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
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
