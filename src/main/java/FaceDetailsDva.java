import com.amazonaws.services.rekognition.model.FaceDetail;

/**
 * Created by Laimil on 03.10.2018.
 */
public class FaceDetailsDva extends FaceDetail{
    FaceDetailsDva(){
        super();
    }

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
            sb.append("Emotions: ").append(((EmotionsDva)this.getEmotions()).toString()).append(",");
        }

        if(this.getLandmarks() != null) {
            sb.append("Landmarks: ").append((LandmarksDva)this.getLandmarks()).append(",");
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
}
