import com.amazonaws.services.rekognition.model.Emotion;

/**
 * Created by Laimil on 03.10.2018.
 */
public class EmotionsDva extends Emotion {
    public void copy(Emotion emotion){
        super.setConfidence(emotion.getConfidence());
        super.setType(emotion.getType());
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(this.getType() != null) {
            sb.append("Type: ").append("\'"+this.getType()+"\'").append(",");
        }

        if(this.getConfidence() != null) {
            sb.append("Confidence: ").append(this.getConfidence());
        }

        sb.append("}");
        return sb.toString();
    }
}
