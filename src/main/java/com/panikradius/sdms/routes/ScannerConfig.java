package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.TableScannerConfig;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

@Path("scannerConfig")
public class ScannerConfig {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSingle() throws SQLException, JsonProcessingException {
        com.panikradius.sdms.models.ScannerConfig scannerConfig = TableScannerConfig.get();
        if (scannerConfig == null) {return Response.status(Response.Status.NOT_FOUND).build();}
        return Response.ok(scannerConfig).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(com.panikradius.sdms.models.ScannerConfig config) {
        try {
            TableScannerConfig.update(config);
            return Response.ok().build();
        } catch (SQLException e) {
            String msg = "Scanner config update error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

}
