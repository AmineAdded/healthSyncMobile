package com.example.healthsync.model;

import java.util.List;

public class HealthData {

    private List<StepRecord> steps;
    private List<HeartRateRecord> heartRate;
    private List<DistanceRecord> distance;
    private List<SleepRecord> sleep;
    private List<ExerciseRecord> exercise;

    // --- Sous-classes pour correspondre à ton JSON ---

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
}
