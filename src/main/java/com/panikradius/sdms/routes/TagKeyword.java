package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.TableTagKeyword;
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

@Path("tagKeyword")

public class TagKeyword {

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMany() throws JsonProcessingException, SQLException {
        return TableTagKeyword.getTable();
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@QueryParam("id") int id) {
        TableTagKeyword.deleteById(id);
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response edit(com.panikradius.sdms.models.TagKeyword tagKeyword) {
        TableTagKeyword.Edit(tagKeyword);
        return Response.ok().build();
    }

    @POST
    @Path("/post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(com.panikradius.sdms.models.TagKeyword tagKeyword) {

//        if (TableTagKeyword.isAlreadyExisting(tagKeyword)) {
//            String msg = "could not save tagKeyword with name: " + tagKeyword.keyword + " because it already exists";
//            Logger.log(msg, Log.LogLevel.INFO);
//            return Response.serverError().build();
//        }

        TableTagKeyword.postPreparedStatement(tagKeyword);
        return Response.ok().build();
    }
}
