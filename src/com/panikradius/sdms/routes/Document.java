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
    public String getMany(
            @QueryParam("skip") int skip,
            @QueryParam("top") int top,
            @QueryParam("name") String name,
            @QueryParam("tags") String tags)
            throws JsonProcessingException, SQLException {

        String[] parts = !tags.isEmpty() ? tags.split(",") : new String[0];

        return TableDocument.getTable(skip, top, name, parts);
    }

    @GET
    @Produces("application/pdf")
    public Response getSingle(@QueryParam("fileName") String fileName) {
        return TableDocument.get(fileName);
    }

    @POST
    @Path("/post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            @FormDataParam("file") InputStream fileStream,
            @FormDataParam("file") FormDataContentDisposition fileInfo,
            @FormDataParam("comment") String comment,
            @FormDataParam("tagIds") String tagIds,
            @FormDataParam("dateDocument") String dateDocument
    ) {

        long timeDB = System.nanoTime();
        String fileName = fileInfo.getFileName();

        if (TableDocument.isAlreadyExisting(fileName)) {
            String msg = "could not save document with name: " + fileName + " because it already exists";
            Logger.log(msg, Log.LogLevel.INFO);
            return Response.serverError().build();
        }

        try {
            com.panikradius.sdms.models.Document document = new com.panikradius.sdms.models.Document();
            document.fileName = fileName;
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

            if (!tagIds.equals("")) {
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
            }

            connection.commit();
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
            
        } catch (Exception e) {
            String msg = "Database error";
            System.out.println(e.getMessage());
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        timeDB = (System.nanoTime() - timeDB) / 1000000;

        long timeIO = System.nanoTime();

        String path = App.pathToDms + fileName;
        try {
            Files.copy(fileStream, Paths.get(path));
        } catch (IOException e) {
            // TODO exception file already exists
            String msg = "File write error: " + fileName;
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
        timeIO = (System.nanoTime() - timeIO) / 1000000;

        String msg = "new document archived --> " + fileName + " --> savetimeDB=" + timeDB + " / savetimeIO=" + timeIO;
        Logger.log(msg, Log.LogLevel.INFO);
        return Response.ok().build();
    }
}
