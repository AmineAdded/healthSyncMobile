package com.example.healthsync.model;

import java.util.List;

public class HealthData {

    private List<StepRecord> steps;
    private List<HeartRateRecord> heartRate;
    private List<DistanceRecord> distance;
    private List<SleepRecord> sleep;
    private List<ExerciseRecord> exercise;
    private List<OxygenSaturationRecord> oxygenSaturation;
    private List<BodyTemperatureRecord> bodyTemperature;
    private List<BloodPressureRecord> bloodPressure;
    private List<WeightRecord> weight;
    private List<HeightRecord> height;

    // --- Steps ---
    public static class StepRecord {
        private int count;
        private String startTime;
        private String endTime;

        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- Heart Rate ---
    public static class HeartRateRecord {
        private List<Double> samples;
        private String startTime;
        private String endTime;

        public List<Double> getSamples() { return samples; }
        public void setSamples(List<Double> samples) { this.samples = samples; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- Distance ---
    public static class DistanceRecord {
        private double distanceMeters;
        private String startTime;
        private String endTime;

        public double getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- Sleep ---
    public static class SleepRecord {
        private String title;
        private String startTime;
        private String endTime;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- Exercise ---
    public static class ExerciseRecord {
        private String title;
        private int exerciseType;
        private String startTime;
        private String endTime;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getExerciseType() { return exerciseType; }
        public void setExerciseType(int exerciseType) { this.exerciseType = exerciseType; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- Oxygen Saturation 🫁 ---
    public static class OxygenSaturationRecord {
        private double percentage;
        private String time;

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // --- Body Temperature 🌡️ ---
    public static class BodyTemperatureRecord {
        private double temperature;
        private String time;

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // --- Blood Pressure 💉 ---
    public static class BloodPressureRecord {
        private double systolic;
        private double diastolic;
        private String time;

        public double getSystolic() { return systolic; }
        public void setSystolic(double systolic) { this.systolic = systolic; }
        public double getDiastolic() { return diastolic; }
        public void setDiastolic(double diastolic) { this.diastolic = diastolic; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // --- Weight ⚖️ ---
    public static class WeightRecord {
        private double weight;
        private String time;

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // --- Height 📏 ---
    public static class HeightRecord {
        private double height;
        private String time;

        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // --- Getters/Setters globaux ---
    public List<StepRecord> getSteps() { return steps; }
    public void setSteps(List<StepRecord> steps) { this.steps = steps; }

    public List<HeartRateRecord> getHeartRate() { return heartRate; }
    public void setHeartRate(List<HeartRateRecord> heartRate) { this.heartRate = heartRate; }

    public List<DistanceRecord> getDistance() { return distance; }
    public void setDistance(List<DistanceRecord> distance) { this.distance = distance; }

    public List<SleepRecord> getSleep() { return sleep; }
    public void setSleep(List<SleepRecord> sleep) { this.sleep = sleep; }

    public List<ExerciseRecord> getExercise() { return exercise; }
    public void setExercise(List<ExerciseRecord> exercise) { this.exercise = exercise; }

    public List<OxygenSaturationRecord> getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(List<OxygenSaturationRecord> oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }

    public List<BodyTemperatureRecord> getBodyTemperature() { return bodyTemperature; }
    public void setBodyTemperature(List<BodyTemperatureRecord> bodyTemperature) { this.bodyTemperature = bodyTemperature; }

    public List<BloodPressureRecord> getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(List<BloodPressureRecord> bloodPressure) { this.bloodPressure = bloodPressure; }

    public List<WeightRecord> getWeight() { return weight; }
    public void setWeight(List<WeightRecord> weight) { this.weight = weight; }

    public List<HeightRecord> getHeight() { return height; }
    public void setHeight(List<HeightRecord> height) { this.height = height; }
}