package com.example.healthsync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            lifecycleScope.launch {
                if (grants.values.all { it }) {
                    Toast.makeText(this@MainActivity, "Permissions accordées", Toast.LENGTH_SHORT).show()
                    readAndSendData()
                } else {
                    Toast.makeText(this@MainActivity, "Permissions refusées", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availabilityStatus = HealthConnectClient.getSdkStatus(this)

        when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Toast.makeText(
                    this,
                    "Health Connect non installé.\nInstallez-le depuis Play Store",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    // Impossible d'ouvrir Play Store
                }
                return
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Toast.makeText(
                    this,
                    "Mise à jour Health Connect recommandée",
                    Toast.LENGTH_SHORT
                ).show()
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                // Health Connect disponible
            }
        }

        try {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()

                if (!granted.containsAll(permissions)) {
                    requestPermissions.launch(permissions.toTypedArray())
                } else {
                    readAndSendData()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erreur permissions: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readAndSendData() {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                val endTime = Instant.now()
                val startTime = endTime.minusSeconds(30L * 24 * 3600)
                val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)

                // Steps
                val stepsRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRangeFilter)
                )
                val stepsArray = JSONArray()
                var totalSteps = 0L
                for (record in stepsRecords.records) {
                    totalSteps += record.count
                    val obj = JSONObject()
                    obj.put("count", record.count)
                    obj.put("startTime", record.startTime.toString())
                    obj.put("endTime", record.endTime.toString())
                    stepsArray.put(obj)
                }
                json.put("steps", stepsArray)

                // Heart rate
                val hrRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                )
                val hrArray = JSONArray()
                for (record in hrRecords.records) {
                    val obj = JSONObject()
                    obj.put("samples", JSONArray(record.samples.map { it.beatsPerMinute }))
                    obj.put("startTime", record.startTime.toString())
                    obj.put("endTime", record.endTime.toString())
                    hrArray.put(obj)
                }
                json.put("heartRate", hrArray)

                // Distance
                val distRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(DistanceRecord::class, timeRangeFilter)
                )
                val distArray = JSONArray()
                var totalDistance = 0.0
                for (record in distRecords.records) {
                    totalDistance += record.distance.inMeters
                    val obj = JSONObject()
                    obj.put("distanceMeters", record.distance.inMeters)
                    obj.put("startTime", record.startTime.toString())
                    obj.put("endTime", record.endTime.toString())
                    distArray.put(obj)
                }
                json.put("distance", distArray)

                // Sleep
                val sleepRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter)
                )
                val sleepArray = JSONArray()
                for (record in sleepRecords.records) {
                    val obj = JSONObject()
                    obj.put("title", record.title ?: "Sommeil")
                    obj.put("startTime", record.startTime.toString())
                    obj.put("endTime", record.endTime.toString())
                    sleepArray.put(obj)
                }
                json.put("sleep", sleepArray)

                // Exercise
                val exerciseRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter)
                )
                val exerciseArray = JSONArray()
                for (record in exerciseRecords.records) {
                    val obj = JSONObject()
                    obj.put("title", record.title ?: "Exercice")
                    obj.put("exerciseType", record.exerciseType)
                    obj.put("startTime", record.startTime.toString())
                    obj.put("endTime", record.endTime.toString())
                    exerciseArray.put(obj)
                }
                json.put("exercise", exerciseArray)

                val summary = """
                    Données collectées:
                    ${sleepRecords.records.size} sessions sommeil
                    $totalSteps pas
                    ${String.format("%.2f", totalDistance/1000)} km
                    ${hrRecords.records.size} mesures cardio
                    ${exerciseRecords.records.size} exercices
                """.trimIndent()

                Toast.makeText(this@MainActivity, summary, Toast.LENGTH_LONG).show()
                sendToServer(json.toString())

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erreur lecture: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendToServer(jsonData: String) {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    //URL d'adresse IP de mon pc
                    val serverUrl = "http://192.168.1.12:9090/fetch"

                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val requestBody = jsonData.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(serverUrl)
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "Aucune réponse"
                    response.close()

                    Pair(responseCode, responseBody)
                }

                val (responseCode, responseBody) = result

                if (responseCode in 200..299) {
                    Toast.makeText(
                        this@MainActivity,
                        "Données envoyées avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Erreur HTTP: $responseCode",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: java.net.ConnectException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Connexion refusée.\nVérifiez que le serveur est démarré",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: java.net.SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Timeout: le serveur ne répond pas",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: java.net.UnknownHostException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Impossible de trouver le serveur",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erreur: ${e.message ?: "Erreur inconnue"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}