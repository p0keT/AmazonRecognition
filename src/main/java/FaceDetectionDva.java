import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceDetection;

/**
 * Created by Laimil on 03.10.2018.
 */
public class FaceDetectionDva extends FaceDetection {

    private FaceDetailsDva face;


    public void setFace(FaceDetailsDva face) {
        this.face = face;
    }

    public FaceDetailsDva getFace() {
        return this.face;
    }
}
