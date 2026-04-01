package com.panikradius.sdms.routes;

import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.HomeStats;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("home")
public class Home {

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStats() {
        try {
            String result = HomeStats.getStats();
            return Response.ok(result).build();
        } catch (Exception e) {
            String msg = "Stats fetch error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }
}
