package com.example.healthsync.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class HealthData {

    @JsonProperty("dailyData")
    private List<DailyData> dailyData;

    @Data
    public static class DailyData {
        private String date;
        private List<StepRecord> steps;
        private Integer totalSteps;
        private List<HeartRateRecord> heartRate;
        private Integer minHeartRate;
        private Integer maxHeartRate;
        private Integer avgHeartRate;
        private List<DistanceRecord> distance;
        private String totalDistanceKm;
        private List<SleepRecord> sleep;
        private String totalSleepHours;
        private List<ExerciseRecord> exercise;
        private List<OxygenSaturationRecord> oxygenSaturation;
        private List<BodyTemperatureRecord> bodyTemperature;
        private List<BloodPressureRecord> bloodPressure;
        private List<WeightRecord> weight;
        private List<HeightRecord> height;
        private String stressLevel;
        private Integer stressScore;
    }

    @Data
    public static class StepRecord {
        private Long count;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class HeartRateRecord {
        private List<Long> samples;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class DistanceRecord {
        private Double distanceMeters;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class SleepRecord {
        private String title;
        private String startTime;
        private String endTime;
        private Long durationMinutes;
    }

    @Data
    public static class ExerciseRecord {
        private String title;
        private Integer exerciseType;
        private String exerciseTypeName;
        private String startTime;
        private String endTime;
        private Long durationMinutes;
        private Long steps;
        private Integer calories;
        private Integer avgHeartRate;
        private Integer maxHeartRate;
        private Integer avgCadence;
        private String avgSpeedKmh;
        private String avgStrideLengthMeters;
    }

    @Data
    public static class OxygenSaturationRecord {
        private Double percentage;
        private String time;
    }

    @Data
    public static class BodyTemperatureRecord {
        private Double temperature;
        private String time;
    }

    @Data
    public static class BloodPressureRecord {
        private Double systolic;
        private Double diastolic;
        private String time;
    }

    @Data
    public static class WeightRecord {
        private Double weight;
        private String time;
    }

    @Data
    public static class HeightRecord {
        private Double height;
        private String time;
    }
}