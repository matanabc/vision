

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
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

public class FrameConsumer implements Runnable{

	private static final String FILE_PLACE = "/home/pi/Documents/vision/Java/Values";//fill place

	protected BlockingQueue<MatTime> queue = null;

	private NetworkTable VisionTable = null;

	//Resolution 
	private int Width;
	private int Height;

	//stream port
	private int streamPortShowCameraSeeAndDetect;//on port 1185 show what the camera see and what he detect
	private int streamPortShowHsv;//on port 1186 show what he see whit the hsv values 

	private CvSource imageSource = null;
	private MjpegServer cvStream = null;

	private CvSource hsvSource = null;
	private MjpegServer hsvStream = null;

	public FrameConsumer(BlockingQueue<MatTime> queue) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(FILE_PLACE));//fill place to use

			this.Width = Integer.parseInt(properties.getProperty("camera_Width", "320"));
			this.Height = Integer.parseInt(properties.getProperty("camera_Height", "240"));

			int FPS = Integer.parseInt(properties.getProperty("camera_FPS", "30"));

			// This creates a CvSource to use. This will take in a Mat image that has had OpenCV operations
			// operations 
			this.imageSource = new CvSource("CV Image and detect Source", VideoMode.PixelFormat.kMJPEG, Width, Height, FPS);
			this.cvStream = new MjpegServer("CV Image and detect Stream", Integer.parseInt(properties.getProperty("frame_Port", "1185")));
			this.cvStream.setSource(imageSource);

			this.hsvSource= new CvSource("CV hsv Source", VideoMode.PixelFormat.kMJPEG, Width, Height, FPS);
			this.hsvStream = new MjpegServer("CV hsv Stream",  Integer.parseInt(properties.getProperty("HSV_Port", "1186")));
			this.hsvStream.setSource(hsvSource);

			this.queue = queue;

			this.VisionTable = NetworkTable.getTable(properties.getProperty("table", "SmartDashboard"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {

		ArrayList<Rect> bound = new ArrayList<Rect>();
		Mat hsv = new Mat();
		GripPipeline gripPipeline = new GripPipeline();

		double x1 = 0;
		double x2 = 0;
		double y1 = 0;
		double y2 = 0;
		double center_x = 0;
		double center_y = 0;

		Scalar scalarGreen = new Scalar(0, 255, 0);
		Scalar scalarRed = new Scalar(0, 0, 255);
		Scalar scalarBlue = new Scalar(255, 0, 0);

		Point p0 = new Point(0, 0);
		Point p1 = new Point(Width, Height);

		Calendar cal = Calendar.getInstance();
		long time = cal.getTimeInMillis();
		int c=0;

		while(true){
			try {
				MatTime inputImage = this.queue.take();
				Imgproc.cvtColor(inputImage.getMat(), hsv, Imgproc.COLOR_BGR2HSV);//Change from rgb to hsv

				gripPipeline.process(hsv);//take the hsv Mat and pot Threshold, find contours and filter contours on it

				ArrayList<MatOfPoint> contours = gripPipeline.filterContoursOutput(); //Getting the contours after the filter                                             
				bound.clear();//Clear the contours from the last time

				for (MatOfPoint rect : contours) {//print the area of the contours that he fond
					Rect r = Imgproc.boundingRect(rect);
					bound.add(r);
					Imgproc.rectangle(inputImage.getMat(), r.tl(), r.br(), scalarGreen, 3);//printing
				};

				if(bound.size() == 2){//if he find 2 contours

					//taking the left point and the right point of the contour that he fond
					x1 = Math.min(bound.get(0).tl().x, bound.get(1).tl().x);
					x2 = Math.max(bound.get(0).br().x, bound.get(1).br().x);
					y1 = Math.min(bound.get(0).tl().y, bound.get(1).tl().y);
					y2 = Math.max(bound.get(0).br().y, bound.get(1).br().y);

					//get the center of the contours that he fond
					center_x = (x1 + x2)/2;
					center_y = (y1 + y2)/2;

					Imgproc.rectangle(inputImage.getMat(), new Point(x1, y1), new Point(x2, y2),scalarBlue, 3);//print the area from left point and the right point of the contor that he fond
					Imgproc.rectangle(inputImage.getMat(), new Point(center_x, center_y), new Point(center_x, center_y), scalarRed, 3);//print the center

					//VisionTable.putNumber("center x",center_x);//send center_x to the robot
					//VisionTable.putNumber("center y",center_y);//send center_y to the robot

					//VisionTable.putString("TargetInfo", "1|" + center_x + "|" + center_y + "|" + inputImage.getTime());

					//VisionTable.putBoolean("find 2 contours", true);

				}else{//if he find more then 2 contours or lest then 2 contours, print red on the sides of the frame
					Imgproc.rectangle(inputImage.getMat(), p0, p1, scalarRed, 10);//printing red on the sides of the frame 
					//VisionTable.putBoolean("find 2 contours", false);

					//VisionTable.putString("TargetInfo", "0|");
				}

				VisionTable.putString("TargetInfo", center_x + ";" + center_y + ";" + inputImage.getTime());

				//print fps
				if (Calendar.getInstance().getTimeInMillis() - time > 1000) {
					time = Calendar.getInstance().getTimeInMillis();
					System.out.println(Thread.currentThread().getName() + " fps: " + c);
					c = 0;
				}else {
					c++;
				}

				this.imageSource.putFrame(inputImage.getMat());//Presents the frame and what what he detect in port 1185

			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
