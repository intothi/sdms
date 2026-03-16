package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.TableTag;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

@Path("tag")
public class Tag {

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMany() throws JsonProcessingException, SQLException {
        return TableTag.getTable(0, 0);
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@QueryParam("id") int id) {
        TableTag.deleteById(id);
        return Response.ok().build();
    }

    @POST
    @Path("/post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(com.panikradius.sdms.models.Tag tag) {
        //TODO TAG validation --> check for double entries
        if (TableTag.isAlreadyExisting(tag)) {
            String msg = "could not save tag with name: " + tag.name + " because it already exists";
            Logger.log(msg, Log.LogLevel.INFO);
            return Response.serverError().build();
        }
        tag.dateTimeCreated = new java.sql.Timestamp(System.currentTimeMillis());
        TableTag.postPreparedStatement(tag);
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(
            @QueryParam("id") int id,
            com.panikradius.sdms.models.Tag tag) {
        TableTag.EditById(id, tag);
        return Response.ok().build();
    }
}
