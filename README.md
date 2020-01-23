# Live-video-Broadcasting-Application-Analytics

This project is built to serve data collection layer.It comprised of OpenCV to fetch the live video from Webcam(or USB camera) and decode the video
into frames. It is integratd with kafka Producer. 

Below the screenshot that shows the approach to build real-time analytics application using Kafka.

![publisher](https://user-images.githubusercontent.com/40739676/72947220-3f444f80-3d79-11ea-84bb-405902b0cefc.PNG)

As you can see in the screenshot, this application is build with OpenCV to fetch live videos and render it on the video container. Also,it has Kafka producer instance which publishes these frames to Kafka.
In this transformation of Mat(Frame) to raw format is different from Live video broadcasting application. The Face detection learning algo utilizes Mat to perform analytics on frames.
Unlike , Live video streaming where frames are transformed to Image and then serialize as Byte Arrays to the Kafka , This project transforms the Mat to JSON and then send it to Kafka as a String.
Kafka serializes string into raw format.  

