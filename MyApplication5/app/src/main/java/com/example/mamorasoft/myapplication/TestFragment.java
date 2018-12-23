package com.example.mamorasoft.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.kimkevin.cachepot.CachePot;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class TestFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private static final int RESULT_LOAD_IMAGE_TEST = 2;
    private static final int RESULT_LOAD_CAMERA_IMAGE_TEST = 3;
    private static final int MAX_HEIGHT = 500;

    private Bitmap bitmapTest, bitmapBlackWhiteTemplate, bitmapBlackWhiteTest, bitmapResult;

    private ImageView imageViewTest;
    private Button btnLoadTest, btnCameraTest, btnBlackWhite, btnTemplateMatching,btn_intent;

    private QuadrilateralSelectionImageView selectionImageView;

    public TestFragment() {
    }

    public static TestFragment newInstance(String param1, String param2) {
        TestFragment fragment = new TestFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.loadLibrary("opencv_java3");
        try {
            bitmapResult = CachePot.getInstance().pop("bitmapSelected");
            imageViewTest.setImageBitmap(bitmapResult);
        }catch (Exception e){ }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewRoot = inflater.inflate(R.layout.fragment_test, container, false);

        imageViewTest = (ImageView) viewRoot.findViewById(R.id.iv_testImage);
        btnLoadTest = (Button) viewRoot.findViewById(R.id.btn_loadTest);
        btnCameraTest = (Button) viewRoot.findViewById(R.id.btn_loadTest_camera);
        btn_intent= (Button) viewRoot.findViewById(R.id.btn_intent);
        btnBlackWhite = (Button) viewRoot.findViewById(R.id.btn_blackWhiteTest);
        btnTemplateMatching = (Button) viewRoot.findViewById(R.id.btn_templateMatching);

        btnLoadTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentLoadTest = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentLoadTest, RESULT_LOAD_IMAGE_TEST);
            }
        });

        btnCameraTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera, RESULT_LOAD_CAMERA_IMAGE_TEST);
            }
        });

        btn_intent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toMain = new Intent(getActivity(), MainActivity.class);
                startActivity(toMain);
            }
        });

        btnBlackWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmapBlackWhiteTest = blackWhite(bitmapTest);
                imageViewTest.setImageBitmap(bitmapBlackWhiteTest);
            }
        });

        btnTemplateMatching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageViewTest.setImageBitmap(bitmapResult);
//                bitmapBlackWhiteTemplate = CachePot.getInstance().pop(Bitmap.class);
//                templatwMatching();
            }
        });

        return viewRoot;
    }

    private List<PointF> findPoints() {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();
//        org.opencv.android.Utils.bitmapToMat(getResizedBitmap(mBitmap, MAX_HEIGHT), image);
//        org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

        Mat edges = edgeDetection(image);
        MatOfPoint2f largest = findLargestContour(edges);

        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
            largest.release();
        } else {
        }

        edges.release();
        image.release();
        orig.release();

        return result;
    }

    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 75, 200);
        return edges;
    }

    private MatOfPoint2f findLargestContour(Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get the 5 largest contours
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                double area1 = Imgproc.contourArea(o1);
                double area2 = Imgproc.contourArea(o2);
                return (int) (area2 - area1);
            }
        });
        if (contours.size() > 5) contours.subList(4, contours.size() - 1).clear();

        MatOfPoint2f largest = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f approx = new MatOfPoint2f();
            MatOfPoint2f c = new MatOfPoint2f();
            contour.convertTo(c, CvType.CV_32FC2);
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true);

            if (approx.total() == 4 && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx;
                break;
            }
        }

        return largest;
    }

    private Mat perspectiveTransform(Mat src, List<PointF> points) {
        Point point1 = new Point(points.get(0).x, points.get(0).y);
        Point point2 = new Point(points.get(1).x, points.get(1).y);
        Point point3 = new Point(points.get(2).x, points.get(2).y);
        Point point4 = new Point(points.get(3).x, points.get(3).y);
        Point[] pts = {point1, point2, point3, point4};
        return fourPointTransform(src, sortPoints(pts));
    }

    private Bitmap applyThreshold(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

        // Some other approaches
//        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
//        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Imgproc.GaussianBlur(src, src, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(src, bm);

        return bm;
    }

    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };
        Comparator<Point> differenceComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        result[0] = Collections.min(srcPoints, sumComparator);        // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator);        // Lower right has the maximal sum
        result[1] = Collections.min(srcPoints, differenceComparator); // Upper right has the minimal difference
        result[3] = Collections.max(srcPoints, differenceComparator); // Lower left has the maximal difference

        return result;
    }

    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / (double) MAX_HEIGHT;

        Point ul = pts[0];
        Point ur = pts[1];
        Point lr = pts[2];
        Point ll = pts[3];

        double widthA = Math.sqrt(Math.pow(lr.x - ll.x, 2) + Math.pow(lr.y - ll.y, 2));
        double widthB = Math.sqrt(Math.pow(ur.x - ul.x, 2) + Math.pow(ur.y - ul.y, 2));
        double maxWidth = Math.max(widthA, widthB) * ratio;

        double heightA = Math.sqrt(Math.pow(ur.x - lr.x, 2) + Math.pow(ur.y - lr.y, 2));
        double heightB = Math.sqrt(Math.pow(ul.x - ll.x, 2) + Math.pow(ul.y - ll.y, 2));
        double maxHeight = Math.max(heightA, heightB) * ratio;

        Mat resultMat = new Mat(Double.valueOf(maxHeight).intValue(), Double.valueOf(maxWidth).intValue(), CvType.CV_8UC4);

        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        srcMat.put(0, 0, ul.x * ratio, ul.y * ratio, ur.x * ratio, ur.y * ratio, lr.x * ratio, lr.y * ratio, ll.x * ratio, ll.y * ratio);
        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight);

        Mat M = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, resultMat, M, resultMat.size());

        srcMat.release();
        dstMat.release();
        M.release();

        return resultMat;
    }

    public void templatwMatching(){
        Rect roiTest, roiTemplate;

        List<Mat> listMatPiecesTest = new ArrayList<>();
        List<Mat> listMatPiecesTemplate = new ArrayList<>();

        List<Bitmap> listBitmapPiecesTest = new ArrayList<>();
        List<Bitmap> listBitmapPiecesTemplate = new ArrayList<>();

        List<Rect> rectListTemplate = new ArrayList<>();
        List<Rect> rectListTest = new ArrayList<>();

        Mat matTest = new Mat();
        Mat matTemplate = new Mat();

        Utils.bitmapToMat(bitmapBlackWhiteTest, matTest);
        Utils.bitmapToMat(bitmapBlackWhiteTemplate, matTemplate);

        int xTest = matTest.cols()/(bitmapBlackWhiteTest.getWidth()/10);
        int yTest = matTest.rows()/(bitmapBlackWhiteTest.getHeight()/10);

        int xTemplate = matTemplate.cols()/(bitmapBlackWhiteTemplate.getWidth()/10);
        int yTemplate = matTemplate.rows()/(bitmapBlackWhiteTemplate.getHeight()/10);

        for (int i =0;i<bitmapBlackWhiteTest.getWidth()/10;i++){
            for (int j=0;j<bitmapBlackWhiteTest.getHeight()/10;j++){
                roiTest = new Rect(new Point(i*xTest, j*yTest), new Point((i+1)*xTest, (j+1)*yTest));
                Mat submatTest = matTest.submat(roiTest);
                listMatPiecesTest.add(submatTest);

                rectListTest.add(roiTest);

                Mat imageTest = new Mat(roiTest.size(), CvType.CV_8UC1);
                Bitmap newBitmap = Bitmap.createBitmap(imageTest.cols(), imageTest.rows(), Bitmap.Config.RGB_565);

                listBitmapPiecesTest.add(newBitmap);
            }
        }

        for (int i =0;i<bitmapBlackWhiteTemplate.getWidth()/10;i++){
            for (int j=0;j<bitmapBlackWhiteTemplate.getHeight()/10;j++){
                roiTemplate = new Rect(new Point(i*xTemplate, j*yTemplate), new Point((i+1)*xTemplate, (j+1)*yTemplate));
                Mat submatTemplate = matTemplate.submat(roiTemplate);

                rectListTemplate.add(roiTemplate);
                listMatPiecesTemplate.add(submatTemplate);

                Mat imageTemplate = new Mat(roiTemplate.size(), CvType.CV_8UC1);
                Bitmap newBitmap = Bitmap.createBitmap(imageTemplate.cols(), imageTemplate.rows(), Bitmap.Config.RGB_565);

                listBitmapPiecesTemplate.add(newBitmap);
            }
        }

        for (int i =0;i<listMatPiecesTest.size();i++){
            Utils.matToBitmap(listMatPiecesTest.get(i), listBitmapPiecesTest.get(i));
        }

        for (int i =0;i<listMatPiecesTemplate.size();i++){
            Utils.matToBitmap(listMatPiecesTemplate.get(i), listBitmapPiecesTemplate.get(i));
        }

        int jumlahHitamHorizontal =0, jumlahHitamVerikal =0;
        List<Integer> gabungan = new ArrayList<>();
        List<List<Integer>> gabunganPiecesTest = new ArrayList<>();
        List<List<Integer>> gabunganPiecesTemplate = new ArrayList<>();

        for (int n =0;n<listMatPiecesTest.size();n++){

            //get horizontal
            for (int i =0;i<listMatPiecesTest.get(n).cols();i++){
                for (int j=0;j<listMatPiecesTest.get(n).rows();j++){
                    if (Color.blue(listBitmapPiecesTest.get(n).getPixel(i,j)) == 0){
                        jumlahHitamHorizontal++;
                    }
                }
                gabungan.add(jumlahHitamHorizontal);
                jumlahHitamHorizontal=0;
            }

            //get vertikal
            for (int i =0;i<listMatPiecesTest.get(n).cols();i++){
                for (int j=0;j<listMatPiecesTest.get(n).rows();j++){
                    if (Color.blue(listBitmapPiecesTest.get(n).getPixel(j,i)) == 0){
                        jumlahHitamVerikal++;
                    }

                }
                gabungan.add(jumlahHitamVerikal);
                jumlahHitamVerikal=0;
            }
            gabunganPiecesTest.add(new ArrayList<Integer>(gabungan));
            gabungan.clear();
        }

        gabungan.clear(); jumlahHitamHorizontal =0; jumlahHitamVerikal=0;

        for (int n=0;n<listMatPiecesTemplate.size();n++){

            for (int i=0;i<listMatPiecesTemplate.get(n).cols();i++){
                for (int j=0;j<listMatPiecesTemplate.get(n).rows();j++){
                    if (Color.blue(listBitmapPiecesTemplate.get(n).getPixel(i,j)) == 0){
                        jumlahHitamHorizontal++;
                    }
                }
                gabungan.add(jumlahHitamHorizontal);
                jumlahHitamHorizontal=0;
            }

            for (int i =0;i<listMatPiecesTemplate.get(n).cols();i++){
                for (int j=0;j<listMatPiecesTemplate.get(n).rows();j++){
                    if (Color.blue(listBitmapPiecesTemplate.get(n).getPixel(j,i))==0){
                        jumlahHitamVerikal++;
                    }
                }
                gabungan.add(jumlahHitamVerikal);
                jumlahHitamVerikal=0;
            }
            gabunganPiecesTemplate.add(new ArrayList<Integer>(gabungan));
            gabungan.clear();
        }


        /**
         * Cosine
         * */

        List<Double> hasilTemplateMatching = new ArrayList<>();
        double sumTempAtas=0, sumpTempBawah=0;
        double sumKuadratValueTest=0, sumKuadratValueTemplate =0;
        for (int i =0;i<gabunganPiecesTemplate.size();i++){
            for (int j=0;j<gabunganPiecesTemplate.get(i).size();j++){
                sumTempAtas += (gabunganPiecesTest.get(i).get(j)*gabunganPiecesTemplate.get(i).get(j));
                sumKuadratValueTest += Math.pow(gabunganPiecesTest.get(i).get(j), 2);
                sumKuadratValueTemplate += Math.pow(gabunganPiecesTemplate.get(i).get(j),2);
            }
            sumpTempBawah = Math.sqrt(sumKuadratValueTemplate) * Math.sqrt(sumKuadratValueTest);
            if (sumpTempBawah!=0){
                hasilTemplateMatching.add(i, sumTempAtas/sumpTempBawah);
            }else if(sumpTempBawah == 0 && sumTempAtas==0){
                hasilTemplateMatching.add(i, 1.00);
            }
            sumTempAtas=0;
            sumKuadratValueTemplate=0;
            sumKuadratValueTest=0;
        }

        for (int i =0;i<rectListTest.size();i++){
            if (hasilTemplateMatching.get(i) < 0.99){
                Utils.bitmapToMat(bitmapBlackWhiteTest, matTest);

                Imgproc.rectangle(matTest, new Point(rectListTest.get(i-2).x, rectListTest.get(i-1).y), new Point(rectListTest.get(i-2).x+30,rectListTest.get(i-1).y+30), new Scalar(0,0,255), 1);

                Utils.matToBitmap(matTest, bitmapBlackWhiteTest);
            }
        }
    }

    public static Bitmap blackWhite(Bitmap src){

        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap bitmapOutput = Bitmap.createBitmap(width, height, src.getConfig());

        int  R, G, B;
        int pixel;

        for (int x = 0; x<width;x++){
            for (int y=0;y<height;y++){

                pixel = src.getPixel(x,y);

                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);

                int gray = (int) (R + G + B)/3;
                if (gray>=127) gray = 255; else gray = 0;

                bitmapOutput.setPixel(x, y, Color.rgb(gray,gray,gray));
            }
        }

        return bitmapOutput;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE_TEST){
            if (resultCode == RESULT_OK && data != null){
                Uri imageUri = data.getData();

                try {
                    bitmapTest = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    bitmapTest = getResizedBitmap(bitmapTest);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                imageViewTest.setImageBitmap(bitmapTest);
            }
        } else if (requestCode == RESULT_LOAD_CAMERA_IMAGE_TEST){
            if (resultCode == RESULT_OK && data != null){
                Bitmap bmp = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();

                Bitmap bitmapCamera = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                imageViewTest.setImageBitmap(bitmapCamera);
                CachePot.getInstance().push("bitmapCamera", bitmapCamera);
                startActivity(new Intent(getActivity(), MainActivity.class));
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 200, 300, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
