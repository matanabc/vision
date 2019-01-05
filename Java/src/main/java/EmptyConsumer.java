
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class EmptyConsumer implements Runnable{

	protected BlockingQueue<MatTime> queue = null;
	
	private CvSource imageSource = null;
	private MjpegServer cvStream = null;
	
	public EmptyConsumer(BlockingQueue<MatTime> queue) {
		//creating camera object 
		
		this.queue = queue;
		
		this.imageSource = new CvSource("CV Image and detect Source", VideoMode.PixelFormat.kMJPEG, 320, 240, 10);
		this.cvStream = new MjpegServer("CV Image and detect Stream", 1185);
		this.cvStream.setSource(imageSource);
	}


	public void run() {


		Calendar cal = Calendar.getInstance();
		long time = cal.getTimeInMillis();
		int c=0;
		
			while(true){
			try {
				Mat inputImage = this.queue.take().getMat();
				//System.out.println(inputImage == null);

				this.imageSource.putFrame(inputImage);
				
				//print fps	
				if (Calendar.getInstance().getTimeInMillis() - time > 1000) {
					time = Calendar.getInstance().getTimeInMillis();
					System.out.println(Thread.currentThread().getName() + " fps: " + c);
					c = 0;
				}else {
					c++;
				}

				
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
