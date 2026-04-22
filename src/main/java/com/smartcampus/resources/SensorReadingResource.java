package com.smartcampus.resources;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class SensorReadingResource {
    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<SensorReading> getReadings() {
        return SmartCampusApplication.readings.getOrDefault(sensorId, new ArrayList<>());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = SmartCampusApplication.sensors.get(sensorId);
        
        // Validation: Check if sensor is under maintenance
        if (sensor != null && "MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException();
        }

        // Add reading to history
        List<SensorReading> sensorReadings = SmartCampusApplication.readings.get(sensorId);
        if (sensorReadings == null) {
            sensorReadings = new ArrayList<>();
            SmartCampusApplication.readings.put(sensorId, sensorReadings);
        }
        sensorReadings.add(reading);

        // Update parent sensor's current value
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }

        return Response.ok(reading).build();
    }
}
