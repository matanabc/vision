
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.opencv.core.Mat;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class FrameProducer implements Runnable{

	private static final String FILE_PLACE = "/home/pi/Documents/vision/Java/Values";//fill place

	protected BlockingQueue<MatTime> queue = null;

	//camera object 
	private UsbCamera camera = null;

	// This creates a CvSink for us to use. This grabs images from our selected camera, 
	// and will allow us to use those images in opencv
	CvSink imageSink = new CvSink("CV Image Grabber");
	
	private Properties properties;	
	private NetworkTable VisionTable = null;
	
	private int USBPort;
	private boolean useUSB;
	private String cameraKeyMode;

	public FrameProducer(boolean usbCam, BlockingQueue<MatTime> queue) {
		this.properties = new Properties();
		
		this.useUSB = usbCam;

		try {
			properties.load(new FileInputStream(FILE_PLACE));//fill place to use
			
			this.VisionTable = NetworkTable.getTable(properties.getProperty("table", "SmartDashboard"));
			this.cameraKeyMode = properties.getProperty("cameraKeyMode", "Camere Mode");
			
			setCamera();


			/*
			//creating camera object 
			// is the camera port, Usually the camera will be on device 0
			this.camera = setCamera(Integer.parseInt(properties.getProperty("camera_Id", "0")));

			//sitting the camera:

			//Usually this well be in low Resolution for faster process
			this.camera.setResolution(Integer.parseInt(properties.getProperty("camera_Width", "160"))	,
					Integer.parseInt(properties.getProperty("camera_Height", "120")));

			if(usbCam) {
				//These will be who you want or what the camera can do 
				this.camera.setFPS(Integer.parseInt(properties.getProperty("camera_FPS_Usb", "30")));//fps
				this.camera.setBrightness(Integer.parseInt(properties.getProperty("camera_Brightness_Usb", "0")));//Brightness
				this.camera.setExposureManual(Integer.parseInt(properties.getProperty("camera_Exposure_Usb", "0")));//Exposure
			}else {//piCam
				//These will be who you want or what the camera can do 
				this.camera.setFPS(Integer.parseInt(properties.getProperty("camera_FPS_Pi", "60")));//fps
				this.camera.setBrightness(Integer.parseInt(properties.getProperty("camera_Brightness_Pi", "50")));//Brightness
				this.camera.getProperty("exposure_time_absolute").set(Integer.parseInt(properties.getProperty("camera_Exposure_Pi", "1")));//Exposure
				//System.out.println("exploser is set to - " + camera.getProperty("exposure_time_absolute").get());
			}*/
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*for (VideoProperty prop1 : camera.enumerateProperties()){
		    System.out.println(prop1.getName());
		}*/

		//this.imageSink.setSource(this.camera);
		
		this.queue = queue;
	}

	public void run() {
		
		Mat inputImage = new Mat();
		
		while (true) {
			// Grab a frame. If it has a frame time of 0, there was an error.
			// Just skip and continue
			
			if((int) VisionTable.getNumber(cameraKeyMode, 0) != USBPort) {
				camera.free();
				camera.
				setCamera();
			}
									
			inputImage = new Mat();

			long frameTime = imageSink.grabFrame(inputImage);

			MatTime matTimeToQueue = new MatTime(inputImage, Calendar.getInstance().getTimeInMillis());

			if (frameTime == 0) continue;
			try {
				this.queue.put(matTimeToQueue);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static UsbCamera setCamera(int cameraId) {
		UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
		return camera;
	}
	
	private void setCamera() {
		try {
			properties.load(new FileInputStream(FILE_PLACE));//fill place to use
			
			USBPort = (int) VisionTable.getNumber(cameraKeyMode, 0);
			
			if(USBPort > 0) {
				//creating camera object 
				// is the camera port, Usually the camera will be on device 0
				//this.camera = setCamera(Integer.parseInt(properties.getProperty("camera_Id", "0")));

				//sitting the camera:

				//Usually this well be in low Resolution for faster process
				this.camera = setCamera(USBPort - 1);
				
				this.camera.setResolution(Integer.parseInt(properties.getProperty("camera_Width", "320"))	,
				Integer.parseInt(properties.getProperty("camera_Height", "240")));
				this.camera.setFPS(Integer.parseInt(properties.getProperty("camera_FPS_Usb", "30")));//fps
				
				return;
				
			} else if (USBPort == 0) {
				this.camera = setCamera(USBPort);
				
				this.camera.setResolution(Integer.parseInt(properties.getProperty("camera_Width", "320"))	,
				Integer.parseInt(properties.getProperty("camera_Height", "240")));
				this.camera.setFPS(Integer.parseInt(properties.getProperty("camera_FPS_Usb", "30")));//fps
			}

			if(useUSB) {
				//These will be who you want or what the camera can do 
				this.camera.setBrightness(Integer.parseInt(properties.getProperty("camera_Brightness_Usb", "0")));//Brightness
				this.camera.setExposureManual(Integer.parseInt(properties.getProperty("camera_Exposure_Usb", "0")));//Exposure
			} else {//piCam
				//These will be who you want or what the camera can do 
				this.camera.setFPS(Integer.parseInt(properties.getProperty("camera_FPS_Pi", "60")));//fps
				this.camera.setBrightness(Integer.parseInt(properties.getProperty("camera_Brightness_Pi", "50")));//Brightness
				this.camera.getProperty("exposure_time_absolute").set(Integer.parseInt(properties.getProperty("camera_Exposure_Pi", "1")));//Exposure
				//System.out.println("exploser is set to - " + camera.getProperty("exposure_time_absolute").get());
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*for (VideoProperty prop1 : camera.enumerateProperties()){
		    System.out.println(prop1.getName());
		}*/

		this.imageSink.setSource(this.camera);
	}

}
