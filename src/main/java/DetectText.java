import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.util.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;



public class DetectText {

    public static void main(String[] args) throws Exception {


        String photo = "input10.jpg";
        String bucket = "bucket";

        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

// Load image
        ByteBuffer imageBytes=null;
        BufferedImage image = null;

        try (InputStream inputStream = new FileInputStream(new File(photo))) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));

        }
        catch(Exception e)
        {
            System.out.println("Failed to load file " + photo);
            System.exit(1);
        }

        //Get image width and height
        InputStream imageBytesStream;
        imageBytesStream = new ByteArrayInputStream(imageBytes.array());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image=ImageIO.read(imageBytesStream);
        ImageIO.write(image, "jpg", baos);

        int height = image.getHeight();
        int width = image.getWidth();

        System.out.println("Image Information:");
        System.out.println(photo);
        System.out.println("Image Height: " + Integer.toString(height));
        System.out.println("Image Width: " + Integer.toString(width));

        DetectTextRequest request = new DetectTextRequest()
                .withImage(new Image()
                        .withBytes((imageBytes)))
                ;


        try {
            DetectTextResult result = rekognitionClient.detectText(request);
            List<TextDetection> textDetections = result.getTextDetections();

            System.out.println("Detected lines and words for " + photo);
            for (TextDetection text: textDetections) {

                System.out.println("Detected: " + text.getDetectedText());
                System.out.println("Confidence: " + text.getConfidence().toString());
                System.out.println("Id : " + text.getId());
                System.out.println("Parent Id: " + text.getParentId());
                System.out.println("Type: " + text.getType());
                System.out.println();
            }
        } catch(AmazonRekognitionException e) {
            e.printStackTrace();
        }
    }
}