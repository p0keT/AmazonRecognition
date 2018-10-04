import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class VideoDetect {
    public static int i=0;
    private static String bucket = "newvision";
    private static String video = "RomaForTest.mp4";
    private static String queueUrl =  "https://sqs.eu-west-1.amazonaws.com/717509710948/newvision";
    private static String topicArn="arn:aws:sns:eu-west-1:717509710948:AmazonRekognitionNewVision";
    private static String roleArn="arn:aws:iam::717509710948:role/Rekognition";
    private static AmazonSQS sqs = null;
    private static AmazonRekognition rek = null;

    private static NotificationChannel channel= new NotificationChannel()
            .withSNSTopicArn(topicArn)
            .withRoleArn(roleArn);


    private static String startJobId = null;


    public static void main(String[] args)  throws Exception{


        sqs = AmazonSQSClientBuilder.defaultClient();
        rek = AmazonRekognitionClientBuilder.defaultClient();

        //=================================================
        //StartLabels(bucket, video);
        //StartPersons(bucket,video);
        StartFaces(bucket,video);

        //=================================================
        System.out.println("Waiting for job: " + startJobId);
        //Poll queue for messages
        List<Message> messages=null;
        int dotLine=0;
        boolean jobFound=false;

        //loop until the job status is published. Ignore other messages in queue.
        do{
            messages = sqs.receiveMessage(queueUrl).getMessages();
            if (dotLine++<20){
                System.out.print(".");
            }else{
                System.out.println();
                dotLine=0;
            }

            if (!messages.isEmpty()) {
                //Loop through messages received.
                for (Message message: messages) {
                    String notification = message.getBody();
                    System.out.println(message.toString());
                    // Get status and job id from notification.
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonMessageTree = mapper.readTree(notification);
                    JsonNode messageBodyText = jsonMessageTree.get("Message");
                    ObjectMapper operationResultMapper = new ObjectMapper();
                    JsonNode jsonResultTree = operationResultMapper.readTree(messageBodyText.textValue());
                    JsonNode operationJobId = jsonResultTree.get("JobId");
                    JsonNode operationStatus = jsonResultTree.get("Status");
                    System.out.println("Job found was " + operationJobId);
                    // Found job. Get the results and display.
                    if(operationJobId.asText().equals(startJobId)){
                        jobFound=true;
                        System.out.println("Job id: " + operationJobId );
                        System.out.println("Status : " + operationStatus.toString());
                        if (operationStatus.asText().equals("SUCCEEDED")){
                            //============================================
                            //GetResultsLabels();
                            //GetResultsPersons();
                            GetResultsFaces();
                            //============================================
                        }
                        else{
                            System.out.println("Video analysis failed");
                        }

                        sqs.deleteMessage(queueUrl,message.getReceiptHandle());
                    }

                    else{
                        System.out.println("Job received was not job " +  startJobId);
                        //Delete unknown message. Consider moving message to dead letter queue
                        sqs.deleteMessage(queueUrl,message.getReceiptHandle());
                    }
                }
            }
        } while (!jobFound);


        System.out.println("Done!");
    }


    private static void StartLabels(String bucket, String video) throws Exception{

        StartLabelDetectionRequest req = new StartLabelDetectionRequest()
                .withVideo(new Video()
                        .withS3Object(new S3Object()
                                .withBucket(bucket)
                                .withName(video)))
                .withMinConfidence(50F)
                .withJobTag("DetectingLabels")
                .withNotificationChannel(channel);

        StartLabelDetectionResult startLabelDetectionResult = rek.startLabelDetection(req);
        startJobId=startLabelDetectionResult.getJobId();


    }



    private static void GetResultsLabels() throws Exception{

        int maxResults=10;
        String paginationToken=null;
        GetLabelDetectionResult labelDetectionResult=null;

        do {
            if (labelDetectionResult !=null){
                paginationToken = labelDetectionResult.getNextToken();
            }

            GetLabelDetectionRequest labelDetectionRequest= new GetLabelDetectionRequest()
                    .withJobId(startJobId)
                    .withSortBy(LabelDetectionSortBy.TIMESTAMP)
                    .withMaxResults(maxResults)
                    .withNextToken(paginationToken);


            labelDetectionResult = rek.getLabelDetection(labelDetectionRequest);

            VideoMetadata videoMetaData=labelDetectionResult.getVideoMetadata();

            System.out.println("Format: " + videoMetaData.getFormat());
            System.out.println("Codec: " + videoMetaData.getCodec());
            System.out.println("Duration: " + videoMetaData.getDurationMillis());
            System.out.println("FrameRate: " + videoMetaData.getFrameRate());


            //Show labels, confidence and detection times
            List<LabelDetection> detectedLabels= labelDetectionResult.getLabels();

            for (LabelDetection detectedLabel: detectedLabels) {
                long seconds=detectedLabel.getTimestamp();
                System.out.print("Millisecond: " + Long.toString(seconds) + " ");
                System.out.println("\t" + detectedLabel.getLabel().getName() +
                        "     \t" +
                        detectedLabel.getLabel().getConfidence().toString());
                System.out.println();
            }
        } while (labelDetectionResult !=null && labelDetectionResult.getNextToken() != null);

    }

    private static void StartPersons(String bucket, String video) throws Exception{

        int maxResults=10;
        String paginationToken=null;

        StartPersonTrackingRequest req = new StartPersonTrackingRequest()
                .withVideo(new Video()
                        .withS3Object(new S3Object()
                                .withBucket(bucket)
                                .withName(video)))
                .withNotificationChannel(channel);



        StartPersonTrackingResult startPersonDetectionResult = rek.startPersonTracking(req);
        startJobId=startPersonDetectionResult.getJobId();

    }

    private static void GetResultsPersons() throws Exception{
        int maxResults=10;
        String paginationToken=null;
        GetPersonTrackingResult personTrackingResult=null;

        do{
            if (personTrackingResult !=null){
                paginationToken = personTrackingResult.getNextToken();
            }

            personTrackingResult = rek.getPersonTracking(new GetPersonTrackingRequest()
                    .withJobId(startJobId)
                    .withNextToken(paginationToken)
                    .withSortBy(PersonTrackingSortBy.TIMESTAMP)
                    .withMaxResults(maxResults));

            VideoMetadata videoMetaData=personTrackingResult.getVideoMetadata();

            System.out.println("Format: " + videoMetaData.getFormat());
            System.out.println("Codec: " + videoMetaData.getCodec());
            System.out.println("Duration: " + videoMetaData.getDurationMillis());
            System.out.println("FrameRate: " + videoMetaData.getFrameRate());


            //Show persons, confidence and detection times
            List<PersonDetection> detectedPersons= personTrackingResult.getPersons();

            for (PersonDetection detectedPerson: detectedPersons) {

                long seconds=detectedPerson.getTimestamp()/1000;
                System.out.print("Sec: " + Long.toString(seconds) + " ");
                System.out.println("Person Identifier: "  + detectedPerson.getPerson().getFace().getGender().getValue());
                System.out.println("Person Identifier: "  + detectedPerson.getPerson().getBoundingBox());
                System.out.println();
            }
        }  while (personTrackingResult !=null && personTrackingResult.getNextToken() != null);

    }

    private static void StartFaces(String bucket, String video) throws Exception{

        StartFaceDetectionRequest req = new StartFaceDetectionRequest()
                .withVideo(new Video()
                        .withS3Object(new S3Object()
                                .withBucket(bucket)
                                .withName(video)))
                .withNotificationChannel(channel);



        StartFaceDetectionResult startLabelDetectionResult = rek.startFaceDetection(req);
        startJobId=startLabelDetectionResult.getJobId();

    }

    private static void GetResultsFaces() throws Exception{

        int maxResults=100;
        String paginationToken=null;
        GetFaceDetectionResult faceDetectionResult=null;

        do{
            i++;
            System.out.println(i);
            if (faceDetectionResult !=null){
                paginationToken = faceDetectionResult.getNextToken();
            }

            faceDetectionResult = rek.getFaceDetection(new GetFaceDetectionRequest()
                    .withJobId(startJobId)
                    .withNextToken(paginationToken)
                    .withMaxResults(maxResults));

            VideoMetadata videoMetaData=faceDetectionResult.getVideoMetadata();

            System.out.println("Format: " + videoMetaData.getFormat());
            System.out.println("Codec: " + videoMetaData.getCodec());
            System.out.println("Duration: " + videoMetaData.getDurationMillis());
            System.out.println("FrameRate: " + videoMetaData.getFrameRate());


            //Show faces, confidence and detection times
            List<FaceDetectionDva> faces= faceDetectionResult.getFaces();
            int j=0;
            for (FaceDetection face: faces) {
                j++;
                long seconds=face.getTimestamp()/1000;
                long time = (face.getTimestamp());
                System.out.print("Sec: " + Long.toString(seconds) + " ");
                FaceDetailsDva fdva = (FaceDetailsDva) face.getFace();
                System.out.println(fdva.toString());
                toFile(face.getFace().toString(),j, Long.toString(time));
                System.out.println();
            }
        } while (faceDetectionResult !=null && faceDetectionResult.getNextToken() != null);
    }


    private static void toFile(String context, Integer index, String time) throws FileNotFoundException, UnsupportedEncodingException {
        String Path = "";
        String timeJson = "TimeStamp: "+time+",BoundingBox";
        context=context.replace("eyeLeft","\'eyeLeft\'");
        context=context.replace("eyeRight","\'eyeRight\'");
        context=context.replace("nose","\'nose\'");
        context=context.replace("mouthLeft","\'mouthLeft\'");
        context=context.replace("mouthRight","\'mouthRight\'");
        context=context.replace("BoundingBox",timeJson);
        PrintWriter writer = new PrintWriter(index+".json","UTF-8");
        writer.println(context);
        System.out.println(context);
        writer.close();
    }
}