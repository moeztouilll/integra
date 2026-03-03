package edu.connexion3a8.tools;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class QRCodeUtils {

    public static Image generateQRCode(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decodeQRCode(File qrCodeFile) {
        try {
            BufferedImage bufferedImage = ImageIO.read(qrCodeFile);
            if (bufferedImage == null)
                return null;
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (IOException | NotFoundException e) {
            System.err.println("QR Code decoding failed: " + e.getMessage());
            return null;
        }
    }
}
