package camera.guo.com.carcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bkrcl.control_car_video.camerautil.CameraCommandUtil;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import camera.guo.com.carcamera.Client.SocketBuild;
import camera.guo.com.carcamera.Client.TransCommand;
import camera.guo.com.carcamera.ImageDetect.DecodeQr.DetectQr;
import camera.guo.com.carcamera.ImageDetect.DetectArrow.AnalysisArrow;
import camera.guo.com.carcamera.ImageDetect.DetectNumber.DetectNumber;
import camera.guo.com.carcamera.ImageDetect.DetectShapes.GetShapesColors;
import camera.guo.com.carcamera.Service.SearchService;
import camera.guo.com.carcamera.Tools.Convert;
import camera.guo.com.carcamera.Tools.FileTool;
import camera.guo.com.carcamera.Value.ColorShape;
import camera.guo.com.carcamera.Value.CommandCode;

public class CarActivity extends AppCompatActivity {
    // 广播名称
    public static final String A_S = "com.a_s";
    private static int initFlag = 0;
    final String TAG = "opencv";
    //net
    WifiManager wifiManager;
    //Handler carHandler;
    DhcpInfo dhcpInfo;
    Bitmap curImage;
    CameraHandler cameraHandler;
    //camera
    CameraCommandUtil cameraCommandUtil;
    TransCommand transCommand;
    SocketBuild socketBuild;
    int stepSpeed = 0, stepLength = 0;
    String QrResult;
    ImageView cameraView;
    /**
     * carHandler
     * <p/>
     * this handler belong to main thread
     */
    Handler carHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /**
             * 方案1 接收指令
             * 1. 测距  0x01
             * 2. 光档  0x02
             * 3. 扫二维码 0x03
             * 4. 检测图形 0x04
             * 5. 车牌号识别 0x05
             * 6. 路标识别  0x06
             */

            if (msg.what == CommandCode.RECEIVING_CAR_DATA) {

                byte[] reciveData = (byte[]) msg.obj;

                byte order = reciveData[3];

                //扫二维码
                if (order == 0x03) {

                    makeMsg("开始识别二维码");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            QrResult = null;
                            DetectQr detectQr = new DetectQr();
                            int turnCount = 0;
                            int or = CommandCode.CAMERA_LEFT;
                            while (QrResult == null) {
                                ctrlCamera(or, 2);
                                QrResult = detectQr.decode(curImage);
                                waitMs(1000);
                                turnCount++;
                                if (turnCount > 15) {
                                    or = CommandCode.CAMERA_RIGHT;
                                    turnCount = 0;
                                }
                            }
                            Message message = Message.obtain(carHandler, CommandCode.DECODE_QR_COMPLETE,
                                    QrResult);
                            message.sendToTarget();
                            ctrlCamera(CommandCode.CAMERA_GET_POS_1, 0);
                        }
                    }).start();

                }
                //识别图形
                if (order == 0x04) {
                    makeMsg("开始识别图形");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            GetShapesColors getShapesColors = new GetShapesColors();
                            String colorShapes = getShapesColors.getColorShapesList(curImage);
                            int redCircle = getShapesColors.redCircle;

                            //send_voice(colorShapes);
                            Message message = Message.obtain(carHandler, CommandCode.DETECT_SHAPE_COMPLETE,
                                    redCircle, 0, colorShapes);
                            message.sendToTarget();
                        }
                    }).start();
                }
                //发送车牌号
                if (order == 0x05) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String numbers = rectcharacter();

                            byte[] car1 = {0x55, (byte) 0xaa, (byte) 0x11, (byte) 0x11, (byte) 0x11,
                                    0x11, 0x00, (byte) 0xbb};
                            transCommand.send(car1);
                            byte[] car2 = {0x55, (byte) 0xaa, 0x11, (byte) 0x11, 0x00, 0x00, 0x00, (byte) 0xbb};
                            transCommand.send(car2);

                            Message msg = Message.obtain(carHandler, CommandCode.DETECT_CAR_NUMBER_COMPLETE,
                                    numbers);
                            msg.sendToTarget();
                        }
                    }).start();
                }
                //识别箭头
                if (order == 0x06) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String arrow = "";

                            AnalysisArrow analysisArrow = new AnalysisArrow();

                            int arrowColor = analysisArrow.getColor(curImage);

                            if (arrowColor == 1) {

                                int ort = analysisArrow.getOrt();

                                if (ort == ColorShape.LEFT) {
                                    arrow = "禁止左转";

                                    byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x05, 0x00, (byte) 0xbb};
                                    transCommand.send(commands);

                                } else if (ort == ColorShape.RIGHT) {
                                    arrow = "禁止右转";

                                    byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x04, 0x00, (byte) 0xbb};
                                    transCommand.send(commands);

                                }

                            }
                            if (arrowColor == 2) {

                                int ort = analysisArrow.getOrt();

                                if (ort == ColorShape.LEFT) {
                                    arrow = "左转";

                                    byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x04, 0x00, (byte) 0xbb};
                                    transCommand.send(commands);

                                } else if (ort == ColorShape.RIGHT) {
                                    arrow = "右转";

                                    byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x05, 0x00, (byte) 0xbb};
                                    transCommand.send(commands);

                                } else {
                                    arrow = "掉头";

                                    byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x06, 0x00, (byte) 0xbb};
                                    transCommand.send(commands);
                                }
                            }
                            if (arrowColor == 0) {
                                arrow = "没发现箭头";

                                //默认左转
                                byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x04, 0x00, (byte) 0xbb};
                                transCommand.send(commands);
                            }

                            //send_voice(arrow);

                            Message msg = Message.obtain(carHandler, CommandCode.DETECT_ARROW_COMPLETE, arrow);
                            msg.sendToTarget();
                        }
                    }).start();
                }
            }


            //follow is button listener
            if (msg.what == CommandCode.DECODE_QR_COMPLETE) {
                detectResult.setText(msg.obj.toString());

                makeMsg("二维码识别完成");

                byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x01, 0x00, (byte) 0xbb};
                transCommand.send(commands);
            }

            if (msg.what == CommandCode.DETECT_SHAPE_COMPLETE) {
                detectResult.setText(msg.obj.toString());

                byte[] commands = {0x55, (byte) 0xaa, 0x00, 0x00, 0x00, 0x01, 0x00, (byte) 0xbb};
                transCommand.send(commands);

                if (msg.arg1 > 0) {
                    makeMsg(msg.arg1 + "个红色圆形");
                } else {
                    makeMsg("图中没有红色圆形");
                }
            }
            if (msg.what == CommandCode.DETECT_ARROW_COMPLETE) {
                detectResult.setText(msg.obj.toString());

                makeMsg("箭头识别完成");
            }
            if (msg.what == CommandCode.DETECT_CAR_NUMBER_COMPLETE) {
                detectResult.setText("牌号：" + msg.obj.toString());
            }

            if (msg.what == CommandCode.RECEIVING_IMAGE) {
                cameraView.setImageBitmap(curImage);
            }
        }
    };

    public String rectcharacter() {
        DetectNumber detectNumber = new DetectNumber();
        Mat plate = detectNumber.cutPlate(curImage);

        String numbers = "";

        if (plate != null) {
            //车牌转为hsv space
            Mat hsv = new Mat(plate.size(), CvType.CV_8UC3);
            Imgproc.cvtColor(plate, hsv, Imgproc.COLOR_BGR2HSV);

            //提取文字
            Mat words = new Mat(plate.size(), CvType.CV_8UC3);
            Core.inRange(hsv, new Scalar(0, 0, 140), new Scalar(180, 100, 255), words);

            //腐蚀掉无用信息
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4));
            Imgproc.erode(words, words, kernel);

            //查找轮廓
            Mat tmp = words.clone();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            Imgproc.findContours(tmp, contours, new Mat(), Imgproc.RETR_EXTERNAL, 2);

            List<rectChar> rectChars = new ArrayList<rectChar>();

            if (contours.size() > 0) {
                //过掉太小矩形
                for (int i = 0; i < contours.size(); i++) {

                    Rect rect = Imgproc.boundingRect(contours.get(i));

                    System.out.println("area:" + rect.area());

                    if (rect.area() < 500)
                        continue;

                    //过滤最左边的“国”字
                    if (rect.x < 50)
                        continue;

                    //识别每个矩形里面的文字
                    Mat character = new Mat(words, rect);

                    Imgproc.cvtColor(character, character, Imgproc.COLOR_GRAY2BGR);
                    Bitmap characterBitmap = Convert.matToBitmap(character);

                    String chars = OCRDetect("eng", characterBitmap);

                    rectChar rc = new rectChar(rect, chars);
                    rectChars.add(rc);
                }

                for (int cols = 0; cols < plate.cols(); cols++) {
                    for (int i = 0; i < rectChars.size(); i++) {
                        if (rectChars.get(i).rect.x == cols) {
                            numbers += rectChars.get(i).chars;
                        }
                    }
                }
            }

            return textErrorRecovery(numbers);

        } else {
            numbers = "未发现车牌";
        }

        return numbers;
    }

    //纠正车牌号码
    private String textErrorRecovery(String src) {
        String numbers = "";

        if (src.length() <= 6) {
            if (src.charAt(0) == '2')
                numbers += 'Z';
            else if (src.charAt(0) == '6')
                numbers += 'G';
            else
                numbers += src.charAt(0);

            if (src.charAt(1) == 'Z') {
                numbers += '2';
            } else if (src.charAt(1) == 'G') {
                numbers += '6';
            } else {
                numbers += src.charAt(1);
            }

            if (src.charAt(2) == 'Z') {
                numbers += '2';
            } else if (src.charAt(2) == 'G') {
                numbers += '6';
            } else {
                numbers += src.charAt(2);
            }

            if (src.charAt(3) == 'Z') {
                numbers += '2';
            } else if (src.charAt(3) == 'G') {
                numbers += '6';
            } else {
                numbers += src.charAt(3);
            }

            if (src.charAt(4) == '2')
                numbers += 'Z';
            else if (src.charAt(4) == '6')
                numbers += 'G';
            else
                numbers += src.charAt(4);


            if (src.charAt(5) == 'Z') {
                numbers += '2';
            } else if (src.charAt(5) == 'G') {
                numbers += '6';
            } else {
                numbers += src.charAt(5);
            }
        }

        return numbers;
    }

    class rectChar {
        Rect rect;
        String chars;

        public rectChar(Rect rect, String s) {
            this.chars = s;
            this.rect = rect;
        }
    }

    TextView carStatus, detectResult, length;
    Button carTrack, carBeep, carAhead, carBack, carLeft, carRight, carStop;
    EditText carMoveLength, carMoveSpeed;
    Button detectShape, detectQrCode, detectArrow, detectCarNumber;
    Button cameraUp, cameraDown, cameraLeft, cameraRight, cameraInit;
    Button cameraSetPos1, cameraGetPos1, cameraSetPos2, cameraGetPos2, cameraSetPos3, cameraGetPos3, cameraSetPos4, cameraGetPos4;
    Button carStart, getPhoto;
    private String IPCamera = "bkrcjk.eicp.net:88";

    Thread phThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                // 得到当前摄像头的图片信息
                curImage = cameraCommandUtil.httpForImage(IPCamera);
                carHandler.sendEmptyMessage(CommandCode.RECEIVING_IMAGE);
            }
        }
    });
    private String carIp;
    // 广播接收器接受SearchService搜索的摄像头IP地址加端口
    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context arg0, Intent arg1) {
            IPCamera = arg1.getStringExtra("IP");
            phThread.start();
        }
    };
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //...
                    onRun();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transCommand = new TransCommand();
        cameraCommandUtil = new CameraCommandUtil();

        //注册广播接收器
        registerBroad();

        //获取服务端Ip地址
        carIp = getIpAddress();

        //建立套接字
        buildSocket();

        //搜索摄像头
        searchService();

        //创建摄像头HANDLER
        cameraHandler = createCameraHandler();

        //初始化控件
        findViews();
    }

    /**
     * 搜索摄像头
     */
    private void searchService() {
        Intent intent = new Intent();
        intent.setClass(CarActivity.this, SearchService.class);
        startService(intent);
    }

    /**
     * 创建摄像头HANDLER
     */
    public CameraHandler createCameraHandler() {
        HandlerThread handlerThread = new HandlerThread("controlCamera");
        //handlerThread必须在将Looper添加进Handler之前执行start方法，否则会为null
        handlerThread.start();
        return new CameraHandler(handlerThread.getLooper());
    }

    /**
     * 建立套接字
     */
    protected void buildSocket() {
        socketBuild = new SocketBuild(carHandler, carIp, 60000);
    }

    public String getIpAddress() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        dhcpInfo = wifiManager.getDhcpInfo();
        return Formatter.formatIpAddress(dhcpInfo.gateway);
    }

    /**
     * 注册广播接收器
     */
    public void registerBroad() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(A_S);
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    private void onRun() {
        initFlag = 1;
    }

    public String OCRDetect(String language, Bitmap plate) {

        TessBaseAPI baseApi = new TessBaseAPI();

        baseApi.init(FileTool.getSDPath(), language);

        Bitmap bitmap = plate.copy(Bitmap.Config.ARGB_8888, true);

        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();
        baseApi.end();

        return text;
    }

    /**
     * transmit Message to CameraHandler and control camera
     * command -- orientation ,to arg1
     * step -- setp's，to arg2
     *
     * @param command
     * @param step
     */
    private void ctrlCamera(int command, int step) {
        Message msg = Message.obtain(cameraHandler, CommandCode.CAMERA_CONTROL,
                command, step);
        msg.sendToTarget();
    }

    public void send_voice(String str) {
        try {
            byte[] text = transCommand.bytesend(str.getBytes("GBK"));
            System.out.println("text length:" + text.length);
            transCommand.voice(text);
        } catch (UnsupportedEncodingException usee) {

        }
    }

    private void makeMsg(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void waitMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void disStatus(byte[] carDat) {

        int runStatu = 0;
        int psStatus = 0;
        int UltraSonic = 0;
        int Light = 0;
        int CodedDisk = 0;

        psStatus = carDat[3] & 0xff;

        UltraSonic = carDat[5] & 0xff;
        UltraSonic = UltraSonic << 8;
        UltraSonic += carDat[4] & 0xff;

        Light = carDat[7] & 0xff;
        Light = Light << 8;
        Light += carDat[6] & 0xff;

        CodedDisk = carDat[9] & 0xff;
        CodedDisk = CodedDisk << 8;
        CodedDisk += carDat[8] & 0xff;

        runStatu = carDat[2];


        carStatus.setText(" 超声波：" + UltraSonic
                + "\nmm 光照：" + Light + "lx" + "\n码盘：" + CodedDisk
                + "\n光敏状态：" + psStatus + "\n状态：" + runStatu);
    }

    private void findViews() {

        carStart = (Button) findViewById(R.id.start);

        cameraView = (ImageView) findViewById(R.id.cameraFrame);
        carStatus = (TextView) findViewById(R.id.car_status);
        detectResult = (TextView) findViewById(R.id.detect_results);
        length = (TextView) findViewById(R.id.length);

        carTrack = (Button) findViewById(R.id.car_track);
        carBeep = (Button) findViewById(R.id.car_beep);
        carBack = (Button) findViewById(R.id.car_back);
        carAhead = (Button) findViewById(R.id.car_go);
        carLeft = (Button) findViewById(R.id.car_left);
        carRight = (Button) findViewById(R.id.car_right);
        carStop = (Button) findViewById(R.id.car_stop);

        carMoveLength = (EditText) findViewById(R.id.length);
        carMoveSpeed = (EditText) findViewById(R.id.speed);

        detectShape = (Button) findViewById(R.id.detect_shape);
        detectQrCode = (Button) findViewById(R.id.detect_qrcode);
        detectArrow = (Button) findViewById(R.id.detect_arrow);
        detectCarNumber = (Button) findViewById(R.id.detect_carNumber);

        cameraUp = (Button) findViewById(R.id.camera_up);
        cameraDown = (Button) findViewById(R.id.camera_down);
        cameraLeft = (Button) findViewById(R.id.camera_left);
        cameraRight = (Button) findViewById(R.id.camera_right);
        cameraInit = (Button) findViewById(R.id.camera_init);

        cameraGetPos1 = (Button) findViewById(R.id.get_pos_1);
        cameraSetPos1 = (Button) findViewById(R.id.set_pos_1);
        cameraGetPos2 = (Button) findViewById(R.id.get_pos_2);
        cameraSetPos2 = (Button) findViewById(R.id.set_pos_2);
        cameraGetPos3 = (Button) findViewById(R.id.get_pos_3);
        cameraSetPos3 = (Button) findViewById(R.id.set_pos_3);
        cameraGetPos4 = (Button) findViewById(R.id.get_pos_4);
        cameraSetPos4 = (Button) findViewById(R.id.set_pos_4);

        getPhoto = (Button) findViewById(R.id.get_photo);

        ClickListener clickListener = new ClickListener();

        /**
         * platform control
         */
        carTrack.setOnClickListener(clickListener);
        carBeep.setOnClickListener(clickListener);
        carBack.setOnClickListener(clickListener);
        carAhead.setOnClickListener(clickListener);
        carLeft.setOnClickListener(clickListener);
        carRight.setOnClickListener(clickListener);
        carStop.setOnClickListener(clickListener);

        /**
         * image analysis
         */
        detectShape.setOnClickListener(clickListener);
        detectQrCode.setOnClickListener(clickListener);
        detectArrow.setOnClickListener(clickListener);
        detectCarNumber.setOnClickListener(clickListener);

        /**
         * camera control
         */
        cameraUp.setOnClickListener(clickListener);
        cameraDown.setOnClickListener(clickListener);
        cameraLeft.setOnClickListener(clickListener);
        cameraRight.setOnClickListener(clickListener);
        cameraInit.setOnClickListener(clickListener);

        /**
         * init position camera
         */
        cameraGetPos1.setOnClickListener(clickListener);
        cameraSetPos1.setOnClickListener(clickListener);
        cameraGetPos2.setOnClickListener(clickListener);
        cameraSetPos2.setOnClickListener(clickListener);
        cameraGetPos3.setOnClickListener(clickListener);
        cameraSetPos3.setOnClickListener(clickListener);
        cameraGetPos4.setOnClickListener(clickListener);
        cameraSetPos4.setOnClickListener(clickListener);


        getPhoto.setOnClickListener(clickListener);
    }

    private void getSpeed() {
        String speed = carMoveSpeed.getText().toString();
        stepSpeed = Integer.parseInt(speed);
    }

    private void getLength() {
        String length = carMoveLength.getText().toString();
        stepLength = Integer.parseInt(length);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    /**
     * camera Handler , belong to the Main thread is not
     */
    class CameraHandler extends Handler {

        public CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CommandCode.CAMERA_CONTROL)
                cameraCommandUtil.postHttp(IPCamera, msg.arg1, msg.arg2);
        }
    }

    /**
     * listener button
     */
    class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            getLength();
            getSpeed();

            switch (v.getId()) {
                /**
                 *      control camera
                 */
                case R.id.camera_up:
                    ctrlCamera(CommandCode.CAMERA_UP, 1);
                    break;
                case R.id.camera_down:
                    ctrlCamera(CommandCode.CAMERA_DOWN, 1);
                    break;
                case R.id.camera_left:
                    ctrlCamera(CommandCode.CAMERA_LEFT, 1);
                    break;
                case R.id.camera_right:
                    ctrlCamera(CommandCode.CAMERA_RIGHT, 1);
                    break;
                case R.id.camera_init:
                    ctrlCamera(CommandCode.CAMERA_INIT, 0);
                    break;
                case R.id.set_pos_1:
                    ctrlCamera(CommandCode.CAMERA_SET_POS_1, 0);
                    break;
                case R.id.get_pos_1:
                    ctrlCamera(CommandCode.CAMERA_GET_POS_1, 0);
                    break;
                case R.id.set_pos_2:
                    ctrlCamera(CommandCode.CAMERA_SET_POS_2, 0);
                    break;
                case R.id.get_pos_2:
                    ctrlCamera(CommandCode.CAMERA_GET_POS_2, 0);
                    break;
                case R.id.set_pos_3:
                    ctrlCamera(CommandCode.CAMERA_SET_POS_3, 0);
                    break;
                case R.id.get_pos_3:
                    ctrlCamera(CommandCode.CAMERA_GET_POS_3, 0);
                    break;
                case R.id.set_pos_4:
                    ctrlCamera(CommandCode.CAMERA_SET_POS_4, 0);
                    break;
                case R.id.get_pos_4:
                    ctrlCamera(CommandCode.CAMERA_GET_POS_4, 0);
                    break;

                /**
                 * analysis image
                 */

                //analysis shapes and colors
                case R.id.detect_shape:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            GetShapesColors getShapesColors = new GetShapesColors();
                            String colorShapes = getShapesColors.getColorShapesList(curImage);
                            send_voice(colorShapes);
                            Message message = Message.obtain(carHandler, CommandCode.DETECT_SHAPE_COMPLETE,
                                    colorShapes);
                            message.sendToTarget();
                        }
                    }).start();

                    break;

                //qr code decode
                case R.id.detect_qrcode:
                    Toast.makeText(getApplicationContext(), "Decodeing QrCode...", Toast.LENGTH_SHORT).show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DetectQr detectQr = new DetectQr();

                            String QrResult = detectQr.decode(curImage);

                            if (QrResult != null) {
                                Message message = Message.obtain(carHandler, CommandCode.DECODE_QR_COMPLETE,
                                        QrResult);
                                message.sendToTarget();
                            } else {
                                Message message = Message.obtain(carHandler, CommandCode.DECODE_QR_COMPLETE,
                                        "");
                                message.sendToTarget();
                            }
                        }
                    }).start();

                    break;

                //arrow detect
                case R.id.detect_arrow:

                    String arrow = "";

                    final AnalysisArrow analysisArrow = new AnalysisArrow();

                    final int arrowColor = analysisArrow.getColor(curImage);

                    if (arrowColor == 1) {

                        int ort = analysisArrow.getOrt();

                        if (ort == ColorShape.LEFT) {
                            arrow = "右转";
                        } else if (ort == ColorShape.RIGHT) {
                            arrow = "左转";
                        }

                    }
                    if (arrowColor == 2) {

                        int ort = analysisArrow.getOrt();

                        if (ort == ColorShape.LEFT) {
                            arrow = "左转";
                        } else if (ort == ColorShape.RIGHT) {
                            arrow = "右转";
                        } else {
                            arrow = "掉头";
                        }

                    }
                    if (arrowColor == 0) {
                        arrow = "没有箭头";
                    }
                    send_voice(arrow);

                    Message msg = Message.obtain(carHandler, CommandCode.DETECT_ARROW_COMPLETE, arrow);
                    msg.sendToTarget();

                    break;

                //car plate
                case R.id.detect_carNumber:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            String str = rectcharacter();

                            Message msg = Message.obtain(carHandler, CommandCode.DETECT_CAR_NUMBER_COMPLETE,
                                    str);
                            msg.sendToTarget();
                        }
                    }).start();

                    break;
                /**
                 * platform control
                 */
                case R.id.start:
                    //平台启动
                    transCommand.start();
                    break;

                case R.id.car_go:
                    transCommand.carGo(stepSpeed, stepLength);
                    break;
                case R.id.car_back:
                    transCommand.carBack(stepSpeed, stepLength);
                    break;
                case R.id.car_left:
                    transCommand.carLeft(stepSpeed);
                    break;
                case R.id.car_right:
                    transCommand.carRight(stepSpeed);
                    break;
                case R.id.car_stop:
                    transCommand.carStop();
                    break;
                case R.id.car_track:
                    transCommand.carTrack(stepSpeed);
                    break;
                case R.id.car_beep:
                    transCommand.carBeep();
                    break;

                case R.id.get_photo:
                    //获取当前时间
                    SimpleDateFormat formatter = new SimpleDateFormat("HHmmss");
                    Date curDate = new Date(System.currentTimeMillis());
                    String fileName = formatter.format(curDate);

                    FileTool.saveBitmap(fileName + ".jpg", curImage);
            }
        }
    }
}