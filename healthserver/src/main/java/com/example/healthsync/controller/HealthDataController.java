package com.example.healthsync.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/fetch")
@CrossOrigin(origins = "*")
public class HealthDataController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("✅ Serveur Spring Boot accessible depuis le téléphone!");
    }

    @PostMapping
    public ResponseEntity<String> receiveHealthData(@RequestBody String rawJson) {
        try {
            System.out.println("\n╔═════════════════════════════════════════════════════════════╗");
            System.out.println("║  🔔 DONNÉES HEALTH CONNECT REÇUES DEPUIS ANDROID          ║");
            System.out.println("╚═════════════════════════════════════════════════════════════╝\n");

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode dailyData = root.get("dailyData");

            if (dailyData == null || !dailyData.isArray()) {
                return ResponseEntity.badRequest().body("❌ Format de données invalide");
            }

            int totalDataPoints = 0;

            for (JsonNode day : dailyData) {
                String date = day.get("date").asText();

                System.out.println("\n╭─────────────────────────────────────────────────────────────╮");
                System.out.println("│  📅 DATE: " + date + "                                   │");
                System.out.println("╰─────────────────────────────────────────────────────────────╯\n");

                // 👣 STEPS
                int totalSteps = day.has("totalSteps") ? day.get("totalSteps").asInt() : 0;
                if (totalSteps > 0) {
                    System.out.println("👣 STEPS: " + totalSteps + " pas");
                    totalDataPoints++;
                }

                // ❤️ HEART RATE
                if (day.has("avgHeartRate")) {
                    int avgHR = day.get("avgHeartRate").asInt();
                    int minHR = day.has("minHeartRate") ? day.get("minHeartRate").asInt() : 0;
                    int maxHR = day.has("maxHeartRate") ? day.get("maxHeartRate").asInt() : 0;

                    System.out.println("❤️  HEART RATE:");
                    System.out.println("   • Moyenne: " + avgHR + " bpm");
                    System.out.println("   • Min: " + minHR + " bpm | Max: " + maxHR + " bpm");
                    totalDataPoints++;
                }

                // 📏 DISTANCE
                if (day.has("totalDistanceKm")) {
                    String distKm = day.get("totalDistanceKm").asText();
                    System.out.println("📏 DISTANCE: " + distKm + " km");
                    totalDataPoints++;
                }

                // 💤 SLEEP
                if (day.has("totalSleepHours")) {
                    String sleepHours = day.get("totalSleepHours").asText();
                    System.out.println("💤 SOMMEIL: " + sleepHours + " heures");

                    JsonNode sleepArray = day.get("sleep");
                    if (sleepArray != null && sleepArray.isArray()) {
                        for (JsonNode sleep : sleepArray) {
                            String title = sleep.get("title").asText();
                            String start = sleep.get("startTime").asText();
                            String end = sleep.get("endTime").asText();
                            int duration = sleep.get("durationMinutes").asInt();
                            System.out.println("   • " + title + ": " + start + " → " + end +
                                    " (" + duration + " min)");
                        }
                    }
                    totalDataPoints++;
                }

                // 🏋️ EXERCISE (COMPLET AVEC TOUTES LES MÉTRIQUES)
                JsonNode exerciseArray = day.get("exercise");
                if (exerciseArray != null && exerciseArray.isArray() && exerciseArray.size() > 0) {
                    System.out.println("\n🏋️  EXERCICES (" + exerciseArray.size() + " sessions):");

                    for (int i = 0; i < exerciseArray.size(); i++) {
                        JsonNode ex = exerciseArray.get(i);

                        System.out.println("\n   ┌─ Session " + (i + 1) + " ─────────────────────────────");
                        System.out.println("   │ 🏃 Type: " + ex.get("exerciseTypeName").asText());
                        System.out.println("   │ ⏱️  Durée: " + ex.get("durationMinutes").asInt() + " minutes");
                        System.out.println("   │ 🕐 Début: " + ex.get("startTime").asText());

                        // Distance
                        if (ex.has("distanceKm") && !ex.get("distanceKm").asText().equals("0.00")) {
                            System.out.println("   │ 📏 Distance: " + ex.get("distanceKm").asText() + " km");
                        }

                        // Pas
                        if (ex.has("steps") && ex.get("steps").asInt() > 0) {
                            System.out.println("   │ 👣 Pas: " + ex.get("steps").asInt());
                        }

                        // Calories actives et totales
                        if (ex.has("activeCalories") && ex.get("activeCalories").asInt() > 0) {
                            System.out.println("   │ 🔥 Calories actives: " + ex.get("activeCalories").asInt() + " kcal");
                        }
                        if (ex.has("totalCalories") && ex.get("totalCalories").asInt() > 0) {
                            System.out.println("   │ 🔥 Calories totales: " + ex.get("totalCalories").asInt() + " kcal");
                        }

                        // Fréquence cardiaque
                        if (ex.has("avgHeartRate") && ex.get("avgHeartRate").asInt() > 0) {
                            System.out.println("   │ ❤️  BPM moyen: " + ex.get("avgHeartRate").asInt() + " bpm");
                            if (ex.has("minHeartRate")) {
                                System.out.println("   │ 💚 BPM min: " + ex.get("minHeartRate").asInt() + " bpm");
                            }
                            if (ex.has("maxHeartRate")) {
                                System.out.println("   │ 💓 BPM max: " + ex.get("maxHeartRate").asInt() + " bpm");
                            }
                        }

                        // Cadence (COMPLET)
                        if (ex.has("avgCadence") && ex.get("avgCadence").asInt() > 0) {
                            System.out.println("   │ 🎵 Cadence moyenne: " + ex.get("avgCadence").asInt() + " pas/min");
                            if (ex.has("minCadence") && ex.get("minCadence").asInt() > 0) {
                                System.out.println("   │    ↳ Min: " + ex.get("minCadence").asInt() + " pas/min");
                            }
                            if (ex.has("maxCadence") && ex.get("maxCadence").asInt() > 0) {
                                System.out.println("   │    ↳ Max: " + ex.get("maxCadence").asInt() + " pas/min");
                            }
                        }

                        // Vitesse (COMPLET)
                        if (ex.has("avgSpeedKmh")) {
                            System.out.println("   │ 🏃 Vitesse moyenne: " + ex.get("avgSpeedKmh").asText() + " km/h");
                            if (ex.has("minSpeedKmh")) {
                                System.out.println("   │    ↳ Min: " + ex.get("minSpeedKmh").asText() + " km/h");
                            }
                            if (ex.has("maxSpeedKmh")) {
                                System.out.println("   │    ↳ Max: " + ex.get("maxSpeedKmh").asText() + " km/h");
                            }
                        }

                        // Longueur de foulée (COMPLET)
                        if (ex.has("avgStrideLengthMeters")) {
                            System.out.println("   │ 👟 Foulée moyenne: " + ex.get("avgStrideLengthMeters").asText() + " m");
                            if (ex.has("minStrideLengthMeters")) {
                                System.out.println("   │    ↳ Min: " + ex.get("minStrideLengthMeters").asText() + " m");
                            }
                            if (ex.has("maxStrideLengthMeters")) {
                                System.out.println("   │    ↳ Max: " + ex.get("maxStrideLengthMeters").asText() + " m");
                            }
                        }

                        // Puissance (pour cyclisme)
                        if (ex.has("avgPowerWatts") && ex.get("avgPowerWatts").asInt() > 0) {
                            System.out.println("   │ ⚡ Puissance: " + ex.get("avgPowerWatts").asInt() + " W");
                        }

                        System.out.println("   └────────────────────────────────────────────");
                    }
                    totalDataPoints += exerciseArray.size();
                }

                // 🫁 OXYGEN SATURATION
                JsonNode oxygenArray = day.get("oxygenSaturation");
                if (oxygenArray != null && oxygenArray.isArray() && oxygenArray.size() > 0) {
                    System.out.println("\n🫁 SATURATION O2 (" + oxygenArray.size() + " mesures):");
                    for (int i = 0; i < Math.min(3, oxygenArray.size()); i++) {
                        JsonNode o2 = oxygenArray.get(i);
                        System.out.println("   • " + String.format("%.1f", o2.get("percentage").asDouble()) +
                                "% | " + o2.get("time").asText());
                    }
                    totalDataPoints += oxygenArray.size();
                }

                // 🌡️ TEMPERATURE
                JsonNode tempArray = day.get("bodyTemperature");
                if (tempArray != null && tempArray.isArray() && tempArray.size() > 0) {
                    System.out.println("\n🌡️  TEMPÉRATURE (" + tempArray.size() + " mesures):");
                    for (int i = 0; i < Math.min(3, tempArray.size()); i++) {
                        JsonNode temp = tempArray.get(i);
                        System.out.println("   • " + String.format("%.1f", temp.get("temperature").asDouble()) +
                                "°C | " + temp.get("time").asText());
                    }
                    totalDataPoints += tempArray.size();
                }

                // 💉 BLOOD PRESSURE
                JsonNode bpArray = day.get("bloodPressure");
                if (bpArray != null && bpArray.isArray() && bpArray.size() > 0) {
                    System.out.println("\n💉 PRESSION ARTÉRIELLE (" + bpArray.size() + " mesures):");
                    for (int i = 0; i < Math.min(3, bpArray.size()); i++) {
                        JsonNode bp = bpArray.get(i);
                        System.out.println("   • " +
                                String.format("%.0f", bp.get("systolic").asDouble()) + "/" +
                                String.format("%.0f", bp.get("diastolic").asDouble()) +
                                " mmHg | " + bp.get("time").asText());
                    }
                    totalDataPoints += bpArray.size();
                }

                // ⚖️ WEIGHT
                JsonNode weightArray = day.get("weight");
                if (weightArray != null && weightArray.isArray() && weightArray.size() > 0) {
                    System.out.println("\n⚖️  POIDS (" + weightArray.size() + " mesures):");
                    for (int i = 0; i < Math.min(3, weightArray.size()); i++) {
                        JsonNode w = weightArray.get(i);
                        System.out.println("   • " + String.format("%.1f", w.get("weight").asDouble()) +
                                " kg | " + w.get("time").asText());
                    }
                    totalDataPoints += weightArray.size();
                }

                // 📏 HEIGHT
                JsonNode heightArray = day.get("height");
                if (heightArray != null && heightArray.isArray() && heightArray.size() > 0) {
                    System.out.println("\n📏 TAILLE (" + heightArray.size() + " mesures):");
                    for (int i = 0; i < Math.min(3, heightArray.size()); i++) {
                        JsonNode h = heightArray.get(i);
                        System.out.println("   • " + String.format("%.2f", h.get("height").asDouble()) +
                                " m | " + h.get("time").asText());
                    }
                    totalDataPoints += heightArray.size();
                }

                // 💧 HYDRATATION (NOUVELLE)
                JsonNode hydrationArray = day.get("hydration");
                if (hydrationArray != null && hydrationArray.isArray() && hydrationArray.size() > 0) {
                    String totalHydration = day.has("totalHydrationLiters") ?
                            day.get("totalHydrationLiters").asText() : "0.00";
                    System.out.println("\n💧 HYDRATATION: " + totalHydration + " litres (" +
                            hydrationArray.size() + " prises)");

                    for (int i = 0; i < Math.min(5, hydrationArray.size()); i++) {
                        JsonNode h = hydrationArray.get(i);
                        System.out.println("   • " + String.format("%.0f", h.get("volumeMl").asDouble()) +
                                " ml | " + h.get("time").asText());
                    }
                    totalDataPoints += hydrationArray.size();
                }

                // 🧠 STRESS LEVEL
                if (day.has("stressLevel")) {
                    String stressLevel = day.get("stressLevel").asText();
                    int stressScore = day.get("stressScore").asInt();

                    String stressEmoji = switch (stressLevel) {
                        case "Très élevé" -> "🔴";
                        case "Élevé" -> "🟠";
                        case "Modéré" -> "🟡";
                        default -> "🟢";
                    };

                    System.out.println("\n🧠 NIVEAU DE STRESS: " + stressEmoji + " " + stressLevel +
                            " (" + stressScore + "/100)");
                }

                System.out.println("\n" + "─".repeat(65));
            }

            System.out.println("\n╔═════════════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ SUCCÈS - " + totalDataPoints + " points de données reçus             ║");
            System.out.println("╚═════════════════════════════════════════════════════════════╝\n");

            // TODO: Sauvegarder dans MongoDB (biometric_data collection)
            // TODO: Générer des alertes avec AI (anomaly detection)
            // TODO: Calculer les tendances et patterns (LSTM/Time Series)

            return ResponseEntity.ok("✅ " + totalDataPoints + " données reçues et traitées avec succès!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n❌ ERREUR lors du traitement des données: " + e.getMessage() + "\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }
}