package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.BackupService;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.TableBackup;
import com.panikradius.sdms.db.TableBackupConfig;
import com.panikradius.sdms.models.BackupConfig;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

@Path("backup")
public class Backup {

    @GET
    @Path("/download")
    @Produces("application/gzip")
    public Response download() {
        try {
            BackupConfig config = TableBackupConfig.get();
            byte[] archive = BackupService.createBackup(config, BackupService.CREATED_BY_USER);
            return Response.ok(archive)
                    .header("Content-Disposition", "attachment; filename=\"sdms_backup.tar.gz\"")
                    .build();
        } catch (Exception e) {
            String msg = "Backup download error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTable(
            @QueryParam("skip") int skip,
            @QueryParam("top") int top) throws JsonProcessingException, SQLException {
        try {
            return TableBackup.getTable(skip, top);
        } catch (Exception e) {
            String msg = "Backup table error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return "{\"items\":[],\"totalCount\":0}";
        }
    }

    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() {
        try {
            BackupConfig config = TableBackupConfig.get();
            return Response.ok(config).build();
        } catch (Exception e) {
            String msg = "Backup config get error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

    @PUT
    @Path("/config")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfig(BackupConfig config) {
        try {
            TableBackupConfig.update(config);
            return Response.ok().build();
        } catch (Exception e) {
            String msg = "Backup config update error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }
}