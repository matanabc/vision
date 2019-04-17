import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.BlockingQueue;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class FrameConsumer implements Runnable{

	protected BlockingQueue<MatTime> queue = null;

	private static NetworkTable VisionTable = NetworkTable.getTable("SmartDashboard");
	private static NetworkTable VisionDriverTable = NetworkTable.getTable("DriverPanel");
	private static NetworkTable VisionPitTable = NetworkTable.getTable("LiveWindow");

	private static ImageStream stream = new ImageStream(5);

	public FrameConsumer(BlockingQueue<MatTime> queue) {
		this.queue = queue;
	}

	public void run() {

		ArrayList<Rect> bound = new ArrayList<Rect>();
		ArrayList<MatOfPoint> contours;
		Mat hsv = new Mat();
		GripPipeline gripPipeline = new GripPipeline();

		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;

		double[] center_x = {0, 0, 0}, center_y = {0, 0, 0}, target_width = {0, 0, 0};	

		Scalar scalarGreen = new Scalar(0, 255, 0), scalarRed = new Scalar(0, 0, 255), scalarBlue = new Scalar(255, 0, 0);

		Point p0 = new Point(0, 0), p1 = new Point(320, 240);

		Calendar cal = Calendar.getInstance();
		long time = cal.getTimeInMillis();
		int c = 0;

		int countoursRun = 0, numOfPare = 0;

		int[] bigToSmall;

		boolean scale = false;

		ContourPaerHelper helper = new ContourPaerHelper();
		
		MatTime inputImage;
		
		int FPS = 5, timeFromLestFrameInStream;

		while(true){
			try {
				inputImage = this.queue.take();

				Imgproc.cvtColor(inputImage.getMat(), hsv, Imgproc.COLOR_BGR2HSV);//Change from rgb to hsv

				if(VisionPitTable.getBoolean("Test Mode Vision", false)){
					gripPipeline.setValuesPit();
				}

				gripPipeline.process(hsv);//take the hsv Mat and pot Threshold, find contours and filter contours on it

				contours = gripPipeline.filterContoursOutput(); //Getting the contours after the filter                                             
				bound.clear();//Clear the contours from the last time


				for (MatOfPoint rect : contours) {//print the area of the contours that he fond
					Rect r = Imgproc.boundingRect(rect);
					bound.add(r);
					Imgproc.rectangle(inputImage.getMat(), r.tl(), r.br(), scalarGreen, 3);//printing
				};

				/*
					bigToSmall = helper.sortRects(bound);

					if(bigToSmall == null) {

					} else if(bigToSmall.length >= 2) {
						if(bigToSmall[0] - bigToSmall[1] > 0) {
							scale = false;
						} else {
							scale = true;
						}
					}
				 */

				if(bound.size() == 2) {		
					//taking the left point and the right point of the contour that he fond
					x1 = Math.min(bound.get(0).tl().x, bound.get(1).tl().x);
					x2 = Math.max(bound.get(0).br().x, bound.get(1).br().x);
					y1 = Math.min(bound.get(0).tl().y, bound.get(1).tl().y);
					y2 = Math.max(bound.get(0).br().y, bound.get(1).br().y);

					//get the center of the contours that he fond
					center_x[0] = (x1 + x2)/2;
					center_y[0] = (y1 + y2)/2;
					target_width[0] = (x2 - x1);

					Imgproc.rectangle(inputImage.getMat(), new Point(x1, y1), new Point(x2, y2),scalarBlue, 3);//print the area from left point and the right point of the contor that he fond
					Imgproc.rectangle(inputImage.getMat(), new Point(center_x[0], center_y[0]), new Point(center_x[0], center_y[0]), scalarRed, 3);//print the center

					for(int i = 1; i < center_x.length; i++) {
						center_x[i] = center_x[0];
						center_y[i] = center_y[0];
						target_width[i] = (x2 - x1);
					}

					sendValuesToRobot(center_x, center_y, target_width, inputImage.getTime());

				} else if(bound.size() > 2) {
					numOfPare = 0;
					countoursRun = 0;

					while (countoursRun < bound.size() - 1 && numOfPare < 3) {
						//if(helper.rotaitonIsGood(contours.get(countoursRun), contours.get(countoursRun + 1), scale)) {
						if(helper.isPairBySquare(contours.get(countoursRun), contours.get(countoursRun + 1))) {
							//taking the left point and the right point of the contour that he fond
							x1 = Math.min(bound.get(countoursRun).tl().x, bound.get(countoursRun + 1).tl().x);
							x2 = Math.max(bound.get(countoursRun).br().x, bound.get(countoursRun + 1).br().x);
							y1 = Math.min(bound.get(countoursRun).tl().y, bound.get(countoursRun + 1).tl().y);
							y2 = Math.max(bound.get(countoursRun).br().y, bound.get(countoursRun + 1).br().y);

							//get the center of the contours that he fond
							center_x[numOfPare] = (x1 + x2)/2;
							center_y[numOfPare] = (y1 + y2)/2;
							target_width[numOfPare] = (x2 - x1);

							Imgproc.rectangle(inputImage.getMat(), new Point(x1, y1), new Point(x2, y2), scalarBlue, 3);//print the area from left point and the right point of the contor that he fond
							Imgproc.rectangle(inputImage.getMat(), new Point(center_x[numOfPare], center_y[numOfPare]), new Point(center_x[numOfPare], center_y[numOfPare]), scalarRed, 3);//print the center

							numOfPare++;
							countoursRun += 2;

						} else {
							countoursRun += 1;
						}
					}

					for(int i = numOfPare; i < center_x.length; i++) {
						if(i == 0) {
							center_x[i] = center_x[0];
							center_y[i] = center_y[0];
							target_width[i] = target_width[0];
						} else {
							center_x[i] = center_x[i - 1];
							center_y[i] = center_y[i - 1];
							target_width[i] = target_width[i - 1];
						}
					}

					sendValuesToRobot(center_x, center_y, target_width, inputImage.getTime());
				} else {//if he find less then 2 contours, print red on the sides of the frame
					Imgproc.rectangle(inputImage.getMat(), p0, p1, scalarRed, 10);//printing red on the sides of the frame 

					sendValuesToRobot(center_x, center_y, target_width, 0);
				}
				
				stream.putFrame(inputImage.getMat());//Presents the frame and what he detect, put it in port 1185

				//this.imageSource.putFrame(inputImage.getMat());//Presents the frame and what he detect, put it in port 1185

				/*} else {
					MatTime inputImage = this.queue.take();

					this.imageSource.putFrame(inputImage.getMat());//Presents the frame and what he detect, put it in port 1185
				}*/
			}catch (Exception e) {
				e.printStackTrace();

				System.out.println("e class" +  e.getClass());
			}
		}
	}



	public void sendValuesToRobot(double[] x, double[] y, double[] target_w, long time) {

		if(time == 0){
			VisionDriverTable.putBoolean("Vision Detect", false);
		} else {
			VisionDriverTable.putBoolean("Vision Detect", true);
		}

		VisionTable.putString("TargetInfo", time + ";" + x[0] + ";" + y[0] + ";" + x[1] + ";" + y[1] + ";" + x[2] + ";" + y[2]);
	}
}