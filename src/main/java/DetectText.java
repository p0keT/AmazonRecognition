import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.util.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;



public class DetectText {

    public static void main(String[] args) throws Exception {


        String photo = "Joined_97_15421846834059_Price.jpg";
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
            String price = "0.0";
            DetectTextResult result = rekognitionClient.detectText(request);
            List<TextDetection> textDetections = result.getTextDetections();
            Iterator<TextDetection> iter = textDetections.iterator();
            while (iter.hasNext()) {
                TextDetection td = iter.next();
                System.out.println(td.getDetectedText());
                td.setDetectedText(td.getDetectedText().replaceAll("[^0-9]",""));
                if (
                        !(td.getParentId()==null &&
                        td.getDetectedText().matches("[0-9]+")&&
                        td.getGeometry().getBoundingBox().getHeight()>0.05)
                        //td.getConfidence()<99.0
                        ) {
                    iter.remove();
                }
            }
            Iterator<TextDetection> iter2 = textDetections.iterator();
            while (iter2.hasNext()) {
                TextDetection td = iter2.next();
                if(td.getConfidence()>95.0)
                System.out.println(td.getDetectedText()+"----"+td.getConfidence());
            }

            if(textDetections.size()==1){
                if(textDetections.get(0).getDetectedText().length()>2){
                    StringBuilder detText = new StringBuilder(textDetections.get(0).getDetectedText());
                    detText.insert(detText.length()-2,'.');
                    price = detText.toString();
                }else{
                    price = textDetections.get(0).getDetectedText();
                }
            }else {


                /*
                 * По очереди будем просматривать все подмножества элементов массива (0 -
                 * последний, 1-последний, 2-последний,...)
                 */
                for (int i = 0; i < textDetections.size(); i++) {
                    /*
                     * Предполагаем, что первый элемент (в каждом подмножестве элементов)
                     * является минимальным
                     */
                    TextDetection min = textDetections.get(i);
                    int min_i = i;
                    /*
                     * В оставшейся части подмножества ищем элемент, который меньше
                     * предположенного минимума
                     */
                    for (int j = i + 1; j < textDetections.size(); j++) {
                        // Если находим, запоминаем его индекс
                        if (textDetections.get(j).getGeometry().getBoundingBox().getHeight() <
                                min.getGeometry().getBoundingBox().getHeight()) {
                            min = textDetections.get(j);
                            min_i = j;
                        }
                    }
                    /*
                     * Если нашелся элемент, меньший, чем на текущей позиции, меняем их
                     * местами
                     */
                    if (i != min_i) {
                        TextDetection tmp = textDetections.get(i);
                        textDetections.set(i, textDetections.get(min_i));
                        textDetections.set(min_i, tmp);
                    }
                }

                price = textDetections.get(textDetections.size() - 1).getDetectedText() +
                        "." +
                        textDetections.get(textDetections.size() - 2).getDetectedText();
            }
            System.out.println("\nPrice: " + Double.valueOf(price));


        } catch(AmazonRekognitionException e) {
            e.printStackTrace();
        }
    }
}