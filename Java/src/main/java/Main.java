


import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.opencv.core.Core;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Main {

	private static final String FILE_PLACE = "/home/pi/Documents/vision/Java/Values";//fill place
	
	public static void main(String[] args) {
		System.out.println("Starting! ");
		Properties properties = new Properties();

		try {
			properties.load(new FileInputStream(FILE_PLACE));

			// Loads our OpenCV library. This MUST be included
			System.loadLibrary("opencv_java310");
			
			// Connect NetworkTables, and get access to the publishing table
			NetworkTable.setClientMode();
			NetworkTable.setIPAddress(properties.getProperty("roboRioIP", "roborio-3211-FRC.local"));//ip of the robot
			NetworkTable.initialize();
			NetworkTable VisionTable = NetworkTable.getTable(properties.getProperty("table", "SmartDashboard"));

			VisionTable.putBoolean("pi connect", true);//Upload to the robot that he connect to him 

			BlockingQueue<MatTime> queue = new ArrayBlockingQueue<MatTime>(1);//Queue for all the frmes whe collect

			//taking frames from the camera and pot them to the Queue 
			FrameProducer producer = new FrameProducer(true, queue);

			if(VisionTable.getBoolean("Show HSV", false)) {
				//taking the frame from the Queue and do vision processing on them 
				FrameConsumerShowHSV consumer1 = new FrameConsumerShowHSV(queue);
				FrameConsumerShowHSV consumer2 = new FrameConsumerShowHSV(queue);
				
				//making them to work in Thread
				new Thread(producer).start();

				new Thread(consumer1).start();
				new Thread(consumer2).start();
			} else {
				//taking the frame from the Queue and do vision processing on them 
				FrameConsumer consumer1 = new FrameConsumer(queue);
				FrameConsumer consumer2 = new FrameConsumer(queue);
				
				//making them to work in Thread
				new Thread(producer).start();

				new Thread(consumer1).start();
				new Thread(consumer2).start();
			}

		}catch (Exception e) {
			System.out.println("Error: " + e);
		}

	}
}
