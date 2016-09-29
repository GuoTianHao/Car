package camera.guo.com.carcamera.ImageDetect.DetectShapes;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import camera.guo.com.carcamera.Value.ColorShape;
import camera.guo.com.carcamera.Tools.FileTool;

/**
 * Created by Guo on 2016/8/5.
 */
public class GetShapes {

    int color;

    static int c = 1;

    public int[] getScreenShapes(Mat screen, int color) {
        Filter filter = new Filter();

        //flag this mat color
        this.color = color;

        Mat mat = new Mat(screen.size(), CvType.CV_8U);

        String fileName = FileTool.getSDPath();

        switch (this.color) {
            case ColorShape.COLOR_RED:
                mat = filter.getRed(screen);
                Highgui.imwrite(fileName + "/red.jpg", mat);
                break;
            case ColorShape.COLOR_YELLOW:
                mat = filter.getYellow(screen);
                Highgui.imwrite(fileName + "/yellow.jpg", mat);
                break;
            case ColorShape.COLOR_GREEN:
                mat = filter.getGreen(screen);
                Highgui.imwrite(fileName + "/green.jpg", mat);
                break;
            case ColorShape.COLOR_CYAN:
                mat = filter.getcYan(screen);
                Highgui.imwrite(fileName + "/cyan.jpg", mat);
                break;
            case ColorShape.COLOR_BLUE:
                mat = filter.getBlue(screen);
                Highgui.imwrite(fileName + "/blue.jpg", mat);
                break;
            case ColorShape.COLOR_MAGENTA:
                mat = filter.getMagenta(screen);
                Highgui.imwrite(fileName + "/magenta.jpg", mat);
                break;
            case ColorShape.COLOR_BLACK:
                mat = filter.getBlack(screen);
                Highgui.imwrite(fileName + "/black.jpg", mat);
                break;
            default:
                System.out.println("没有这个颜色!");
                break;
        }

        return analysisShapes(mat);
    }

    private int[] analysisShapes(Mat src) {
        // Find contours
        Mat tmp = src.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(tmp, contours, new Mat(), 2, 2);

        int rectangle = 0, triangle = 0, diamond = 0, star = 0, circle = 0;

        int[] colorShapeList = new int[contours.size() + 1];

        //index 0 remember color information
        colorShapeList[0] = color;

        int contoursCount = 1;

        int len = contours.size();

        for (int i = 0; i < len; i++) {
            MatOfPoint contour = contours.get(i);

            int area = (int) Imgproc.contourArea(contour);

            System.out.print("area: " + area + ",");

            //过滤小色块
            Rect rect = Imgproc.boundingRect(contour);

            System.out.print(rect);

            //不符合规则则不进行判断
            if (area < 400 || rect.height < 20 || rect.width < 20 || rect.width > src.width() / 3 ||
                    rect.height > src.height() / 3) {
                continue;
            }

            //求角个数
            MatOfPoint2f m2f = new MatOfPoint2f();
            double length = Imgproc.arcLength(new MatOfPoint2f(contour
                    .toArray()), true);
            Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()),
                    m2f, length * 0.03, true);
            int cvs = m2f.rows();

            //System.out.println("面积: " + area + "角个数: " + cvs);

            //分析角
            if (cvs == 3) {
                colorShapeList[contoursCount] = ColorShape.SHAPE_TRIANGLE;
            }
            if (cvs == 4) {
                colorShapeList[contoursCount] = ColorShape.SHAPE_RACTANGLE;

                //逼近的面积
                int area1 = (int) (Imgproc.contourArea(m2f));

                //求最小矩形面积
                MatOfPoint2f m2f1 = new MatOfPoint2f(contour.toArray());
                RotatedRect rotatedRect = Imgproc.minAreaRect(m2f1);

                Point[] pt = new Point[4];
                rotatedRect.points(pt);

                MatOfPoint minRect = new MatOfPoint(pt);

                int area2 = (int) (Imgproc.contourArea(minRect));

                if (Math.abs(area1 - area2) > 500) {
                    colorShapeList[contoursCount] = ColorShape.SHAPE_DIAMOND;
                }
            }
            if (cvs > 4 && cvs <= 9) {
                colorShapeList[contoursCount] = ColorShape.SHAPE_CIRCLE;
            }
            if (cvs == 10) {
                colorShapeList[contoursCount] = ColorShape.SHAPE_STAR;
            }

            contoursCount++;
        }

        return colorShapeList;
    }
}
