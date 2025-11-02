package com.health.virtualdoctor.ui.user;

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.health.virtualdoctor.R
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.health.virtualdoctor.ui.auth.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class UserMetricsActivity : ComponentActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var requestPermissions: ActivityResultLauncher<Set<String>>

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    // Views
    private lateinit var tvStatus: android.widget.TextView
    private lateinit var tvSteps: android.widget.TextView
    private lateinit var tvDistance: android.widget.TextView
    private lateinit var tvHeartRate: android.widget.TextView
    private lateinit var tvSleep: android.widget.TextView
    private lateinit var tvHydration: android.widget.TextView
    private lateinit var tvStress: android.widget.TextView
    private lateinit var tvExerciseCount: android.widget.TextView
    private lateinit var exerciseContainer: android.widget.LinearLayout
    private lateinit var tvSpO2: android.widget.TextView
    private lateinit var tvTemperature: android.widget.TextView
    private lateinit var tvBloodPressure: android.widget.TextView
    private lateinit var tvWeight: android.widget.TextView
    private lateinit var tvJsonData: android.widget.TextView
    private lateinit var btnRefresh: android.widget.Button

    private val permissions = setOf(
        // Permissions de base
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        // Permissions supplémentaires pour détails exercice
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(StepsCadenceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(PowerRecord::class),
        // ✅ NOUVELLE: Hydratation
        HealthPermission.getReadPermission(HydrationRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_metrics)

        // Initialiser les vues
        tvStatus = findViewById(R.id.tvStatus)
        tvSteps = findViewById(R.id.tvSteps)
        tvDistance = findViewById(R.id.tvDistance)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvSleep = findViewById(R.id.tvSleep)
        tvHydration = findViewById(R.id.tvHydration)
        tvStress = findViewById(R.id.tvStress)
        tvExerciseCount = findViewById(R.id.tvExerciseCount)
        exerciseContainer = findViewById(R.id.exerciseContainer)
        tvSpO2 = findViewById(R.id.tvSpO2)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvBloodPressure = findViewById(R.id.tvBloodPressure)
        tvWeight = findViewById(R.id.tvWeight)
        tvJsonData = findViewById(R.id.tvJsonData)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener {
            checkPermissions()
        }

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

        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { grantedPermissions: Set<String> ->
            lifecycleScope.launch {
                if (grantedPermissions.containsAll(permissions)) {
                    Toast.makeText(this@UserMetricsActivity, "✅ Toutes les permissions accordées", Toast.LENGTH_SHORT).show()
                    readAndSendData()
                } else {
                    val missingPermissions = permissions - grantedPermissions
                    val missingCount = missingPermissions.size

                    // Message plus clair
                    val message = """
                        ⚠️ $missingCount permissions manquantes

                        Dans Health Connect :
                        1. Ouvrez "Autorisations d'application"
                        2. Trouvez "HealthSync"
                        3. Activez TOUTES les permissions
                        4. Relancez l'application
                    """.trimIndent()

                    Toast.makeText(
                        this@UserMetricsActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    // Log des permissions manquantes
                    Log.w("HealthSync", "Permissions manquantes ($missingCount):")
                    missingPermissions.forEach { permission ->
                        Log.w("HealthSync", "  - ${permission.substringAfterLast(".")}")
                    }

                    // Rediriger vers Health Connect après 3 secondes
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val intent = android.content.Intent("androidx.health.ACTION_MANAGE_PERMISSIONS")
                            intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@UserMetricsActivity,
                                "Ouvrez manuellement Health Connect > Autorisations",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }, 3000)
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        lifecycleScope.launch {
            try {
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

                // Debug : Afficher les permissions accordées
                Log.d("HealthSync", "Permissions accordées (${grantedPermissions.size}):")
                grantedPermissions.forEach { permission ->
                    Log.d("HealthSync", "  ✅ ${permission.substringAfterLast(".")}")
                }

                if (!grantedPermissions.containsAll(permissions)) {
                    val missingPermissions = permissions - grantedPermissions
                    Log.w("HealthSync", "Permissions manquantes (${missingPermissions.size}):")
                    missingPermissions.forEach { permission ->
                        Log.w("HealthSync", "  ❌ ${permission.substringAfterLast(".")}")
                    }

                    requestPermissions.launch(permissions)
                } else {
                    readAndSendData()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserMetricsActivity, "Erreur permissions: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getExerciseTypeName(type: Int): String {
        return when (type) {
            0 -> "Autre"
            1 -> "Badminton"
            2 -> "Baseball"
            3 -> "Basketball"
            4 -> "Biathlon"
            5 -> "Handisport"
            6 -> "Bowling"
            7 -> "Boxe"
            8 -> "Canoë"
            9 -> "Cricket"
            10 -> "Curling"
            11 -> "Vélo elliptique"
            12 -> "Vélo"
            13 -> "Danse"
            14 -> "Plongée"
            15 -> "Aviron"
            16 -> "Fencing"
            17 -> "Football américain"
            18 -> "Frisbee"
            19 -> "Golf"
            20 -> "Gymnastique"
            21 -> "Handball"
            22 -> "HIIT"
            23 -> "Randonnée"
            24 -> "Hockey"
            25 -> "Patinage"
            26 -> "Arts martiaux"
            27 -> "Paddling"
            28 -> "Para gliding"
            29 -> "Pilates"
            30 -> "Polo"
            31 -> "Racquetball"
            32 -> "Rock climbing"
            33 -> "Roller skating"
            34 -> "Aviron"
            35 -> "Rugby"
            36 -> "Course à pied"
            37 -> "Voile"
            38 -> "Scuba diving"
            39 -> "Skateboarding"
            40 -> "Ski"
            41 -> "Snowboarding"
            42 -> "Snowshoeing"
            43 -> "Softball"
            44 -> "Squash"
            45 -> "Stepper"
            46 -> "Surfing"
            47 -> "Natation en piscine"
            48 -> "Natation en eau libre"
            49 -> "Tennis de table"
            50 -> "Tennis"
            51 -> "Treadmill"
            52 -> "Volleyball"
            53 -> "Marche"
            54 -> "Water polo"
            55 -> "Haltérophilie"
            56 -> "Entraînement"
            57 -> "Yoga"
            58 -> "Zumba"
            59 -> "Plongée avec tuba"
            60 -> "Alpinisme"
            61 -> "Hang gliding"
            62 -> "Bowling"
            63 -> "Marche nordique"
            64 -> "Course d'orientation"
            65 -> "Pêche"
            66 -> "Roller"
            67 -> "Escalade"
            68 -> "Equitation"
            69 -> "Stair climbing"
            70 -> "Burpee"
            71 -> "Sit-up"
            72 -> "Saut à la corde"
            73 -> "Rameur"
            74 -> "Pompes"
            75 -> "Course sur place"
            76 -> "Plank"
            77 -> "Abdos"
            78 -> "Jumping jack"
            79 -> "Marche"
            80 -> "Stretching"
            81 -> "Renforcement musculaire"
            82 -> "Cross training"
            83 -> "Aérobic"
            84 -> "Course en salle"
            85 -> "Vélo d'appartement"
            86 -> "Vélo en salle"
            87 -> "Marche en salle"
            88 -> "Exercice libre"
            89 -> "Entraînement mixte"
            90 -> "Entraînement général"
            else -> "Activité ($type)"
        }
    }

    private fun readAndSendData() {
        lifecycleScope.launch {
            try {
                val json = JSONObject()
                val today = LocalDate.now()
                val daysData = JSONArray()

                for (daysAgo in 0..6) {
                    val targetDate = today.minusDays(daysAgo.toLong())
                    val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val endOfDay = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)

                    val dayJson = JSONObject()
                    dayJson.put("date", targetDate.toString())

                    Toast.makeText(this@UserMetricsActivity, "🔄 Lecture du $targetDate...", Toast.LENGTH_SHORT).show()

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
                    dayJson.put("steps", stepsArray)
                    dayJson.put("totalSteps", totalSteps)

                    // 2️⃣ Heart Rate
                    val hrRecords = healthConnectClient.readRecords(
                        ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter)
                    )
                    val hrArray = JSONArray()
                    val allHeartRates = mutableListOf<Long>()
                    for (record in hrRecords.records) {
                        val samples = record.samples.map { it.beatsPerMinute }
                        allHeartRates.addAll(samples)
                        val obj = JSONObject()
                        obj.put("samples", JSONArray(samples))
                        obj.put("startTime", dateFormatter.format(record.startTime))
                        obj.put("endTime", dateFormatter.format(record.endTime))
                        hrArray.put(obj)
                    }
                    dayJson.put("heartRate", hrArray)

                    if (allHeartRates.isNotEmpty()) {
                        dayJson.put("minHeartRate", allHeartRates.minOrNull())
                        dayJson.put("maxHeartRate", allHeartRates.maxOrNull())
                        dayJson.put("avgHeartRate", allHeartRates.average().roundToInt())
                    }

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
                    dayJson.put("distance", distArray)
                    dayJson.put("totalDistanceKm", String.format("%.2f", totalDistance / 1000))

                    // 4️⃣ Sleep
                    val sleepRecords = healthConnectClient.readRecords(
                        ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter)
                    )
                    val sleepArray = JSONArray()
                    var totalSleepMinutes = 0L
                    for (record in sleepRecords.records) {
                        val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                        totalSleepMinutes += durationMinutes
                        val obj = JSONObject()
                        obj.put("title", record.title ?: "Sommeil")
                        obj.put("startTime", dateFormatter.format(record.startTime))
                        obj.put("endTime", dateFormatter.format(record.endTime))
                        obj.put("durationMinutes", durationMinutes)
                        sleepArray.put(obj)
                    }
                    dayJson.put("sleep", sleepArray)
                    dayJson.put("totalSleepHours", String.format("%.1f", totalSleepMinutes / 60.0))

                    // 5️⃣ Exercise (COMPLET AVEC TOUTES LES MÉTRIQUES) 🏋️‍♂️
                    val exerciseRecords = healthConnectClient.readRecords(
                        ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter)
                    )
                    val exerciseArray = JSONArray()

                    for (record in exerciseRecords.records) {
                        val obj = JSONObject()
                        val exerciseTimeRange = TimeRangeFilter.between(record.startTime, record.endTime)

                        // Infos de base
                        obj.put("title", record.title ?: "Exercice")
                        obj.put("exerciseType", record.exerciseType)
                        obj.put("exerciseTypeName", getExerciseTypeName(record.exerciseType))
                        obj.put("startTime", dateFormatter.format(record.startTime))
                        obj.put("endTime", dateFormatter.format(record.endTime))

                        val durationMinutes = java.time.Duration.between(record.startTime, record.endTime).toMinutes()
                        obj.put("durationMinutes", durationMinutes)

                        // ✅ 1. Pas pendant l'exercice
                        try {
                            val exerciseSteps = healthConnectClient.readRecords(
                                ReadRecordsRequest(StepsRecord::class, exerciseTimeRange)
                            )
                            val exerciseTotalSteps = exerciseSteps.records.sumOf { it.count }
                            obj.put("steps", exerciseTotalSteps)
                        } catch (e: Exception) {
                            obj.put("steps", 0)
                        }

                        // ✅ 2. Distance pendant l'exercice
                        try {
                            val exerciseDistance = healthConnectClient.readRecords(
                                ReadRecordsRequest(DistanceRecord::class, exerciseTimeRange)
                            )
                            val totalDistanceMeters = exerciseDistance.records.sumOf { it.distance.inMeters }
                            obj.put("distanceMeters", totalDistanceMeters)
                            obj.put("distanceKm", String.format("%.2f", totalDistanceMeters / 1000))
                        } catch (e: Exception) {
                            obj.put("distanceMeters", 0.0)
                            obj.put("distanceKm", "0.00")
                        }

                        // ✅ 3. Calories ACTIVES brûlées
                        try {
                            val activeCalories = healthConnectClient.readRecords(
                                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, exerciseTimeRange)
                            )
                            val totalActiveCalories = activeCalories.records.sumOf { it.energy.inKilocalories.toInt() }
                            obj.put("activeCalories", totalActiveCalories)
                        } catch (e: Exception) {
                            obj.put("activeCalories", 0)
                        }

                        // ✅ 4. Calories TOTALES brûlées
                        try {
                            val totalCalories = healthConnectClient.readRecords(
                                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, exerciseTimeRange)
                            )
                            val totalCaloriesBurned = totalCalories.records.sumOf { it.energy.inKilocalories.toInt() }
                            obj.put("totalCalories", totalCaloriesBurned)
                        } catch (e: Exception) {
                            obj.put("totalCalories", 0)
                        }

                        // ✅ 5. BPM moyen pendant l'exercice
                        try {
                            val exerciseHR = healthConnectClient.readRecords(
                                ReadRecordsRequest(HeartRateRecord::class, exerciseTimeRange)
                            )
                            val allBPM = exerciseHR.records.flatMap { it.samples.map { s -> s.beatsPerMinute } }
                            if (allBPM.isNotEmpty()) {
                                obj.put("avgHeartRate", allBPM.average().roundToInt())
                                obj.put("minHeartRate", allBPM.minOrNull())
                                obj.put("maxHeartRate", allBPM.maxOrNull())
                            }
                        } catch (e: Exception) {
                            obj.put("avgHeartRate", 0)
                        }

                        // ✅ 6. Cadence (pas/minute) - MOYENNE, MIN, MAX
                        try {
                            val cadenceRecords = healthConnectClient.readRecords(
                                ReadRecordsRequest(StepsCadenceRecord::class, exerciseTimeRange)
                            )
                            val cadences = cadenceRecords.records.flatMap { it.samples.map { s -> s.rate } }
                            if (cadences.isNotEmpty()) {
                                obj.put("avgCadence", cadences.average().roundToInt())
                                obj.put("minCadence", cadences.minOrNull()?.roundToInt() ?: 0)
                                obj.put("maxCadence", cadences.maxOrNull()?.roundToInt() ?: 0)
                            }
                        } catch (e: Exception) {
                            obj.put("avgCadence", 0)
                            obj.put("minCadence", 0)
                            obj.put("maxCadence", 0)
                        }

                        // ✅ 7. Vitesse moyenne/max ET calcul longueur de foulée min/max/moyenne
                        try {
                            val speedRecords = healthConnectClient.readRecords(
                                ReadRecordsRequest(SpeedRecord::class, exerciseTimeRange)
                            )
                            val speeds = speedRecords.records.flatMap { it.samples.map { s -> s.speed.inMetersPerSecond } }
                            if (speeds.isNotEmpty()) {
                                val avgSpeedMs = speeds.average()
                                val maxSpeedMs = speeds.maxOrNull()!!
                                val minSpeedMs = speeds.minOrNull()!!

                                obj.put("avgSpeedKmh", String.format("%.2f", avgSpeedMs * 3.6))
                                obj.put("maxSpeedKmh", String.format("%.2f", maxSpeedMs * 3.6))
                                obj.put("minSpeedKmh", String.format("%.2f", minSpeedMs * 3.6))

                                // ✅ Calcul longueur de foulée MOYENNE/MIN/MAX (si cadence disponible)
                                val avgCadence = obj.optInt("avgCadence", 0)
                                val minCadence = obj.optInt("minCadence", 0)
                                val maxCadence = obj.optInt("maxCadence", 0)

                                if (avgCadence > 0) {
                                    val avgStride = (avgSpeedMs * 60) / avgCadence
                                    obj.put("avgStrideLengthMeters", String.format("%.2f", avgStride))
                                }
                                if (maxCadence > 0 && minSpeedMs > 0) {
                                    val minStride = (minSpeedMs * 60) / maxCadence // foulée min = vitesse min / cadence max
                                    obj.put("minStrideLengthMeters", String.format("%.2f", minStride))
                                }
                                if (minCadence > 0 && maxSpeedMs > 0) {
                                    val maxStride = (maxSpeedMs * 60) / minCadence // foulée max = vitesse max / cadence min
                                    obj.put("maxStrideLengthMeters", String.format("%.2f", maxStride))
                                }
                            }
                        } catch (e: Exception) {
                            // Pas de vitesse disponible
                        }

                        // ✅ 8. Puissance (pour cyclisme)
                        try {
                            val powerRecords = healthConnectClient.readRecords(
                                ReadRecordsRequest(PowerRecord::class, exerciseTimeRange)
                            )
                            val avgPower = powerRecords.records
                                .flatMap { it.samples.map { s -> s.power.inWatts } }
                                .average()
                            if (!avgPower.isNaN()) {
                                obj.put("avgPowerWatts", avgPower.roundToInt())
                            }
                        } catch (e: Exception) {
                            // Pas de puissance disponible
                        }

                        exerciseArray.put(obj)
                    }
                    dayJson.put("exercise", exerciseArray)

                    // 6️⃣ Oxygen Saturation
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
                    dayJson.put("oxygenSaturation", oxygenArray)

                    // 7️⃣ Body Temperature
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
                    dayJson.put("bodyTemperature", tempArray)

                    // 8️⃣ Blood Pressure
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
                    dayJson.put("bloodPressure", bpArray)

                    // 9️⃣ Weight
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
                    dayJson.put("weight", weightArray)

                    // 🔟 Height
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
                    dayJson.put("height", heightArray)

                    // ✅ NOUVELLE: 1️⃣1️⃣ Hydratation 💧
                    try {
                        val hydrationRecords = healthConnectClient.readRecords(
                            ReadRecordsRequest(HydrationRecord::class, timeRangeFilter)
                        )
                        val hydrationArray = JSONArray()
                        var totalHydrationMl = 0.0
                        for (record in hydrationRecords.records) {
                            totalHydrationMl += record.volume.inMilliliters
                            val obj = JSONObject()
                            obj.put("volumeMl", record.volume.inMilliliters)
                            obj.put("time", dateFormatter.format(record.startTime))
                            hydrationArray.put(obj)
                        }
                        dayJson.put("hydration", hydrationArray)
                        dayJson.put("totalHydrationLiters", String.format("%.2f", totalHydrationMl / 1000))
                    } catch (e: Exception) {
                        dayJson.put("hydration", JSONArray())
                        dayJson.put("totalHydrationLiters", "0.00")
                    }

                    // 🧠 Calcul du niveau de stress
                    val stressLevel = calculateStressLevel(
                        avgHeartRate = dayJson.optInt("avgHeartRate", 70),
                        sleepHours = totalSleepMinutes / 60.0,
                        steps = totalSteps.toInt()
                    )
                    dayJson.put("stressLevel", stressLevel)
                    dayJson.put("stressScore", calculateStressScore(stressLevel))

                    daysData.put(dayJson)
                }

                json.put("dailyData", daysData)

                // Mettre à jour l'UI avec les données du jour
                updateUI(daysData.getJSONObject(0))

                Toast.makeText(this@UserMetricsActivity, "✅ Données de 7 jours collectées!", Toast.LENGTH_LONG).show()

                Log.d("HealthSync", "JSON à envoyer: $json")
                sendToServer(json.toString())

            } catch (e: Exception) {
                Toast.makeText(this@UserMetricsActivity, "❌ Erreur lecture: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(dayJson: JSONObject) {
        try {
            tvStatus.text = "Données du ${dayJson.getString("date")}"

            // Pas
            val totalSteps = dayJson.optLong("totalSteps", 0)
            tvSteps.text = "👣 Pas : $totalSteps"

            // Distance
            val totalDistanceKm = dayJson.optString("totalDistanceKm", "0.00")
            tvDistance.text = "📏 Distance : $totalDistanceKm km"

            // Fréquence cardiaque
            val avgHeartRate = dayJson.optInt("avgHeartRate", 0)
            tvHeartRate.text = "❤️ BPM moyen : $avgHeartRate"

            // Sommeil
            val totalSleepHours = dayJson.optString("totalSleepHours", "0.0")
            tvSleep.text = "😴 Sommeil : $totalSleepHours h"

            // Hydratation
            val totalHydration = dayJson.optString("totalHydrationLiters", "0.00")
            tvHydration.text = "💧 Hydratation : $totalHydration L"

            // Stress
            val stressLevel = dayJson.optString("stressLevel", "Inconnu")
            val stressScore = dayJson.optInt("stressScore", 0)
            tvStress.text = "🧠 Stress : $stressLevel ($stressScore)"

            // Exercices
            val exercises = dayJson.optJSONArray("exercise") ?: JSONArray()
            tvExerciseCount.text = "🏋️ Exercices : ${exercises.length()}"
            exerciseContainer.removeAllViews()
            for (i in 0 until exercises.length()) {
                val ex = exercises.getJSONObject(i)
                val textView = android.widget.TextView(this)
                textView.text =
                    "- ${ex.optString("exerciseTypeName")} (${ex.optString("durationMinutes")} min, ${ex.optString("distanceKm", "0.00")} km)"
                exerciseContainer.addView(textView)
            }

            // SpO2
            val oxygenArray = dayJson.optJSONArray("oxygenSaturation") ?: JSONArray()
            if (oxygenArray.length() > 0) {
                val lastO2 = oxygenArray.getJSONObject(oxygenArray.length() - 1)
                tvSpO2.text = "🫁 SpO₂ : ${lastO2.optDouble("percentage", 0.0)}%"
            } else {
                tvSpO2.text = "🫁 SpO₂ : -"
            }

            // Température corporelle
            val tempArray = dayJson.optJSONArray("bodyTemperature") ?: JSONArray()
            if (tempArray.length() > 0) {
                val lastTemp = tempArray.getJSONObject(tempArray.length() - 1)
                tvTemperature.text = "🌡 Température : ${lastTemp.optDouble("temperature", 0.0)} °C"
            } else {
                tvTemperature.text = "🌡 Température : -"
            }

            // Pression artérielle
            val bpArray = dayJson.optJSONArray("bloodPressure") ?: JSONArray()
            if (bpArray.length() > 0) {
                val lastBP = bpArray.getJSONObject(bpArray.length() - 1)
                tvBloodPressure.text =
                    "🩸 Tension : ${lastBP.optDouble("systolic", 0.0)}/${lastBP.optDouble("diastolic", 0.0)} mmHg"
            } else {
                tvBloodPressure.text = "🩸 Tension : -"
            }

            // Poids
            val weightArray = dayJson.optJSONArray("weight") ?: JSONArray()
            if (weightArray.length() > 0) {
                val lastWeight = weightArray.getJSONObject(weightArray.length() - 1)
                tvWeight.text = "⚖️ Poids : ${lastWeight.optDouble("weight", 0.0)} kg"
            } else {
                tvWeight.text = "⚖️ Poids : -"
            }

            // JSON complet (facultatif)
            tvJsonData.text = dayJson.toString(2)

        } catch (e: Exception) {
            tvStatus.text = "❌ Erreur mise à jour UI : ${e.message}"
            e.printStackTrace()
        }
    }

    private fun calculateStressLevel(avgHeartRate: Int, sleepHours: Double, steps: Int): String {
        var stressPoints = 0

        stressPoints += when {
            avgHeartRate > 90 -> 40
            avgHeartRate > 80 -> 25
            avgHeartRate > 70 -> 10
            else -> 0
        }

        stressPoints += when {
            sleepHours < 5 -> 30
            sleepHours < 6 -> 20
            sleepHours < 7 -> 10
            else -> 0
        }

        stressPoints += when {
            steps < 2000 -> 30
            steps < 5000 -> 15
            steps < 8000 -> 5
            else -> 0
        }

        return when {
            stressPoints >= 60 -> "Très élevé"
            stressPoints >= 40 -> "Élevé"
            stressPoints >= 20 -> "Modéré"
            else -> "Faible"
        }
    }

    private fun calculateStressScore(level: String): Int {
        return when (level) {
            "Très élevé" -> 80
            "Élevé" -> 60
            "Modéré" -> 40
            else -> 20
        }
    }

    private fun sendToServer(jsonData: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@UserMetricsActivity, "🔄 Connexion au serveur...", Toast.LENGTH_SHORT).show()

                val result = withContext(Dispatchers.IO) {
                    val serverUrl = "https://pleuropneumonic-ferromagnetic-conrad.ngrok-free.dev/fetch"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UserMetricsActivity, "📡 POST vers: $serverUrl", Toast.LENGTH_SHORT).show()
                    }

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val requestBody = jsonData.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(serverUrl)
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "Aucune réponse"

                    Log.d("HealthSync", "Response: $responseCode - $responseBody")

                    response.close()

                    Pair(responseCode, responseBody)
                }

                val (responseCode, responseBody) = result

                if (responseCode in 200..299) {
                    Toast.makeText(
                        this@UserMetricsActivity,
                        "✅ Succès! $responseBody",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@UserMetricsActivity,
                        "❌ Erreur HTTP $responseCode",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@UserMetricsActivity,
                    "❌ Erreur: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
