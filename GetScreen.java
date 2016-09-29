package camera.guo.com.carcamera.ImageDetect.DetectShapes;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import camera.guo.com.carcamera.Tools.Convert;

import android.graphics.Bitmap;

/**
 * Created by Guo on 2016/9/1.
 */
public class GetScreen {

    public static  Mat getScreen(Bitmap bitmap) {

        Mat current = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);

        current = Convert.bitmapToMat(bitmap, current);

        Size size = current.size();

        // Convert BGR to HSV
        Mat hsv = new Mat(size, CvType.CV_8UC3);
        Imgproc.cvtColor(current, hsv, Imgproc.COLOR_BGR2HSV);

        // Filter screen（color white）
        Mat white = new Mat(size, CvType.CV_8U);
        Core.inRange(hsv, new Scalar(0, 0, 130), new Scalar(180, 240, 255),
                white);

        // Close
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(10, 10));

        Imgproc.erode(white, white, kernel);
        Imgproc.dilate(white, white, kernel);

        // Find largest contour
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat tmp = white;
        Imgproc.findContours(tmp, contours, new Mat(), 2, 2);

        int maxArea = 0;
        MatOfPoint maxContour = contours.get(0);

        // Find largest
        for (int i = 0; i < contours.size(); i++) {
            int a = (int) Imgproc.contourArea(contours.get(i));

            if (a > maxArea) {
                maxArea = a;
                maxContour = contours.get(i);
            }
        }

        // Bounding largest contours
        Rect boundRect = new Rect();
        boundRect = Imgproc.boundingRect(maxContour);

        Mat screen = new Mat(current, boundRect);

        return screen;
    }
}
