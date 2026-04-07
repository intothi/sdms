package com.panikradius.sdms;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class OcrService {

    private static final String TESSDATA_PATH = "/usr/share/tesseract-ocr/5/tessdata";
    private static final String LANGUAGE = "deu";

    public static Tesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(TESSDATA_PATH);
        tesseract.setLanguage(LANGUAGE);
        tesseract.setOcrEngineMode(1);
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
}
