package camera.guo.com.carcamera.ImageDetect.DetectShapes;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Guo on 2016/8/5.
 */
public class Filter {
    // Filter black
    public Mat getBlack(Mat screen) {
        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(0, 0, 0), new Scalar(180, 255, 110), mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter white
    public Mat getWhite(Mat screen) {
        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(0, 0, 125), new Scalar(180, 255, 255), mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(5, 5));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter red
    public Mat getRed(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat matH = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(156, 50, 50), new Scalar(180, 255, 255),
                matH);

        Mat matL = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(0, 50, 50), new Scalar(10, 255, 255), matL);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);

        Core.addWeighted(matH, 100, matL, 100, 0, mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter Yellow
    public Mat getYellow(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(26, 50, 50), new Scalar(41, 255, 255), mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter Green
    public Mat getGreen(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(42, 50, 50), new Scalar(77, 255, 255), mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter Cyan
    public Mat getcYan(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(78, 50, 100), new Scalar(99, 255, 255),
                mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter Blue
    public Mat getBlue(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(100, 200, 101), new Scalar(124, 255, 255),
                mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // Filter Magenta
    public Mat getMagenta(Mat screen) {

        Mat hsv = cvtHSV(screen);

        Mat mat = new Mat(screen.size(), CvType.CV_8U);
        Core.inRange(hsv, new Scalar(125, 50, 50), new Scalar(155, 255, 255),
                mat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(3, 3));

        Imgproc.erode(mat, mat, kernel);

        return mat;
    }

    // BGR convert to HSV
    private static Mat cvtHSV(Mat bgrImg) {
        Mat hsv = new Mat(bgrImg.size(), CvType.CV_8UC3);

        Imgproc.cvtColor(bgrImg, hsv, Imgproc.COLOR_BGR2HSV);

        return hsv;
    }
}