package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.db.TableLog;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

@Path("log")
public class Log {

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMany(@QueryParam("skip") int skip, @QueryParam("top") int top) throws JsonProcessingException, SQLException {
        return TableLog.getTable(skip, top);
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@QueryParam("id") int id) {
        TableLog.deleteById(id);
        return Response.ok().build();
    }

}
