import com.amazonaws.services.rekognition.model.Emotion;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Landmark;

import java.util.ArrayList;

/**
 * Created by Laimil on 03.10.2018.
 */
public class FaceDetailsDva extends FaceDetail{

    ArrayList<Emotion> emotions = new ArrayList<>();
    ArrayList<Landmark> landmarks = new ArrayList<>();

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(this.getBoundingBox() != null) {
            sb.append("BoundingBox: ").append(this.getBoundingBox()).append(",");
        }

        if(this.getAgeRange() != null) {
            sb.append("AgeRange: ").append(this.getAgeRange()).append(",");
        }

        if(this.getSmile() != null) {
            sb.append("Smile: ").append(this.getSmile()).append(",");
        }

        if(this.getEyeglasses() != null) {
            sb.append("Eyeglasses: ").append(this.getEyeglasses()).append(",");
        }

        if(this.getSunglasses() != null) {
            sb.append("Sunglasses: ").append(this.getSunglasses()).append(",");
        }

        if(this.getGender() != null) {
            sb.append("Gender: ").append(this.getGender()).append(",");
        }

        if(this.getBeard() != null) {
            sb.append("Beard: ").append(this.getBeard()).append(",");
        }

        if(this.getMustache() != null) {
            sb.append("Mustache: ").append(this.getMustache()).append(",");
        }

        if(this.getEyesOpen() != null) {
            sb.append("EyesOpen: ").append(this.getEyesOpen()).append(",");
        }

        if(this.getMouthOpen() != null) {
            sb.append("MouthOpen: ").append(this.getMouthOpen()).append(",");
        }

        if(this.getEmotions() != null) {
            sb.append("Emotions: ").append((emotions).toString()).append(",");
        }

        if(this.getLandmarks() != null) {
            sb.append("Landmarks: ").append(landmarks).append(",");
        }

        if(this.getPose() != null) {
            sb.append("Pose: ").append(this.getPose()).append(",");
        }

        if(this.getQuality() != null) {
            sb.append("Quality: ").append(this.getQuality()).append(",");
        }

        if(this.getConfidence() != null) {
            sb.append("Confidence: ").append(this.getConfidence());
        }

        sb.append("}");
        return sb.toString();
    }

    public void copy(FaceDetail fd){
        super.setAgeRange(fd.getAgeRange());
        super.setBeard(fd.getBeard());
        super.setBoundingBox(fd.getBoundingBox());
        super.setConfidence(fd.getConfidence());
        super.setEmotions(fd.getEmotions());
        super.setEyeglasses(fd.getEyeglasses());
        super.setGender(fd.getGender());
        super.setLandmarks(fd.getLandmarks());
        super.setMouthOpen(fd.getMouthOpen());
        super.setMustache(fd.getMustache());
        super.setPose(fd.getPose());
        super.setQuality(fd.getQuality());
        super.setSmile(fd.getSmile());
        super.setSunglasses(fd.getSunglasses());

        if(fd.getEmotions()!=null)
            for(int i=0; i<fd.getEmotions().size(); i++){
                Emotion emtemp = new EmotionsDva();
                EmotionsDva dvatemp = (EmotionsDva) emtemp;
                dvatemp.copy(fd.getEmotions().get(i));
                emotions.add(dvatemp);
        }

        if(fd.getLandmarks()!=null)
            for(int i=0; i<fd.getLandmarks().size(); i++){
                Landmark dvatempland = new LandmarksDva();
                LandmarksDva templand = (LandmarksDva)dvatempland;
                templand.copy(fd.getLandmarks().get(i));
                landmarks.add(templand);
            }

    }
}
