package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.App;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.DbConnection;
import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.db.TableDocumentTag;
import com.panikradius.sdms.models.DocumentTag;
import com.panikradius.sdms.models.Log;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
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

//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    public String getSingle(@QueryParam("id") int id) throws JsonProcessingException {
//        return TableDocument.get(id);
//    }

    @POST
    @Path("/post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            @FormDataParam("file") InputStream fileStream,
            @FormDataParam("file") FormDataContentDisposition fileInfo,
            @FormDataParam("name") String name,
            @FormDataParam("comment") String comment,
            @FormDataParam("tagIds") String tagIds,
            @FormDataParam("dateDocument") String dateDocument
    ) {

        long timeDB = System.nanoTime();
        long size = fileInfo.getSize();
        // TODO check vor duplicates in DB

        try {
            com.panikradius.sdms.models.Document document = new com.panikradius.sdms.models.Document();
            document.name = name;
            document.comment = comment;
            document.dateDocument = Date.valueOf(dateDocument);
            document.dateTimeArchived = new java.sql.Timestamp(System.currentTimeMillis());

            Connection connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);

            connection.setAutoCommit(false);
            int documentID = TableDocument.post(connection, document);
            if (documentID == 0) {
                throw new Exception();
            }

            String[] parts = tagIds.split(",");
            int[] tagIdArray = new int[parts.length];
            for (int i = 0; i<parts.length; i++) {
                tagIdArray[i] = Integer.parseInt(parts[i]);
            }

            for (int i = 0; i< tagIdArray.length; i++) {
                TableDocumentTag.postPreparedStatement(
                        connection,
                        new DocumentTag(documentID, tagIdArray[i]));
            }

            connection.commit();
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
            
        } catch (Exception e) {
            String msg = "Database error";
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        timeDB = (System.nanoTime() - timeDB) / 1000000;

        long timeIO = System.nanoTime();
        String path = App.pathToDms + fileInfo.getFileName();
        try {
            Files.copy(fileStream, Paths.get(path));
        } catch (IOException e) {
            // TODO exception file already exists
            String msg = "File write error";
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
        timeIO = (System.nanoTime() - timeIO) / 1000000;

        String msg = "archived document --> savetimeDB=" + timeDB + " / savetimeIO=" + timeIO;
        Logger.log(msg, Log.LogLevel.INFO);

        return Response.ok().build();
    }


//    @DELETE
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response delete(@QueryParam("id") int id) {
//        TableDocument.deleteById(id);
//        return Response.ok().build();
//    }

}
