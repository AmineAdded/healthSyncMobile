package com.example.healthsync.controller;

import com.example.healthsync.model.HealthData;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/fetch")
@CrossOrigin(origins = "*") // autorise les appels de ton appli Android
public class HealthDataController {

    @PostMapping
    public ResponseEntity<String> receiveHealthData(@RequestBody HealthData healthData) {
        try {
            System.out.println("===== Données reçues depuis Android =====");

            if (healthData.getSteps() != null)
                System.out.println("👣 Steps: " + healthData.getSteps().size());

            if (healthData.getHeartRate() != null)
                System.out.println("❤️ Heart Rates: " + healthData.getHeartRate().size());

            if (healthData.getDistance() != null)
                System.out.println("📏 Distances: " + healthData.getDistance().size());

            if (healthData.getSleep() != null)
                System.out.println("💤 Sleep sessions: " + healthData.getSleep().size());

            if (healthData.getExercise() != null)
                System.out.println("🏋️ Exercises: " + healthData.getExercise().size());

            // Ici tu pourrais :
            // ✅ Sauvegarder dans une base de données (MongoDB, MySQL, etc.)
            // ✅ Calculer des statistiques
            // ✅ Générer des graphiques

            return ResponseEntity.ok("Données reçues avec succès ✅");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors du traitement des données: " + e.getMessage());
        }
    }
}
