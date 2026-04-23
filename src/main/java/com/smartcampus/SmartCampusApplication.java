package com.smartcampus;

import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartCampusApplication extends ResourceConfig {
    // In-memory data structures
    public static Map<String, Room> rooms = new HashMap<>();
    public static Map<String, Sensor> sensors = new HashMap<>();
    public static Map<String, List<SensorReading>> readings = new HashMap<>();

    public SmartCampusApplication() {
        // Scan for resource classes
        packages("com.smartcampus.resources", "com.smartcampus.exceptions", "com.smartcampus");
    }
}
