package camera.guo.com.carcamera.Value;

import java.util.ResourceBundle;

/**
 * Created by Guo on 2016/8/14.
 */
public class CommandCode {

    public static final byte[] CAR_HEAD = {(byte) 0x55, (byte) 0xaa};
    public static final byte CAR_END = (byte) 0xbb;

    //平台运动指令
    public static final byte CAR_STOP = (byte) 0x01;
    public static final byte CAR_AHEAD = (byte) 0x02;
    public static final byte CAR_BACK = (byte) 0x03;
    public static final byte CAR_LEFT = (byte) 0x04;
    public static final byte CAR_RIGHT = (byte) 0x05;
    public static final byte CAR_TRACK = (byte) 0x06;
    public static final byte CAR_BEEP = (byte) 0x30;

    //摄像头运动步数
    public static final int CAMERA_ONE_STEP = 1;
    public static final int CAMERA_TWO_STEP = 2;

    //摄像头控制指令
    public static final int CAMERA_UP = 0;
    public static final int CAMERA_DOWN = 2;
    public static final int CAMERA_LEFT = 4;
    public static final int CAMERA_RIGHT = 6;

    public static final int CAMERA_INIT = 25;

    //摄像头预设位调用与设置指令
    public static final int CAMERA_SET_POS_1 = 32;
    public static final int CAMERA_GET_POS_1 = 33;

    public static final int CAMERA_SET_POS_2 = 34;
    public static final int CAMERA_GET_POS_2 = 35;

    public static final int CAMERA_SET_POS_3 = 36;
    public static final int CAMERA_GET_POS_3 = 37;

    public static final int CAMERA_SET_POS_4 = 38;
    public static final int CAMERA_GET_POS_4 = 39;

    public static final int RECEIVING_IMAGE = 11;

    public static final int RECEIVING_CAR_DATA = 12;

    //摄像头和平台控制指令flag
    public static final int CAMERA_CONTROL = 50;
    public static final int CAR_CONTROL = 60;
    public static final int DECODE_QR_COMPLETE = 70;
    public static final int DETECT_SHAPE_COMPLETE = 80;

    public static final  int DETECT_ARROW_COMPLETE = 90;
    public static final  int DETECT_CAR_NUMBER_COMPLETE = 100;

}
