package publisher1;

import org.apache.kafka.common.serialization.Serializer;
import org.opencv.core.Mat;

import java.util.Map;

public class FrameSerializer implements Serializer<Frame> {
    private String encoding = "UTF-8";

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, Frame data) {
        byte[] valueOfFrame;
        try{
           Mat temp = data.getFrame();

        }
        catch (Exception e)
        {}
        return new byte[0];
    }

    @Override
    public void close() {

    }
}
