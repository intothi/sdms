package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.db.TableDocument;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.SQLException;

@Path("document")
public class Document {

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMany(@QueryParam("skip") int skip, @QueryParam("top") int top)
            throws JsonProcessingException, SQLException {

        return TableDocument.getTable(skip, top);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSingle(@QueryParam("id") int id) throws JsonProcessingException {
        return TableDocument.get(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(com.panikradius.sdms.models.Document documentApi) {

        com.panikradius.sdms.models.Document document = new com.panikradius.sdms.models.Document(
                0,
                documentApi.name,
                documentApi.comment,
                documentApi.filePath,
                documentApi.dateDocument,
                null
        );

        TableDocument.post(document);
        return Response.ok().build();
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@QueryParam("id") int id) {
        TableDocument.deleteById(id);
        return Response.ok().build();
    }
}
