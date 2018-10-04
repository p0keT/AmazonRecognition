import com.amazonaws.services.rekognition.model.Landmark;

/**
 * Created by Laimil on 03.10.2018.
 */
public class LandmarksDva extends Landmark {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(this.getType() != null) {
            sb.append("Type: ").append("\'"+this.getType()+"\'").append(",");
        }

        if(this.getX() != null) {
            sb.append("X: ").append(this.getX()).append(",");
        }

        if(this.getY() != null) {
            sb.append("Y: ").append(this.getY());
        }

        sb.append("}");
        return sb.toString();
    }

}
