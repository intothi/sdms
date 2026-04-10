package com.panikradius.sdms.routes;

import com.panikradius.sdms.Logger;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("scan")
public class Scan {

    private static volatile boolean scanRunning = false;

    @POST
    @Path("/start")
    public Response start() {
        scanRunning = true;
        Logger.log("Scan gestartet", Log.LogLevel.INFO);
        return Response.ok().build();
    }

    @POST
    @Path("/done")
    public Response done() {
        scanRunning = false;
        Logger.log("Scan abgeschlossen", Log.LogLevel.INFO);
        return Response.ok().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.ok("{\"running\":" + scanRunning + "}").build();
    }
}