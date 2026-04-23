package com.smartcampus.resources;

import com.smartcampus.SmartCampusApplication;
import com.smartcampus.models.Room;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public List<Room> getAllRooms() {
        return new ArrayList<>(SmartCampusApplication.rooms.values());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room ID is required").build();
        }
        
        SmartCampusApplication.rooms.put(room.getId(), room);
        
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = SmartCampusApplication.rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room not found");
        }
        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = SmartCampusApplication.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NO_CONTENT).build(); // Idempotent
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            // This will be handled by RoomNotEmptyException in Phase 6
            // But I'll implement the check here or throw a specific exception
            throw new com.smartcampus.exceptions.RoomNotEmptyException();
        }

        SmartCampusApplication.rooms.remove(roomId);
        return Response.ok().build();
    }
}
