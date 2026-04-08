package com.panikradius.sdms;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OcrService {

    private static final String TESSDATA_PATH = "/usr/share/tesseract-ocr/5/tessdata";
    private static final String LANGUAGE = "deu";

    public static Tesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(TESSDATA_PATH);
        tesseract.setLanguage(LANGUAGE);
        tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        return tesseract;
    }

    public static BufferedImage correctRotation(Tesseract tesseract, BufferedImage image){
        int rotate = 0;
        try {
            tesseract.setPageSegMode(0);
            String osd = tesseract.doOCR(image);
            String[] lines = osd.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("Rotate:")) {
                    rotate = Integer.parseInt(lines[i].split(":")[1].trim());
                    break;
                }
            }
        } catch (TesseractException e) {}

        if (rotate != 0) {
            double angle = Math.toRadians(rotate);
            AffineTransform transform = AffineTransform.getRotateInstance(
                    angle,
                    image.getWidth() / 2.0,
                    image.getHeight() / 2.0
            );
            AffineTransformOp affineTransformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            image = affineTransformOp.filter(image, null);
        }

        return image;
    }

    public static String performOcr(Tesseract tesseract, BufferedImage image) throws Exception {
        tesseract.setPageSegMode(3);
        return tesseract.doOCR(image);
    }

    public static byte[] createSearchablePdf(Tesseract tesseract, List<BufferedImage> pages) throws Exception {
        List<File> pagePdfs = new ArrayList<>();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        for (int i = 0; i < pages.size(); i++) {
            File tempImage = File.createTempFile("ocr_page_" + i + "_", ".png", tmpDir);
            ImageIO.write(pages.get(i), "png", tempImage);

            File tempOut = File.createTempFile("ocr_out_" + i + "_", "", tmpDir);
            tesseract.createDocuments(
                    tempImage.getAbsolutePath(),
                    tempOut.getAbsolutePath(),
                    java.util.Arrays.asList(ITesseract.RenderedFormat.PDF)
            );

            pagePdfs.add(new File(tempOut.getAbsolutePath() + ".pdf"));
            tempImage.delete();
            tempOut.delete();
        }

        // PDFs zusammenführen
        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        pdfMergerUtility.setDestinationStream(byteArrayOutputStream);
        for (int i = 0; i < pagePdfs.size(); i++) {
            pdfMergerUtility.addSource(pagePdfs.get(i));
        }
        pdfMergerUtility.mergeDocuments(null);

        for (int i = 0; i < pagePdfs.size(); i++) {
            pagePdfs.get(i).delete();
        }

        return byteArrayOutputStream.toByteArray();
    }
}
