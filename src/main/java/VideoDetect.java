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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoDetect {
    public static int i=0;
    public static int j = 0;
    private static String bucket = "newvision";
    private static String video = "office3.mp4";
    private static String queueUrl =  "https://sqs.eu-west-1.amazonaws.com/717509710948/newvision";
    private static String topicArn="arn:aws:sns:eu-west-1:717509710948:AmazonRekognitionNewVision";
    private static String roleArn="arn:aws:iam::717509710948:role/Rekognition";
    private static AmazonSQS sqs = null;
    private static AmazonRekognition rek = null;
    public static ArrayList<String> people = new ArrayList<>();
    public static ArrayList<Integer> timestampForPeople = new ArrayList<>();
    public static ArrayList<String> jsonFrames = new ArrayList<>();
    //:TODO фреймрейт і довжину відео можна витягнути з сдк, треба це зробити
    private static int frameRate = 30;
    private static int duration = 25659;

    private static NotificationChannel channel= new NotificationChannel()
            .withSNSTopicArn(topicArn)
            .withRoleArn(roleArn);


    private static String startJobId = null;


    public static void main(String[] args)  throws Exception{


        sqs = AmazonSQSClientBuilder.defaultClient();
        rek = AmazonRekognitionClientBuilder.defaultClient();

        //=================================================
        //StartLabels(bucket, video);
        StartPersons(bucket,video);
        //StartFaces(bucket,video);

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
                            GetResultsPersons();
                            //GetResultsFaces();
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

        long numberOfAllFrames = duration/1000*frameRate;
        long oneFrameTime = duration/numberOfAllFrames;
        long begin = 0;
        long end = oneFrameTime;

        for (int k = 0; k <numberOfAllFrames ; k++) {
            toJsonFrame(begin,end,k);
            begin+=oneFrameTime;
            end+=oneFrameTime;
        }
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
                j++;
                long seconds=detectedPerson.getTimestamp()/1000;
                System.out.println("========================================");
                System.out.print("Sec: " + Long.toString(seconds) + " ");
                if(detectedPerson.getPerson().getFace()!=null)
                System.out.println("Person Identifier: "  + detectedPerson.getPerson().getFace().toString());
                System.out.println(": "  + detectedPerson.getPerson().getBoundingBox());
                System.out.println(": "  + detectedPerson.getPerson().toString());
                System.out.println("========================================");

                String contextBody = "{Index: "+detectedPerson.getPerson().getIndex()+", BoundingBox: "+detectedPerson.getPerson().getBoundingBox()+"}";
                FaceDetailsDva fdva = new FaceDetailsDva();
                try {
                    fdva.copy(detectedPerson.getPerson().getFace());
                }catch (NullPointerException e){
                    System.out.println("No faces found");
                }
                toJsonList(contextBody,fdva.toString(),j,detectedPerson.getTimestamp().toString());
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

        int maxResults=1000;
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
            List<FaceDetection> faces= faceDetectionResult.getFaces();
            int j=0;
            for (FaceDetection face: faces) {
                j++;
                long seconds=face.getTimestamp()/1000;
                long time = (face.getTimestamp());
                System.out.print("Sec: " + Long.toString(seconds) + " ");
                FaceDetailsDva fdva = new FaceDetailsDva();
                fdva.copy(face.getFace());
                System.out.println(fdva.toString());
                toJsonList("", fdva.toString(),j, Long.toString(time));
                System.out.println();
            }
        } while (faceDetectionResult !=null && faceDetectionResult.getNextToken() != null);
    }


    static String contextFinal = "";
    private static void toJsonList(String contextBody, String contextFace, Integer index, String time) throws FileNotFoundException, UnsupportedEncodingException {
        String contextInfo = "Info: {TimeStamp: "+time+"}";
        timestampForPeople.add(Integer.valueOf(time));
        contextBody = "Body: "+contextBody+"";
        contextFace = "Face: "+contextFace+"";


        if(contextBody==""||contextBody==null)
        {
            contextFinal  = "{"+contextInfo+", "+contextFace+"}";
        }else{
            if(contextFace==""||contextFace==null){
                contextFinal  = "{"+contextInfo+", "+contextBody+"}";
            }else{
                contextFinal  = "{"+contextInfo+", "+contextBody+", "+contextFace+"}";
            }
        }
        people.add(contextFinal);
//        PrintWriter writer = new PrintWriter(index+".json","UTF-8");
//        writer.println(contextFinal);
//        writer.close();

    }
    private static void toJsonFrame(long begin, long end, int frameNumber){
        String jsonToSave = "{frame: "+frameNumber+", people: [";
        for (int k = 0; k < people.size(); k++) {
            if(timestampForPeople.get(k)>=begin && timestampForPeople.get(k)<end){
                jsonToSave+=(people.get(k)+", ");
            }
        }
        if(','==(jsonToSave.charAt(jsonToSave.length()-2))){
            jsonToSave=jsonToSave.substring(0,jsonToSave.length()-2);
        }
        jsonToSave+="]}";
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("jsons\\"+frameNumber+".json","UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        jsonToSave=quotesAdding(jsonToSave);
        writer.println(jsonToSave);
        writer.close();
    }

    private static String quotesAdding(String str){
        str=str.replace("\'","");
        for (int i = 1; i <str.length()-1 ; i++) {
            StringBuffer buff = new StringBuffer(str);
            Pattern p2 = Pattern.compile("[a-zA-Z]");
            Matcher m1 = p2.matcher(String.valueOf(str.charAt(i)));
            Matcher m2 = p2.matcher(String.valueOf(str.charAt(i-1)));
            Matcher m3 = p2.matcher(String.valueOf(str.charAt(i+1)));
            if(m1.find()==true && m2.find()==false && str.charAt(i-1)!='\"'){
                buff.insert(i,"\"");
                str= String.valueOf(buff);
            }

        }
        for (int i = 1; i <str.length()-1 ; i++) {
            StringBuffer buff = new StringBuffer(str);
            Pattern p2 = Pattern.compile("[a-zA-Z]");
            Matcher m1 = p2.matcher(String.valueOf(str.charAt(i)));
            Matcher m2 = p2.matcher(String.valueOf(str.charAt(i-1)));
            Matcher m3 = p2.matcher(String.valueOf(str.charAt(i+1)));

            if(m1.find()==true && m3.find()==false && str.charAt(i+1)!='\"'){
                buff.insert(i+1,"\"");
                str= String.valueOf(buff);
            }
        }
        return str;
    }
}