package com.example.healthsync

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermissions: ActivityResultLauncher<Set<String>>

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

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

        // ✅ Contract pour Health Connect avec types explicites
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { grantedPermissions: Set<String> ->
            lifecycleScope.launch {
                if (grantedPermissions.containsAll(permissions)) {
                    Toast.makeText(this@MainActivity, "✅ Toutes les permissions accordées", Toast.LENGTH_SHORT).show()
                    readAndSendData()
                } else {
                    val missingPermissions = permissions - grantedPermissions
                    val missingCount = missingPermissions.size
                    val missingList = missingPermissions.take(3).joinToString("\n") { permission ->
                        permission.substringAfterLast(".")
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "❌ $missingCount permission(s) refusée(s)\n$missingList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        lifecycleScope.launch {
            try {
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

                if (!grantedPermissions.containsAll(permissions)) {
                    requestPermissions.launch(permissions)
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
                val startTime = endTime.minusSeconds( 24 * 3600) // 1 jour
                val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)

                Toast.makeText(this@MainActivity, "🔄 Lecture des données Health Connect...", Toast.LENGTH_SHORT).show()

                // 1️⃣ Steps
                val stepsRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(StepsRecord::class, timeRangeFilter)
                )
                val stepsArray = JSONArray()
                var totalSteps = 0L
                for (record in stepsRecords.records) {
                    totalSteps += record.count
                    val obj = JSONObject()
                    obj.put("count", record.count)
                    obj.put("startTime", dateFormatter.format(record.startTime))
                    obj.put("endTime", dateFormatter.format(record.endTime))
                    stepsArray.put(obj)
                }
                json.put("steps", stepsArray)

                // 2️⃣ Heart Rate
                val hrRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                )
                val hrArray = JSONArray()
                for (record in hrRecords.records) {
                    val obj = JSONObject()
                    obj.put("samples", JSONArray(record.samples.map { it.beatsPerMinute }))
                    obj.put("startTime", dateFormatter.format(record.startTime))
                    obj.put("endTime", dateFormatter.format(record.endTime))
                    hrArray.put(obj)
                }
                json.put("heartRate", hrArray)

                // 3️⃣ Distance
                val distRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(DistanceRecord::class, timeRangeFilter)
                )
                val distArray = JSONArray()
                var totalDistance = 0.0
                for (record in distRecords.records) {
                    totalDistance += record.distance.inMeters
                    val obj = JSONObject()
                    obj.put("distanceMeters", record.distance.inMeters)
                    obj.put("startTime", dateFormatter.format(record.startTime))
                    obj.put("endTime", dateFormatter.format(record.endTime))
                    distArray.put(obj)
                }
                json.put("distance", distArray)

                // 4️⃣ Sleep
                val sleepRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter)
                )
                val sleepArray = JSONArray()
                for (record in sleepRecords.records) {
                    val obj = JSONObject()
                    obj.put("title", record.title ?: "Sommeil")
                    obj.put("startTime", dateFormatter.format(record.startTime))
                    obj.put("endTime", dateFormatter.format(record.endTime))
                    sleepArray.put(obj)
                }
                json.put("sleep", sleepArray)

                // 5️⃣ Exercise
                val exerciseRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter)
                )
                val exerciseArray = JSONArray()
                for (record in exerciseRecords.records) {
                    val obj = JSONObject()
                    obj.put("title", record.title ?: "Exercice")
                    obj.put("exerciseType", record.exerciseType)
                    obj.put("startTime", dateFormatter.format(record.startTime))
                    obj.put("endTime", dateFormatter.format(record.endTime))
                    exerciseArray.put(obj)
                }
                json.put("exercise", exerciseArray)

                // 6️⃣ Oxygen Saturation 🫁
                val oxygenRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(OxygenSaturationRecord::class, timeRangeFilter)
                )
                val oxygenArray = JSONArray()
                for (record in oxygenRecords.records) {
                    val obj = JSONObject()
                    obj.put("percentage", record.percentage.value)
                    obj.put("time", dateFormatter.format(record.time))
                    oxygenArray.put(obj)
                }
                json.put("oxygenSaturation", oxygenArray)

                // 7️⃣ Body Temperature 🌡️
                val tempRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(BodyTemperatureRecord::class, timeRangeFilter)
                )
                val tempArray = JSONArray()
                for (record in tempRecords.records) {
                    val obj = JSONObject()
                    obj.put("temperature", record.temperature.inCelsius)
                    obj.put("time", dateFormatter.format(record.time))
                    tempArray.put(obj)
                }
                json.put("bodyTemperature", tempArray)

                // 8️⃣ Blood Pressure 💉
                val bpRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(BloodPressureRecord::class, timeRangeFilter)
                )
                val bpArray = JSONArray()
                for (record in bpRecords.records) {
                    val obj = JSONObject()
                    obj.put("systolic", record.systolic.inMillimetersOfMercury)
                    obj.put("diastolic", record.diastolic.inMillimetersOfMercury)
                    obj.put("time", dateFormatter.format(record.time))
                    bpArray.put(obj)
                }
                json.put("bloodPressure", bpArray)

                // 9️⃣ Weight ⚖️
                val weightRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(WeightRecord::class, timeRangeFilter)
                )
                val weightArray = JSONArray()
                for (record in weightRecords.records) {
                    val obj = JSONObject()
                    obj.put("weight", record.weight.inKilograms)
                    obj.put("time", dateFormatter.format(record.time))
                    weightArray.put(obj)
                }
                json.put("weight", weightArray)

                // 🔟 Height 📏
                val heightRecords = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeightRecord::class, timeRangeFilter)
                )
                val heightArray = JSONArray()
                for (record in heightRecords.records) {
                    val obj = JSONObject()
                    obj.put("height", record.height.inMeters)
                    obj.put("time", dateFormatter.format(record.time))
                    heightArray.put(obj)
                }
                json.put("height", heightArray)

                // Résumé
                val summary = """
                    📊 Données collectées:
                    🏋️ ${exerciseRecords.records.size} exercices
                     💤 ${sleepRecords.records.size} sessions sommeil
                    👣 ${stepsRecords.records.size} pas (Total: $totalSteps)
                    ❤️ ${hrRecords.records.size} mesures cardio
                    📏 ${distRecords.records.size} distances (${String.format("%.2f", totalDistance/1000)} km)
                    🫁 ${oxygenRecords.records.size} saturations O2
                    🌡️ ${tempRecords.records.size} températures
                    💉 ${bpRecords.records.size} pressions artérielles
                    ⚖️ ${weightRecords.records.size} poids
                    📏 ${heightRecords.records.size} tailles
                """.trimIndent()

                Toast.makeText(this@MainActivity, summary, Toast.LENGTH_LONG).show()
                sendToServer(json.toString())

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "❌ Erreur lecture: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }


    private fun sendToServer(jsonData: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "🔄 Connexion au serveur...", Toast.LENGTH_SHORT).show()

                val result = withContext(Dispatchers.IO) {
                    val serverUrl = "https://pleuropneumonic-ferromagnetic-conrad.ngrok-free.dev/fetch"

                    // Affichage via Toast sur le thread principal
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "📡 Tentative de connexion à: $serverUrl", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@MainActivity, "📦 Taille des données: ${jsonData.length} caractères", Toast.LENGTH_SHORT).show()
                    }

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val request = chain.request()

                            // Affiche le toast depuis le thread principal
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(this@MainActivity, "🚀 Envoi de la requête...", Toast.LENGTH_SHORT).show()
                            }

                            val response = chain.proceed(request)

                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(this@MainActivity, "✅ Réponse reçue: ${response.code}", Toast.LENGTH_SHORT).show()
                            }

                            response
                        }

                        .build()

                    val requestBody = jsonData.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(serverUrl)
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "⏳ Attente de la réponse...", Toast.LENGTH_SHORT).show()
                    }

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "Aucune réponse"
                    response.close()

                    Pair(responseCode, responseBody)
                }

                val (responseCode, responseBody) = result

                if (responseCode in 200..299) {
                    Toast.makeText(this@MainActivity, "✅ Succès HTTP $responseCode : $responseBody", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "❌ Erreur HTTP $responseCode : $responseBody", Toast.LENGTH_LONG).show()
                }

            } catch (e: java.net.ConnectException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Connexion refusée.\nVérifiez que le serveur est sur 192.168.1.12:9090",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: java.net.SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "⏱️ Timeout après 30s.\nLe serveur met trop de temps à répondre",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: java.net.UnknownHostException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "🌐 Impossible de trouver 192.168.1.12\nVérifiez votre WiFi",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Erreur: ${e.javaClass.simpleName}\n${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}