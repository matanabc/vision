import java.util.Calendar;

import org.opencv.core.Mat;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;

public class ImageStream {

	private CvSource imageSource = null;
	private MjpegServer cvStream = null;
	
	private int FPS, FPSCount = 0;
	private long lestFPSPrintTime, lestFramePutTime;

	
	/**
	 * @param FPS - Who match FPS to stream
	 */
	public ImageStream(int FPS) {

		// This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
		// operations 
		imageSource = new CvSource("CV Image and detect Source", VideoMode.PixelFormat.kMJPEG, 0, 0, 0);
		cvStream = new MjpegServer("CV Image and detect Stream", 1185);
		cvStream.setSource(imageSource);
		
		this.FPS = FPS > 0 ? FPS : 1;
		lestFPSPrintTime = Calendar.getInstance().getTimeInMillis(); 
		lestFramePutTime = lestFPSPrintTime;
	}
	
	public void putFrame(Mat frame) {
		if (Calendar.getInstance().getTimeInMillis() - lestFPSPrintTime > 1000) {
			System.out.println("fps: " + FPSCount);
			imageSource.putFrame(frame);
			
			lestFPSPrintTime = Calendar.getInstance().getTimeInMillis();
			lestFramePutTime = lestFPSPrintTime;
			
			FPSCount = 0;
			
		} else if(Calendar.getInstance().getTimeInMillis() - lestFramePutTime > 1000 / FPS){
			imageSource.putFrame(frame);
			
			lestFramePutTime = Calendar.getInstance().getTimeInMillis();
		} else {
			FPSCount++;
		}		
	}
}
