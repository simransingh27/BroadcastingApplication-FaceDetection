package publisher1;

import org.opencv.core.Mat;

public class Frame {
    private Mat frame;

    public Frame(Mat frame) {
        this.frame = frame;
    }

    public Mat getFrame() {
        return frame;
    }
}
