package publisher1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.codec.binary.Base64;
import org.apache.kafka.clients.admin.NewTopic;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import publisher1.utils.Utils;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 */
public class FXCameraController {
    // the FXML button
    @FXML
    private Button button;
    // the FXML image view
    @FXML
    private ImageView currentFrame;

    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    //private VideoCapture capture0 = new VideoCapture();
    private VideoCapture videoCapture = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used
    private static int cameraId = 0;
    String topicName = "live";
    NewTopic topicCreated = ProducerVideoMessages.createTopics(topicName);
    int i = 0;

    public FXCameraController() throws IOException {
    }

    /**
     * The action triggered by pushing the button on the GUI
     *
     * @param event the push button event
     */
    @FXML
    protected void startCamera(ActionEvent event) {
        if (!this.cameraActive) {
            System.out.println("camera active? : " + this.cameraActive);
            // start the video capture
            this.videoCapture.open(cameraId);
            // is the video stream available?
            if (this.videoCapture.isOpened()) {
                this.cameraActive = true;
                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        Mat frame = grabFrame();//Grab frame

                        String rawFormat = FXCameraController.matToJson(frame);// Conversion of Mat->JSON->String
                        try {
                            ProducerVideoMessages.sendImagesString(rawFormat, topicName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Image imageToShow = Utils.mat2Image(frame, i++, topicCreated.name());
                        updateImageView(currentFrame, imageToShow);

                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                // update the button content
                this.button.setText("Stop Camera");
            } else {
                // log the error
                System.err.println("Impossible to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.button.setText("Start Camera");
            // stop the timer
            this.stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
    private Mat grabFrame() {
        Mat frame = new Mat();
        if (this.videoCapture.isOpened()) {
            try {
                // read the current frame
                this.videoCapture.read(frame);
                //resize the frame.
                Size resize = new Size(550, 450);
                Mat resizeImage = new Mat();
                // if the frame is not empty, process it
                if (!frame.empty()) {
                    /**
                     * These are different ways to scale down the frames , currently I managed to 1.1 MB to 800KB.
                     * */
                    // Imgproc.resize(frame, resizeImage,resize,(int)(resizeImage.rows()*1.5),(int)(resizeImage.cols()*1.5));
                   // Imgproc.resize(frame, resizeImage, resize, 0, 0, Imgproc.INTER_AREA);
                    Imgproc.resize(frame,resizeImage,resize);
                     return resizeImage;
                 //   return frame;
                }

            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return null;
    }

    /**
     * Conversion of Mat to string
     * Cols, ROws, elemSize , type and add these as a JSON
     * convert mat to byte[]
     * encode the byte[] to string
     * Then converts it to JSON object and return it as a string
     */
    public static String matToJson(Mat mat) {

        JsonObject obj = new JsonObject();

        if (mat.isContinuous()) {
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();
            int type = mat.type();

            obj.addProperty("rows", rows);
            obj.addProperty("cols", cols);
            obj.addProperty("type", type);

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString;
            if (type == CvType.CV_8UC3) {
                byte[] data = new byte[cols * rows * elemSize];
                mat.get(0, 0, data);
                System.out.println("byte array : "+data.length);
                dataString = new String(Base64.encodeBase64(data));
            } else {
                throw new UnsupportedOperationException("unknown type");
            }
            obj.addProperty("data", dataString);

            Gson gson = new Gson();
            String json = gson.toJson(obj);

            return json;
        } else {
            System.out.println("Mat not continuous.");
        }
        return "{}";
    }


    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.videoCapture.isOpened()) {
            // release the camera
            this.videoCapture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    public void setClosed() {
        this.stopAcquisition();
    }

}