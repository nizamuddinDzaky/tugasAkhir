package com.example.mamorasoft.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    QuadrilateralSelectionImageView mSelectionImageView;
    Button mButton, btn_load;

    Bitmap mBitmap;
    Bitmap bitmap_edge, bitmap_greyScale, bitmap_contour, bitmap_threshold;
    Bitmap mResult;

    ImageView image_contour,image_greyscale,image_edge, image_threshold,image_result;

    MaterialDialog mResultDialog;
    ImageView imageCamera;
    private static final int MAX_HEIGHT = 500;


    private int PICK_IMAGE_REQUEST = 1;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image_greyscale=(ImageView)findViewById(R.id.image_greyscale);
        image_edge=(ImageView)findViewById(R.id.image_edge);
        image_contour=(ImageView)findViewById(R.id.image_contour);
        image_threshold=(ImageView)findViewById(R.id.image_threshold);
        image_result=(ImageView)findViewById(R.id.image_result);

        mSelectionImageView = (QuadrilateralSelectionImageView) findViewById(R.id.polygonView);
        mButton = (Button) findViewById(R.id.button);
        btn_load = (Button) findViewById(R.id.btn_load);

        btn_load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);
            }
        });
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<PointF> points = mSelectionImageView.getPoints();

                if (mBitmap != null) {
                    Mat orig = new Mat();
                    Mat orig2 = new Mat();
                    org.opencv.android.Utils.bitmapToMat(mBitmap, orig);
                    org.opencv.android.Utils.bitmapToMat(bitmap_threshold, orig2);

                    Log.i("UkuranBitmap", "awal: "+orig.size());
                    Log.i("UkuranBitmap", "akhir: "+orig2.size());

                    Mat transformed = perspectiveTransform(orig, points);
                    mResult = applyThreshold(transformed);

//                    if (mResultDialog.getCustomView() != null) {
//                        PhotoView photoView = (PhotoView) mResultDialog.getCustomView().findViewById(R.id.imageView);
//                        photoView.setImageBitmap(mResult);
//                        mResultDialog.show();
                        image_result.setImageBitmap(mResult);
//                    }

                    orig.release();
                    transformed.release();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_gallery) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);
        } else if (id == R.id.action_camera) {
            // TODO Camera
            Toast.makeText(getApplicationContext(), "To Do", Toast.LENGTH_LONG).show();
//            Toast.makeText().show();
//            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(takePictureIntent, 1);
//            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    Uri uri = data.getData();

                    try {
                        mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                image_ori.setImageBitmap(getResizedBitmap(mBitmap, MAX_HEIGHT));
                        mSelectionImageView.setImageBitmap(getResizedBitmap(mBitmap, MAX_HEIGHT));
                        List<PointF> points = findPoints();
                        mSelectionImageView.setPoints(points);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
//           Ambil Dari Kamera
//            case 1:
//                if(resultCode == RESULT_OK){
//                    Bundle extras = data.getExtras();
//                    mBitmap= (Bitmap) extras.get("data");
////            imageCamera .setImageBitmap(mBitmap);
//                    mSelectionImageView.setImageBitmap(getResizedBitmap(mBitmap, MAX_HEIGHT));
//                    List<PointF> points = findPoints();
//                    mSelectionImageView.setPoints(points);
//                }
//                break;
        }
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//
//        }
    }

    /**
     * Resize a given bitmap to scale using the given height
     *
     * @return The resized bitmap
     */
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxHeight) {
        double ratio = bitmap.getHeight() / (double) maxHeight;
        int width = (int) (bitmap.getWidth() / ratio);
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false);
    }

    /**
     * Attempt to find the four corner points for the largest contour in the image.
     *
     * @return A list of points, or null if a valid rectangle cannot be found.
     */
    private List<PointF> findPoints() {
        List<PointF> result = null;

        Mat image = new Mat();
        Mat orig = new Mat();

        org.opencv.android.Utils.bitmapToMat(getResizedBitmap(mBitmap, MAX_HEIGHT), image);
        org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

        Mat greyScale = new Mat();
        Mat edges = new Mat();
        Mat threshold = new Mat();

        bitmap_greyScale= Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
        bitmap_edge = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
        bitmap_threshold = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);


//        Log.i("TypeImage", "findPoints: "+image.type());

//        GreyScale
        Imgproc.cvtColor(image, greyScale, Imgproc.COLOR_RGB2GRAY);
//        Log.i("TypeImage", "findPoints: "+greyScale.type());
        Utils.matToBitmap(greyScale, bitmap_greyScale);

        image_greyscale.setImageBitmap(bitmap_greyScale);

        Imgproc.GaussianBlur(greyScale, greyScale, new Size(3, 3), 0);
        Imgproc.Canny(greyScale, edges, 80, 240);//75, 200
        Utils.matToBitmap(edges, bitmap_edge);
        image_edge.setImageBitmap(bitmap_edge);

        Imgproc.threshold(edges, threshold,100, 150, Imgproc.ADAPTIVE_THRESH_MEAN_C);
        Utils.matToBitmap(threshold, bitmap_threshold);
        image_threshold.setImageBitmap(bitmap_threshold);
//
//        Imgproc.threshold(threshold, threshold,100, 150, Imgproc.ADAPTIVE_THRESH_MEAN_C);
//
//        Imgproc.GaussianBlur(threshold, threshold, new Size(5, 5), 0);
//        Utils.matToBitmap(threshold, bitmap_threshold);
//        image_threshold.setImageBitmap(bitmap_threshold);
////        Imgproc.dilate(threshold, threshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
//
//        Imgproc.Canny(threshold, edges, 80, 240);//75, 200
//
//
//        Utils.matToBitmap(edges, bitmap_edge);
//
//        image_edge.setImageBitmap(bitmap_edge);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(threshold, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(image, contours, contourIdx, new Scalar(255, 255, 255), 1);
        }

        bitmap_contour = Bitmap.createBitmap(image.cols(),image.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap_contour);
        image_contour.setImageBitmap(bitmap_contour);

////
        MatOfPoint2f largest = findLargestContour(contours);
        if (largest != null) {
            Point[] points = sortPoints(largest.toArray());
            result = new ArrayList<>();
            result.add(new PointF(Double.valueOf(points[0].x).floatValue(), Double.valueOf(points[0].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[1].x).floatValue(), Double.valueOf(points[1].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[2].x).floatValue(), Double.valueOf(points[2].y).floatValue()));
            result.add(new PointF(Double.valueOf(points[3].x).floatValue(), Double.valueOf(points[3].y).floatValue()));
            largest.release();
        } else {
//            Timber.d("Can't find rectangle!");
        }
        return result;
    }

    /**
     * Detect the edges in the given Mat
     * @param src A valid Mat object
     * @return A Mat processed to find edges
     */
    private Mat edgeDetection(Mat src) {
        Mat edges = new Mat();
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 80, 240);
        return edges;
    }

    /**
     * Find the largest 4 point contour in the given Mat.
     *
     //     * @param src A valid Mat
     * @return The largest contour as a Mat
     */
    private MatOfPoint2f findLargestContour(List<MatOfPoint> contours) {
//        List<MatOfPoint> contours = new ArrayList<>();
//        Imgproc.findContours(src, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

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

    /**
     * Transform the coordinates on the given Mat to correct the perspective.
     *
     * @param src A valid Mat
     * @param points A list of coordinates from the given Mat to adjust the perspective
     * @return A perspective transformed Mat
     */
    private Mat perspectiveTransform(Mat src, List<PointF> points) {
        Point point1 = new Point(points.get(0).x, points.get(0).y);
        Point point2 = new Point(points.get(1).x, points.get(1).y);
        Point point3 = new Point(points.get(2).x, points.get(2).y);
        Point point4 = new Point(points.get(3).x, points.get(3).y);
        Point[] pts = {point1, point2, point3, point4};
        return fourPointTransform(src, sortPoints(pts));
    }

    /**
     * Apply a threshold to give the "scanned" look
     *
     * NOTE:
     * See the following link for more info http://docs.opencv.org/3.1.0/d7/d4d/tutorial_py_thresholding.html#gsc.tab=0
     * @param src A valid Mat
     * @return The processed Bitmap
     */
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

    /**
     * Sort the points
     *
     * The order of the points after sorting:
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src The points to sort
     * @return An array of sorted points
     */
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

    /**
     * NOTE:
     * Based off of http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
     *
     * @param src
     * @param pts
     * @return
     */
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
}
