import java.util.ArrayList;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class ContourPaerHelper {
	
	//rotaiton variable:
	private double rotation;
	
	//sort rects variable:
	private int[] smallXToBigX, p; 
	private int minPlace = 0;
	private double minValue;
	
	// match pair by Square:
	private double[] leftYPoints = new double[4], leftXPoints = new double[4], rightYPoints = new double[4], rightXPoints = new double[4];
	private int[] leftXSort, leftYSort, rightXSort,rightYSort;
	private Point[] leftPointSort, rightPointSort;
	
	//sort points:
	private int[] smallToBig;
	private Point[] sort;

	
	public double rotaiton(MatOfPoint2f points, boolean scale) {
		//RotatedRect ellipse = Imgproc.fitEllipse(points);

		rotation = Imgproc.fitEllipse(points).angle;

		if(scale) {
			rotation = -1 * (rotation - 180);
		} 

		return rotation;
	}

	public boolean rotaitonIsGood(MatOfPoint rightContourPoints, MatOfPoint leftContourPoints, boolean scale) {

		if(rightContourPoints.toArray().length < 5 || leftContourPoints.toArray().length < 5) {
			return false;
		}

		//System.out.println("Right = " + rotaiton(new MatOfPoint2f(rightContourPoints.toArray()), rightContourPoints.width(), rightContourPoints.height()) + " left = " + rotaiton(new MatOfPoint2f(leftContourPoints.toArray()), leftContourPoints.width(), leftContourPoints.height()));

		return rotaiton(new MatOfPoint2f(rightContourPoints.toArray()), scale) > 90
				&& rotaiton(new MatOfPoint2f(leftContourPoints.toArray()), scale) < 90;

		//return rotaiton(new MatOfPoint2f(rightContourPoints.toArray()), scale) > rotaiton(new MatOfPoint2f(leftContourPoints.toArray()), scale);

		//return rotaiton(new MatOfPoint2f(rightContourPoints.toArray())) > 90 && rotaiton(new MatOfPoint2f(leftContourPoints.toArray())) < 90;
	}

	public int[] sortRects(ArrayList<Rect> a) {

		if(a.size() > 0 ) {
			smallXToBigX = new int[a.size()];
			p = new int[a.size()];

			for(int i = 0; i < a.size(); i++) {
				p[i] = a.get(i).x;
			}

			minValue = a.get(0).x;
			minPlace = 0;

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

	
	/**
	 * @return true if the square between the to contours the up base small or equal to down base
	 * */
	public boolean isPairBySquare(MatOfPoint rightContourPoints, MatOfPoint leftContourPoints){

		if(rightContourPoints.toArray().length < 5 || leftContourPoints.toArray().length < 5) {
			return false;
		}
		
		Point leftPoints[] = new Point[4]; 
		Imgproc.fitEllipse(new MatOfPoint2f(leftContourPoints.toArray())).points(leftPoints);
		
		Point rightPoints[] = new Point[4]; 
		Imgproc.fitEllipse(new MatOfPoint2f(rightContourPoints.toArray())).points(rightPoints);

		/*RotatedRect ellipseLeft = Imgproc.fitEllipse(new MatOfPoint2f(leftContourPoints.toArray()));
		Point leftPoints[] = new Point[4]; 
		ellipseLeft.points(leftPoints);
		
		RotatedRect ellipseRight = Imgproc.fitEllipse(new MatOfPoint2f(rightContourPoints.toArray()));
		Point rightPoints[] = new Point[4]; 
		ellipseRight.points(rightPoints);*/

		
		
		/* -------------------
		 * sort y place left 
		 * sort x place left 
		 * sort y place right 
		 * sort x place right
		 * -------------------*/

		/*
		double[] leftYPoints = new double[4];
		double[] leftXPoints = new double[4];
		double[] rightYPoints = new double[4];
		double[] rightXPoints = new double[4];*/

		for(int i = 0; i < 4; i++){
			leftXPoints[i] = leftPoints[i].x;
			leftYPoints[i] = leftPoints[i].y;
			rightXPoints[i] = rightPoints[i].x;
			rightYPoints[i] = rightPoints[i].y;
		}

		leftXSort = sortFromSmallToBig(leftXPoints);
		leftYSort = sortFromSmallToBig(leftYPoints);
		rightXSort = sortFromSmallToBig(rightXPoints);
		rightYSort = sortFromSmallToBig(rightYPoints);

		//sort point left and right 
		
		/*
		 * Point[0] - right up
		 * Point[1] - right down
		 * Point[2] - left down
		 * Point[3] - right up
		 */
		
		leftPointSort = sortPoints(leftPoints, leftYSort, leftXSort);
		rightPointSort = sortPoints(rightPoints, rightYSort, rightXSort);
		
		//System.out.println((rightPointSort[3].x - leftPointSort[0].x) + " - " + (rightPointSort[2].x - leftPointSort[1].x));

		return rightPointSort[3].x - leftPointSort[0].x < rightPointSort[2].x - leftPointSort[1].x;
	}


	private int[] sortFromSmallToBig(double[] points){

		smallToBig = new int[points.length];

		minValue = points[0];
		minPlace = 0;

		for(int i = 0; i < points.length; i++) {
			for(int j = 0; j < points.length; j++) {
				if(points[j] < minValue) {
					minPlace = j;
					minValue = points[j];
				}
			}
			minValue = 600;
			smallToBig[i] = minPlace;
			points[minPlace] = 600;
		}

		return smallToBig;
	}

	private Point[] sortPoints(Point[] points, int[] smallToBigY, int[] smallToBigX){
		sort = new Point[4];

		//right up
		if(points[smallToBigY[0]].x > points[smallToBigY[1]].x){
			sort[0] = points[smallToBigY[0]];
		} else {
			sort[0] = points[smallToBigY[1]];
		}

		//right down
		if(points[smallToBigY[2]].x > points[smallToBigY[3]].x){
			sort[1] = points[smallToBigY[2]];
		} else {
			sort[1] = points[smallToBigY[3]];
		}

		//left down
		if(points[smallToBigY[2]].x < points[smallToBigY[3]].x){
			sort[2] = points[smallToBigY[2]];
		} else {
			sort[2] = points[smallToBigY[3]];
		}

		//left up
		if(points[smallToBigY[0]].x < points[smallToBigY[1]].x){
			sort[3] = points[smallToBigY[0]];
		} else {
			sort[3] = points[smallToBigY[1]];
		}

		return sort;
	}
}
