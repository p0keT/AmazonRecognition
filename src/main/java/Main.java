//Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//PDX-License-Identifier: MIT-0 (For details, see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;

public class Main {

    public static ArrayList<Integer> leftR = new ArrayList<>();
    public static ArrayList<Integer> topR = new ArrayList<>();
    public static ArrayList<Integer> heightR = new ArrayList<>();
    public static ArrayList<Integer> widthR = new ArrayList<>();
    public static int k=0;

    public static void main(String[] args) throws Exception {

        String photo = "input13.jpg";

        //Get Rekognition client
        AmazonRekognition amazonRekognition = AmazonRekognitionClientBuilder.defaultClient();


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

        //Call detect faces and show face age and placement

        try{
            DetectFacesRequest request = new DetectFacesRequest()
                    .withImage(new Image()
                            .withBytes((imageBytes)))
                    .withAttributes(Attribute.ALL);


            DetectFacesResult result = amazonRekognition.detectFaces(request);
            System.out.println("Orientation: " + result.getOrientationCorrection() + "\n");
            List <FaceDetail> faceDetails = result.getFaceDetails();

            for (FaceDetail face: faceDetails) {
                System.out.println("Face:");
                ShowBoundingBoxPositions(height,
                        width,
                        face.getBoundingBox(),
                        result.getOrientationCorrection());
                AgeRange ageRange = face.getAgeRange();
                System.out.println(face);
                System.out.println("The detected face is estimated to be between "
                        + ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
                        + " years old.");
                System.out.println();
            }

        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
        }


        ImageFrame frame = new ImageFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }


    public static void ShowBoundingBoxPositions(int imageHeight, int imageWidth, BoundingBox box, String rotation) {

        float left = 0;
        float top = 0;

//        if(rotation==null){
//            System.out.println("No estimated estimated orientation. Check Exif data.");
//            return;
//        }
//        //Calculate face position based on image orientation.
//        switch (rotation) {
//            case "ROTATE_0":
//                left = imageWidth * box.getLeft();
//                top = imageHeight * box.getTop();
//                break;
//            case "ROTATE_90":
//                left = imageHeight * (1 - (box.getTop() + box.getHeight()));
//                top = imageWidth * box.getLeft();
//                break;
//            case "ROTATE_180":
//                left = imageWidth - (imageWidth * (box.getLeft() + box.getWidth()));
//                top = imageHeight * (1 - (box.getTop() + box.getHeight()));
//                break;
//            case "ROTATE_270":
//                left = imageHeight * box.getTop();
//                top = imageWidth * (1 - box.getLeft() - box.getWidth());
//                break;
//            default:
//                System.out.println("No estimated orientation information. Check Exif data.");
//                return;
//        }
        left = imageWidth * box.getLeft();
        top = imageHeight * box.getTop();
        //Display face location information.
        k++;
        System.out.println(k);
        System.out.println("Left: " + String.valueOf((int) left));
        System.out.println("Top: " + String.valueOf((int) top));
        System.out.println("Face Width: " + String.valueOf((int)(imageWidth * box.getWidth())));
        System.out.println("Face Height: " + String.valueOf((int)(imageHeight * box.getHeight())));
        leftR.add((int) left);
        topR.add((int) top);
        heightR.add((int)(imageHeight * box.getHeight()));
        widthR.add((int)(imageWidth * box.getWidth()));

    }

    static class ImageFrame extends JFrame
    {
        public ImageFrame()
        {
            setTitle("ImageTest");
            setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // Добавление компонента к фрейму.

            ImageComponent component = new ImageComponent();
            add(component);
        }
        public static final int DEFAULT_WIDTH = 300;
        public static final int DEFAULT_HEIGHT = 200;
    }
    static class ImageComponent extends JComponent
    {
        public ImageComponent()
        {
            // Получаем изображения.
            try
            {
                image = ImageIO.read(new File("input2.jpg"));
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        public void paintComponent(Graphics g)
        {
            if(image == null) return;
            int imageWidth = image.getWidth(this);
            int imageHeight = image.getHeight(this);

            // Отображение рисунка в левом верхнем углу.
            g.drawImage(image, 0, 0, null);
            g.setColor(Color.RED);

            for (int i = 0; i <leftR.size() ; i++) {
                g.drawString(String.valueOf(i+1),leftR.get(i),topR.get(i));
                g.drawRect(leftR.get(i),topR.get(i),widthR.get(i),heightR.get(i));
            }
            // Многократный вывод изображения в панели.

            for(int i = 0; i * imageWidth <= getWidth(); i++)
                for(int j = 0; j * imageHeight <= getHeight(); j++)
                    if(i + j > 0)
                        g.copyArea(0, 0, imageWidth, imageHeight, i * imageWidth, j * imageHeight);
        }
        private BufferedImage image;
    }
}
 