package camera.guo.com.carcamera.ImageDetect.DetectShapes;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

import camera.guo.com.carcamera.Value.ColorShape;

/**
 * Created by Guo on 2016/8/8.
 */
public class GetShapesColors {

    String shapeList = "";
    public int redCircle = 0;

    public String getColorShapesList(Bitmap current) {

        Mat screen = GetScreen.getScreen(current);

        GetShapes getShape = new GetShapes();

        int reds[], yellows[], greens[], cyans[],
                blues[], Magentas[], blacks[];

        reds = getShape.getScreenShapes(screen, ColorShape.COLOR_RED);
        yellows = getShape.getScreenShapes(screen, ColorShape.COLOR_YELLOW);
        greens = getShape.getScreenShapes(screen, ColorShape.COLOR_GREEN);
        cyans = getShape.getScreenShapes(screen, ColorShape.COLOR_CYAN);
        blues = getShape.getScreenShapes(screen, ColorShape.COLOR_BLUE);
        Magentas = getShape.getScreenShapes(screen, ColorShape.COLOR_MAGENTA);
        blacks = getShape.getScreenShapes(screen, ColorShape.COLOR_BLACK);

        showShapeColorList(reds);
        showShapeColorList(yellows);
        showShapeColorList(greens);
        showShapeColorList(cyans);
        showShapeColorList(blues);
        showShapeColorList(Magentas);
        showShapeColorList(blacks);

        return shapeList;
    }


    /**
     * 输出到控制台
     *
     * @param colorShapeList
     */
    private void showShapeColorList(int[] colorShapeList) {

        String color = "";

        switch (colorShapeList[0]) {
            case ColorShape.COLOR_BLUE:
                color = "蓝色";
                break;
            case ColorShape.COLOR_CYAN:
                color = "青色";
                break;
            case ColorShape.COLOR_GREEN:
                color = "绿色";
                break;
            case ColorShape.COLOR_MAGENTA:
                color = "紫色";
                break;
            case ColorShape.COLOR_RED:
                color = "红色";
                break;
            case ColorShape.COLOR_YELLOW:
                color = "黄色";
                break;
            case ColorShape.COLOR_BLACK:
                color = "黑色";
                break;
        }

        int rectangle, triangle, diamond, star, circle;

        rectangle = 0;
        triangle = 0;
        diamond = 0;
        star = 0;
        circle = 0;

        for (int ix = 1; ix < colorShapeList.length; ix++) {
            switch (colorShapeList[ix]) {
                case ColorShape.SHAPE_CIRCLE:
                    circle++;
                    break;
                case ColorShape.SHAPE_RACTANGLE:
                    rectangle++;
                    break;
                case ColorShape.SHAPE_DIAMOND:
                    diamond++;
                    break;
                case ColorShape.SHAPE_STAR:
                    star++;
                    break;
                case ColorShape.SHAPE_TRIANGLE:
                    triangle++;
                    break;
            }
        }

        addShapes(circle, color, "circle");
        addShapes(diamond, color, "diamond");
        addShapes(rectangle, color, "rectangle");
        addShapes(triangle, color, "triangle");
        addShapes(star, color, "star");
    }

    private void addShapes(int shapeCount, String color, String shape) {

        if (shapeCount > 0) {
            shapeList += shapeCount + "个" + color;

            if (shape.equals("circle")) {
                shapeList += "圆形  ";

                if(color == "红色") {
                    redCircle++;
                }
            }
            else if (shape.equals("rectangle"))
                shapeList += "距形  ";
            else if (shape.equals("diamond"))
                shapeList += "菱形  ";
            else if (shape.equals("star"))
                shapeList += "星形  ";
            else if (shape.equals("triangle"))
                shapeList += "三角形  ";
        }
    }
}
