package com.smartcampus.resources;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> getSensors(@QueryParam("type") String type) {
        if (type == null || type.isEmpty()) {
            return new ArrayList<>(SmartCampusApplication.sensors.values());
        }
        
        return SmartCampusApplication.sensors.values().stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sensor ID is required").build();
        }

        // Validate roomId exists
        String roomId = sensor.getRoomId();
        if (roomId == null || !SmartCampusApplication.rooms.containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Referenced roomId does not exist.");
        }

        // Add sensor
        SmartCampusApplication.sensors.put(sensor.getId(), sensor);

        // Update room's sensor list
        Room room = SmartCampusApplication.rooms.get(roomId);
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<String>());
        }
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = SmartCampusApplication.sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found");
        }
        return Response.ok(sensor).build();
    }

    // Sub-resource locator for Phase 5
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Validation: ensures the sensor exists
        if (!SmartCampusApplication.sensors.containsKey(sensorId)) {
            throw new NotFoundException("Sensor not found");
        }
        return new SensorReadingResource(sensorId);
    }
}
