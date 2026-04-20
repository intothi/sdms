package com.panikradius.sdms.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.panikradius.sdms.App;
import com.panikradius.sdms.Logger;
import com.panikradius.sdms.db.DbConnection;
import com.panikradius.sdms.db.TableDocument;
import com.panikradius.sdms.db.TableDocumentTag;
import com.panikradius.sdms.db.TableTag;
import com.panikradius.sdms.db.TableTagKeyword;
import com.panikradius.sdms.models.DocumentTag;
import com.panikradius.sdms.models.Log;
import com.panikradius.sdms.models.TagKeyword;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Path("document")
public class Document {

    @GET
    @Path("/table")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMany(
            @QueryParam("skip") int skip,
            @QueryParam("top") int top,
            @QueryParam("name") String name,
            @QueryParam("tags") String tags,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortDir") String sortDir)
            throws JsonProcessingException {

        String[] parts = !tags.isEmpty() ? tags.split(",") : new String[0];

        return TableDocument.getTable(skip, top, name, parts, sortBy, sortDir);
    }

    @GET
    @Produces("application/pdf")
    public Response getSingle(@QueryParam("fileName") String fileName) {
        return TableDocument.get(fileName);
    }

    @GET
    @Path("/thread")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getThread(@QueryParam("id") int id) {
        try {
            String result = TableDocument.getThread(id);
            return Response.ok(result).build();
        } catch (Exception e) {
            String msg = "Thread fetch error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public Response delete(@QueryParam("id") int id) {

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);

            connection.setAutoCommit(false);

            String fileName = TableDocument.getFileNameById(connection, id);
            if (fileName == null) {
                Logger.log("document delete requested -> fileName not found in Db", Log.LogLevel.ERROR);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TableDocumentTag.deleteByDocumentId(connection, id);
            TableDocument.deleteById(connection, id);

            connection.commit();

            if (!Files.deleteIfExists(Paths.get(App.pathToDms + fileName))) {
                String msg = "The document has been deleted from the database " +
                        "but could not be found in the file system -> orphaned entry: " + fileName;

                Logger.log(msg, Log.LogLevel.INFO);
            }


        } catch (Exception e) {
            try { if (connection != null) connection.rollback(); } catch (Exception ignored) {}
            String msg = "Delete error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        } finally {
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }

        Logger.log("document deleted --> id=" + id, Log.LogLevel.INFO);
        return Response.ok().build();
    }

    @POST
    @Path("/post")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(
            @FormDataParam("file") InputStream inputStream,
            @FormDataParam("file") FormDataContentDisposition fileInfo,
            @FormDataParam("comment") String comment,
            @FormDataParam("tagIds") String tagIds,
            @FormDataParam("dateDocument") String dateDocument,
            @FormDataParam("dueDate") String dueDateString,
            @FormDataParam("parentId") Integer parentId
    ) {
        long timeStart = System.nanoTime();

        byte [] fileBytes = getFileBytes(inputStream);
        if (fileBytes == null) {
            String msg = "File stream read error";
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        double timeEndGetFileBytes = (System.nanoTime() - timeStart) / 1_000_000_000.0;

        PDDocument pdf = null;
        try {
            pdf = PDDocument.load(fileBytes);
        } catch (IOException e) {
            String msg = "PDF load error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        double timeEndLoadPfd = (System.nanoTime() - timeStart) / 1_000_000_000.0 - timeEndGetFileBytes;

        PDFTextStripper stripper = null;
        String existingText = null;
        try {
            stripper = new PDFTextStripper();
            //TODO very poor performance
            existingText = stripper.getText(pdf);
        } catch (IOException e) {
            String msg = "PDF strip error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        int countPages = pdf.getNumberOfPages();
        try {
            pdf.close();
        } catch (IOException e) {
            String msg = "Could not close PDF, this could leak memory: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
        }

        String ocrText = existingText.trim();

        double timeEndStripPfd = (System.nanoTime() - timeStart) / 1_000_000_000.0 - timeEndGetFileBytes - timeEndLoadPfd;

        Set<Integer> matchedTagIds = getMatchedTagIds(ocrText);
        Set<Integer> allTagIds = new HashSet<>();

        if (!tagIds.equals("")) {
            String[] parts = tagIds.split(",");
            for (int i = 0; i<parts.length; i++) {
                allTagIds.add(Integer.parseInt(parts[i]));
            }
        }

        allTagIds.addAll(matchedTagIds);
        Integer[] allTagIdsArray = allTagIds.toArray(new Integer[0]);

        double timeEndTagResolving = ((System.nanoTime() - timeStart) / 1_000_000_000.0)
                -timeEndGetFileBytes -timeEndLoadPfd -timeEndStripPfd;

        Timestamp timestamp = new java.sql.Timestamp(System.currentTimeMillis());
        String dateNameForFile = timestamp.toLocalDateTime()
                .atZone(java.time.ZoneId.of("UTC"))
                .withZoneSameInstant(java.time.ZoneId.of("Europe/Berlin"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss"));

        String fileName;

        //TODO better exception handling, thus move vars to the bottom
        int documentID = 0;
        double timeEndFilenameAndObjectBuilding = 0;
        double timeEndChecksum = 0;

        try {

            List<String> tagNames = TableTag.getNamesByIds(allTagIdsArray);
            StringBuilder fileNameBuilder = new StringBuilder(dateNameForFile);
            for (int i = 0; i < tagNames.size(); i++) {
                fileNameBuilder.append("_").append(replaceUmlautsAndOtherSpecial(tagNames.get(i)));
            }
            fileName = fileNameBuilder + ".pdf";

            if (TableDocument.isAlreadyExisting(fileName)) {
                String msg = "could not save document with name: " + fileName + " because it already exists";
                Logger.log(msg, Log.LogLevel.INFO);
                return Response.serverError().build();
            }

            Date dueDate = dueDateString == null ? null : Date.valueOf(dueDateString);

            com.panikradius.sdms.models.Document document = new com.panikradius.sdms.models.Document(
                    fileName,
                    comment,
                    Date.valueOf(dateDocument),
                    dueDate,
                    timestamp
            );

            document.deskewDone = false;
            document.parentId = parentId;
            document.fileSize = fileBytes.length;
            // NOTE(CT) if we want to add support for other file formats Tika seems to be the way to go
            document.mimeType = "application/pdf";
            // NOTE(CT) Lingua or tika for language recognition
            document.language = "de";

            timeEndFilenameAndObjectBuilding =
                    ((System.nanoTime() - timeStart) / 1_000_000_000.0)
                            -timeEndGetFileBytes -timeEndLoadPfd -timeEndStripPfd -timeEndTagResolving;

            // create checksum
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                stringBuilder.append(String.format("%02x", hash[i]));
            }
            document.checksum = stringBuilder.toString();

            timeEndChecksum =
                    ((System.nanoTime() - timeStart) / 1_000_000_000.0)
                            -timeEndGetFileBytes -timeEndLoadPfd -timeEndStripPfd
                            -timeEndTagResolving -timeEndFilenameAndObjectBuilding;

            document.countPages = countPages;
            document.ocrText = ocrText;

            String trimmed = document.ocrText.trim();
            document.countWords = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;

            Connection connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);

            connection.setAutoCommit(false);
            documentID = TableDocument.post(connection, document);
            if (documentID == 0) {
                throw new Exception();
            }

            for (int i = 0; i< allTagIdsArray.length; i++) {
                TableDocumentTag.postPreparedStatement(
                        connection,
                        new DocumentTag(documentID, allTagIdsArray[i]));
            }

            connection.commit();
            try { connection.close(); } catch (Exception e) { /* Ignored */ }
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Logger.log("//" + e.getMessage(), Log.LogLevel.ERROR);
            return Response.serverError().entity(e.getMessage()).build();
        }

        String path = App.pathToDms + fileName;
        try {
            Files.write(Paths.get(path), fileBytes);
        } catch (IOException e) {
            String msg = "File write error: " + fileName;
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }

        double timeEnd = (System.nanoTime() - timeStart) / 1_000_000_000.0;
        String msg = "new document archived --> " + documentID
                + " --> timeGetFileBytes=" + String.format("%.2f", timeEndGetFileBytes)+" sec."
                + " --> timeLoadPFD=" + String.format("%.2f", timeEndLoadPfd)+" sec."
                + " --> timeStripPFD=" + String.format("%.2f", timeEndStripPfd)+" sec."
                + " --> timeTagResolving=" + String.format("%.2f", timeEndTagResolving)+" sec."
                + " --> timeFileName/ObjectBuild=" + String.format("%.2f", timeEndFilenameAndObjectBuilding)+" sec."
                + " --> timeChecksum=" + String.format("%.2f", timeEndChecksum)+" sec."
                + " --> TotalSavetime=" + String.format("%.2f", timeEnd)+" sec.";
        Logger.log(msg, Log.LogLevel.INFO);
        System.gc();
        return Response.ok().build();
    }

    private static byte[] getFileBytes(InputStream inputStream){

        byte[] fileBytes = null;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            fileBytes = buffer.toByteArray();
        } catch (IOException e) {
            Logger.log(e.getMessage(), Log.LogLevel.ERROR);
        }

        return fileBytes;
    }

    private static Set<Integer> getMatchedTagIds(String ocrText) {
        Set<Integer> matchedTagIds = new HashSet<>();
        try {

            String text = ocrText.toLowerCase();

            Map<TagKeyword, Integer> keywordToTagId = TableTagKeyword.getKeywordToTagIdMap();

            for (Map.Entry<TagKeyword, Integer> entry : keywordToTagId.entrySet()) {

                TagKeyword tagKeyword = entry.getKey();

                if (tagKeyword.exactMatch) {
                    Pattern pattern = Pattern.compile("\\b" + Pattern.quote(tagKeyword.keyword) + "\\b");
                    if (pattern.matcher(text).find()) {
                        matchedTagIds.add(entry.getValue());
                    }
                } else {
                    if (text.contains(tagKeyword.keyword)) {
                        matchedTagIds.add(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            Logger.log("PDF keyword extraction failed: " + e.getMessage(), Log.LogLevel.ERROR);
        }
        return matchedTagIds;
    }

    @PUT
    @Path("/meta")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMeta(
            @QueryParam("id") int id,
            com.panikradius.sdms.models.DocumentMeta meta) {
        try {
            TableDocument.updateMeta(id, meta);
            return Response.ok().build();
        } catch (Exception e) {
            String msg = "Document meta update error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

    @PUT
    @Path("/done")
    public Response toggleDone(@QueryParam("id") int id, @QueryParam("done") boolean done) {
        try {
            TableDocument.updateDone(id, done);
            return Response.ok().build();
        } catch (Exception e) {
            String msg = "Document done update error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        }
    }

    @PUT
    @Path("/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTags(@QueryParam("id") int id, com.panikradius.sdms.models.TagUpdateRequest body) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    DbConnection.DB_URL,
                    DbConnection.USER,
                    DbConnection.PW);
            connection.setAutoCommit(false);

            String oldFileName = TableDocument.getFileNameById(connection, id);
            if (oldFileName == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String timestamp = oldFileName.substring(0, 19);

            Integer[] tagIdsArray = body.tagIds.toArray(new Integer[0]);
            List<String> tagNames = TableTag.getNamesByIds(tagIdsArray);

            StringBuilder fileNameBuilder = new StringBuilder(timestamp);
            for (int i = 0; i < tagNames.size(); i++) {
                fileNameBuilder.append("_").append(replaceUmlautsAndOtherSpecial(tagNames.get(i)));
            }
            String newFileName = fileNameBuilder + ".pdf";

            // First update Tags in DB
            TableDocumentTag.deleteByDocumentId(connection, id);
            for (int i = 0; i < tagIdsArray.length; i++) {
                TableDocumentTag.postPreparedStatement(connection, new DocumentTag(id, tagIdsArray[i]));
            }

            // Second update filename in DB
            TableDocument.updateFileName(connection, id, newFileName);

            // Last update Filename on filesystem
            java.nio.file.Path oldPath = Paths.get(App.pathToDms + oldFileName);
            java.nio.file.Path newPath = Paths.get(App.pathToDms + newFileName);
            Files.move(oldPath, newPath);

            connection.commit();

            Logger.log("document renamed: " + oldFileName + " -> " + newFileName, Log.LogLevel.INFO);
            return Response.ok().build();

        } catch (Exception e) {
            try { if (connection != null) connection.rollback(); } catch (Exception ignored) {}
            String msg = "Tags update error: " + e.getMessage();
            Logger.log(msg, Log.LogLevel.ERROR);
            return Response.serverError().entity(msg).build();
        } finally {
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
    }

    private static String replaceUmlautsAndOtherSpecial(String input) {
        return input
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("Ä", "Ae")
                .replace("Ö", "Oe")
                .replace("Ü", "Ue")
                .replace("ß", "ss")
                .replace("/","_")
                .replace(" ", "_");
    }
}
