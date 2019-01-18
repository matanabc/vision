

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
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

	private CvSource imageSource = null;
	private MjpegServer cvStream = null;

	private CvSource hsvSource = null;
	private MjpegServer hsvStream = null;

	private double ratioBetweenTwo;
	private final double RATIO_BETWEEN_2_NEED_TO_BE = 50/-20;//(w/h)
	
	private double rotation = 0;

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

			this.hsvSource = new CvSource("CV hsv Source", VideoMode.PixelFormat.kMJPEG, Width, Height, FPS);
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
		ArrayList<MatOfPoint> contours;
		Mat hsv = new Mat();
		GripPipeline gripPipeline = new GripPipeline();

		double x1 = 0;
		double x2 = 0;
		double y1 = 0;
		double y2 = 0;

		double[] center_x = {0, 0, 0};
		double[] center_y = {0, 0, 0};	
		double[] target_width = {0, 0, 0};	

		int numOfPare = 0;

		//double center_x = 0;
		//double center_y = 0;

		Scalar scalarGreen = new Scalar(0, 255, 0);
		Scalar scalarRed = new Scalar(0, 0, 255);
		Scalar scalarBlue = new Scalar(255, 0, 0);

		Point p0 = new Point(0, 0);
		Point p1 = new Point(Width, Height);

		Calendar cal = Calendar.getInstance();
		long time = cal.getTimeInMillis();
		int c=0;

		int countoursRun = 0;

		int[] bigToSmall;
		
		boolean scale = false;

		while(true){
			try {
				MatTime inputImage = this.queue.take();
				Imgproc.cvtColor(inputImage.getMat(), hsv, Imgproc.COLOR_BGR2HSV);//Change from rgb to hsv

				gripPipeline.process(hsv);//take the hsv Mat and pot Threshold, find contours and filter contours on it

				contours = gripPipeline.filterContoursOutput(); //Getting the contours after the filter                                             
				bound.clear();//Clear the contours from the last time


				for (MatOfPoint rect : contours) {//print the area of the contours that he fond
					Rect r = Imgproc.boundingRect(rect);
					bound.add(r);
					Imgproc.rectangle(inputImage.getMat(), r.tl(), r.br(), scalarGreen, 3);//printing
				};

				bigToSmall = sort(bound);

				if(bigToSmall.length >= 2) {
					if(bigToSmall[0] - bigToSmall[1] > 0) {
						scale = false;
					} else {
						scale = true;
					}
				}

				if(bound.size() == 2) {		
					if(rotaitonIsGood(contours.get(0), contours.get(1), scale)) {

						//taking the left point and the right point of the contour that he fond
						x1 = Math.min(bound.get(0).tl().x, bound.get(1).tl().x);
						x2 = Math.max(bound.get(0).br().x, bound.get(1).br().x);
						y1 = Math.min(bound.get(0).tl().y, bound.get(1).tl().y);
						y2 = Math.max(bound.get(0).br().y, bound.get(1).br().y);

						if(ratioIsGood(x1, x2, y1, y2)) {
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
						}
					}
				} else if(bound.size() > 2) {
					numOfPare = 0;
					countoursRun = 0;

					while (countoursRun < bound.size() - 1) {
						if(rotaitonIsGood(contours.get(countoursRun), contours.get(countoursRun + 1), scale)) {

							//taking the left point and the right point of the contour that he fond
							x1 = Math.min(bound.get(countoursRun).tl().x, bound.get(countoursRun + 1).tl().x);
							x2 = Math.max(bound.get(countoursRun).br().x, bound.get(countoursRun + 1).br().x);
							y1 = Math.min(bound.get(countoursRun).tl().y, bound.get(countoursRun + 1).tl().y);
							y2 = Math.max(bound.get(countoursRun).br().y, bound.get(countoursRun + 1).br().y);

							if(ratioIsGood(x1, x2, y1, y2)) {
								//get the center of the contours that he fond
								center_x[numOfPare] = (x1 + x2)/2;
								center_y[numOfPare] = (y1 + y2)/2;
								target_width[numOfPare] = (x2 - x1);

								Imgproc.rectangle(inputImage.getMat(), new Point(x1, y1), new Point(x2, y2),scalarBlue, 3);//print the area from left point and the right point of the contor that he fond
								Imgproc.rectangle(inputImage.getMat(), new Point(center_x[0], center_y[0]), new Point(center_x[0], center_y[0]), scalarRed, 3);//print the center

								numOfPare++;
								countoursRun += 2;
							}
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
				} else {//if he find less then 2 contours, print red on the sides of the frame
					Imgproc.rectangle(inputImage.getMat(), p0, p1, scalarRed, 10);//printing red on the sides of the frame 
				}


				//---------------------------------------------code of 2017-----------------------------------------------
				/*
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

					System.out.println(bound.get(0).tl().x + bound.get(0).width - bound.get(1).tl().x);
					System.out.println(bound.get(0).tl().y - bound.get(0).br().y);

					ratioBetweenTwo = (bound.get(0).tl().x + bound.get(0).width - bound.get(1).tl().x) / (bound.get(0).tl().y - bound.get(0).br().y);
					if(ratioBetweenTwo <= ratioBetweenTwoWanted * 1.1 || ratioBetweenTwo >= ratioBetweenTwoWanted * 0.9) {
						System.out.println("in");
					}

					//VisionTable.putNumber("center x",center_x);//send center_x to the robot
					//VisionTable.putNumber("center y",center_y);//send center_y to the robot

					//VisionTable.putString("TargetInfo", "1|" + center_x + "|" + center_y + "|" + inputImage.getTime());

					//VisionTable.putBoolean("find 2 contours", true);

				}else{//if he find more then 2 contours or lest then 2 contours, print red on the sides of the frame
					Imgproc.rectangle(inputImage.getMat(), p0, p1, scalarRed, 10);//printing red on the sides of the frame 
					//VisionTable.putBoolean("find 2 contours", false);

					//VisionTable.putString("TargetInfo", "0|");
				}*/

				//VisionTable.putString("TargetInfo", center_x + ";" + center_y + ";" + inputImage.getTime());
				//-------------------------------------------------------------------------------------------------------

				sendValuesToRobot(center_x, center_y, target_width, inputImage.getTime());

				//print fps
				if (Calendar.getInstance().getTimeInMillis() - time > 1000) {
					time = Calendar.getInstance().getTimeInMillis();
					System.out.println(Thread.currentThread().getName() + " fps: " + c);
					c = 0;
				} else {
					c++;
				}

				this.imageSource.putFrame(inputImage.getMat());//Presents the frame and what he detect, put it in port 1185

			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public double rotaiton(MatOfPoint2f points, boolean scale) {
		RotatedRect ellipse = Imgproc.fitEllipse(points);

		double rotation = ellipse.angle;
		
		if(scale) {
			rotation = -1 * (rotation - 180);
		} 

		return rotation;
	}

	public boolean ratioIsGood(double x1, double x2, double y1, double y2) {
		ratioBetweenTwo = (x2 - x1) / (y2 - y1);

		return true;//ratioBetweenTwo <= RATIO_BETWEEN_2_NEED_TO_BE * 1.1 || ratioBetweenTwo >= RATIO_BETWEEN_2_NEED_TO_BE * 0.9;
	}

	public boolean rotaitonIsGood(MatOfPoint rightContourPoints, MatOfPoint leftContourPoints, boolean scale) {

		if(rightContourPoints.toArray().length < 5 || leftContourPoints.toArray().length < 5) {
			return false;
		}

		//System.out.println("Right = " + rotaiton(new MatOfPoint2f(rightContourPoints.toArray()), rightContourPoints.width(), rightContourPoints.height()) + " left = " + rotaiton(new MatOfPoint2f(leftContourPoints.toArray()), leftContourPoints.width(), leftContourPoints.height()));

		return rotaiton(new MatOfPoint2f(rightContourPoints.toArray()), scale) > 90
				&& rotaiton(new MatOfPoint2f(leftContourPoints.toArray()), scale) < 90;

		//return rotaiton(new MatOfPoint2f(rightContourPoints.toArray())) > 90 && rotaiton(new MatOfPoint2f(leftContourPoints.toArray())) < 90;
	}

	public void sendValuesToRobot(double[] x, double[] y, double[] target_w, long time) {
		VisionTable.putString("TargetInfo", time + ";" + x[0] + ";" + y[0] + ";" + x[1] + ";" + y[1] + ";" + x[2] + ";" + y[2]);
	}

	public int[] sort(ArrayList<Rect> a) {

		if(a.size() > 0 ) {
			int[] smallXToBigX = new int[a.size()];
			int[] p = new int[a.size()];

			for(int i = 0; i < a.size(); i++) {
				p[i] = a.get(i).x;
			}

			int minValue = a.get(0).x;
			int minPlace = 0;

			for(int i = 0; i < p.length; i++) {
				for(int j = 0; j < p.length; j++) {
					if(p[j] < minValue) {
						minPlace = j;
						minValue = p[j];
					}
				}
				minValue = 600;
				smallXToBigX[i] = minPlace;
				p[minPlace] = 600;
			}

			return smallXToBigX;
		}

		return null;
	}
}

