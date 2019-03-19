import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

public class Contour{
	private MatOfPoint MatOfPoint; 
	private double angle;

	public Contour(MatOfPoint matOfPoint, double angle) {
		this.MatOfPoint = matOfPoint;
		this.angle = angle;
	}

	public Contour(MatOfPoint matOfPoint) {
		this.MatOfPoint = matOfPoint;
		if(this.MatOfPoint.toArray().length > 5) {
			angle = 0;
		} else {
			angle = Imgproc.fitEllipse(new MatOfPoint2f(this.MatOfPoint.toArray())).angle;
		}
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public MatOfPoint getMatOfPoint() {
		return MatOfPoint;
	}
	
	public double getAngle() {
		return angle;
	}
}
