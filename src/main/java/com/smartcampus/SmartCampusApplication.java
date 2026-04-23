package com.smartcampus;

import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import org.glassfish.jersey.server.ResourceConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

public class SmartCampusApplication extends ResourceConfig {
    // In-memory data structures with thread safety
    public static Map<String, Room> rooms = new ConcurrentHashMap<>();
    public static Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    public static Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    public SmartCampusApplication() {
        // Scan for resource classes
        packages("com.smartcampus.resources", "com.smartcampus.exceptions", "com.smartcampus");
    }
}
