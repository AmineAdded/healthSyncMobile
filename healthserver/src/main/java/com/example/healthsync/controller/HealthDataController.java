package com.example.healthsync.controller;

import com.example.healthsync.model.HealthData;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/fetch")
@CrossOrigin(origins = "*")
public class HealthDataController {

    @GetMapping
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("✅ Serveur Spring Boot accessible depuis le téléphone!");
    }
    @PostMapping
    public ResponseEntity<String> receiveHealthData(@RequestBody HealthData healthData) {
        try {
            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║  🔔 DONNÉES HEALTH CONNECT REÇUES DEPUIS ANDROID     ║");
            System.out.println("╚════════════════════════════════════════════════════════╝\n");

            int totalDataPoints = 0;

            // 👣 Steps
            if (healthData.getSteps() != null && !healthData.getSteps().isEmpty()) {
                int count = healthData.getSteps().size();
                totalDataPoints += count;
                long totalSteps = healthData.getSteps().stream()
                        .mapToLong(HealthData.StepRecord::getCount)
                        .sum();

                System.out.println("👣 STEPS (" + count + " entrées):");
                System.out.println("   Total: " + totalSteps + " pas");

                // Afficher les 3 dernières entrées
                healthData.getSteps().stream()
                        .limit(3)
                        .forEach(step ->
                                System.out.println("   • " + step.getCount() + " pas | " + step.getStartTime())
                        );
                System.out.println();
            }

            // ❤️ Heart Rate
            if (healthData.getHeartRate() != null && !healthData.getHeartRate().isEmpty()) {
                int count = healthData.getHeartRate().size();
                totalDataPoints += count;

                System.out.println("❤️ HEART RATE (" + count + " entrées):");

                healthData.getHeartRate().stream()
                        .limit(3)
                        .forEach(hr -> {
                            if (hr.getSamples() != null && !hr.getSamples().isEmpty()) {
                                double avg = hr.getSamples().stream()
                                        .mapToDouble(Double::doubleValue)
                                        .average()
                                        .orElse(0.0);
                                System.out.println("   • Moyenne: " + String.format("%.1f", avg) + " bpm | " + hr.getStartTime());
                            }
                        });
                System.out.println();
            }

            // 📏 Distance
            if (healthData.getDistance() != null && !healthData.getDistance().isEmpty()) {
                int count = healthData.getDistance().size();
                totalDataPoints += count;
                double totalKm = healthData.getDistance().stream()
                        .mapToDouble(HealthData.DistanceRecord::getDistanceMeters)
                        .sum() / 1000.0;

                System.out.println("📏 DISTANCE (" + count + " entrées):");
                System.out.println("   Total: " + String.format("%.2f", totalKm) + " km");
                System.out.println();
            }

            // 💤 Sleep
            if (healthData.getSleep() != null && !healthData.getSleep().isEmpty()) {
                int count = healthData.getSleep().size();
                totalDataPoints += count;

                System.out.println("💤 SLEEP (" + count + " sessions):");
                healthData.getSleep().stream()
                        .limit(3)
                        .forEach(sleep ->
                                System.out.println("   • " + sleep.getTitle() + " | " +
                                        sleep.getStartTime() + " → " + sleep.getEndTime())
                        );
                System.out.println();
            }

            // 🏋️ Exercise
            if (healthData.getExercise() != null && !healthData.getExercise().isEmpty()) {
                int count = healthData.getExercise().size();
                totalDataPoints += count;

                System.out.println("🏋️ EXERCISE (" + count + " sessions):");
                healthData.getExercise().stream()
                        .limit(3)
                        .forEach(ex ->
                                System.out.println("   • " + ex.getTitle() + " (Type: " + ex.getExerciseType() +
                                        ") | " + ex.getStartTime())
                        );
                System.out.println();
            }

            // 🫁 Oxygen Saturation
            if (healthData.getOxygenSaturation() != null && !healthData.getOxygenSaturation().isEmpty()) {
                int count = healthData.getOxygenSaturation().size();
                totalDataPoints += count;

                System.out.println("🫁 OXYGEN SATURATION (" + count + " mesures):");
                healthData.getOxygenSaturation().stream()
                        .limit(3)
                        .forEach(o2 ->
                                System.out.println("   • " + String.format("%.1f", o2.getPercentage()) +
                                        "% | " + o2.getTime())
                        );
                System.out.println();
            }

            // 🌡️ Body Temperature
            if (healthData.getBodyTemperature() != null && !healthData.getBodyTemperature().isEmpty()) {
                int count = healthData.getBodyTemperature().size();
                totalDataPoints += count;

                System.out.println("🌡️ BODY TEMPERATURE (" + count + " mesures):");
                healthData.getBodyTemperature().stream()
                        .limit(3)
                        .forEach(temp ->
                                System.out.println("   • " + String.format("%.1f", temp.getTemperature()) +
                                        "°C | " + temp.getTime())
                        );
                System.out.println();
            }

            // 💉 Blood Pressure
            if (healthData.getBloodPressure() != null && !healthData.getBloodPressure().isEmpty()) {
                int count = healthData.getBloodPressure().size();
                totalDataPoints += count;

                System.out.println("💉 BLOOD PRESSURE (" + count + " mesures):");
                healthData.getBloodPressure().stream()
                        .limit(3)
                        .forEach(bp ->
                                System.out.println("   • " + String.format("%.0f", bp.getSystolic()) +
                                        "/" + String.format("%.0f", bp.getDiastolic()) +
                                        " mmHg | " + bp.getTime())
                        );
                System.out.println();
            }

            // ⚖️ Weight
            if (healthData.getWeight() != null && !healthData.getWeight().isEmpty()) {
                int count = healthData.getWeight().size();
                totalDataPoints += count;

                System.out.println("⚖️ WEIGHT (" + count + " mesures):");
                healthData.getWeight().stream()
                        .limit(3)
                        .forEach(w ->
                                System.out.println("   • " + String.format("%.1f", w.getWeight()) +
                                        " kg | " + w.getTime())
                        );
                System.out.println();
            }

            // 📏 Height
            if (healthData.getHeight() != null && !healthData.getHeight().isEmpty()) {
                int count = healthData.getHeight().size();
                totalDataPoints += count;

                System.out.println("📏 HEIGHT (" + count + " mesures):");
                healthData.getHeight().stream()
                        .limit(3)
                        .forEach(h ->
                                System.out.println("   • " + String.format("%.2f", h.getHeight()) +
                                        " m | " + h.getTime())
                        );
                System.out.println();
            }

            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║  ✅ SUCCÈS - " + totalDataPoints + " points de données reçus         ║");
            System.out.println("╚════════════════════════════════════════════════════════╝\n");

            // TODO: Sauvegarder dans MySQL (healthsync_db)
            // TODO: Générer des graphiques et statistiques

            return ResponseEntity.ok("✅ " + totalDataPoints + " données reçues et traitées avec succès!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("\n❌ ERREUR lors du traitement des données: " + e.getMessage() + "\n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }
}