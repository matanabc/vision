import org.opencv.core.Mat;

public final class MatTime {
	private Mat mat;
	private long time;
	
	public MatTime(Mat mat, Long time) {
		super();
		this.mat = mat;
		this.time = time;
	}
	@Override
	public String toString() {
		return "MatTime [mat=" + mat + ", time=" + time + "]";
	}
	public Mat getMat() {
		return mat;
	}
	public long getTime() {
		return time;
	}	
}
