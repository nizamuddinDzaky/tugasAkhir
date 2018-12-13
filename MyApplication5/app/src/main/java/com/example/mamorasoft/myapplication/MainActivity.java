package com.example.mamorasoft.myapplication;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 0;
    private static final int RESULT_LOAD_IMAGE_TEMPLATE = 1;
    private static final int RESULT_LOAD_IMAGE_TEST = 2;

    Button btnLoadTemplate, btnLoadTest, btnTemplateMatching;
    ImageView imageViewTemplate, imageViewTest;

    Bitmap bitmapTest, bitmapTemplate;
    static Bitmap bitmapOutputTest, bitmapOutputTemplate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.loadLibrary("opencv_java3");

        iniViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }
    }

    private void iniViews() {
        btnLoadTemplate = (Button) findViewById(R.id.btn_loadTemplate);
        btnLoadTest = (Button) findViewById(R.id.btn_loadTest);
        btnTemplateMatching = (Button) findViewById(R.id.btn_templateMatching);

        imageViewTemplate = (ImageView) findViewById(R.id.iv_templateImage);
        imageViewTest = (ImageView) findViewById(R.id.iv_testImage);
    }

    public void loadTemplateImage(View view) {

        Intent intentLoadTemplate = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentLoadTemplate, RESULT_LOAD_IMAGE_TEMPLATE);
    }

    public void loadTestImage(View view) {

        Intent intentLoadTest = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intentLoadTest, RESULT_LOAD_IMAGE_TEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RESULT_LOAD_IMAGE_TEMPLATE:
                if (resultCode == RESULT_OK && data != null){
                    Uri imageUri = data.getData();

                    try {
                        bitmapTemplate = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imageViewTemplate.setImageBitmap(bitmapTemplate);
                }
                break;
            case RESULT_LOAD_IMAGE_TEST:
                if (resultCode == RESULT_OK && data != null){
                    Uri imageUri = data.getData();

                    try {
                        bitmapTest = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imageViewTest.setImageBitmap(bitmapTest);
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

    public void convertToBlackWhite(View view) {

        bitmapOutputTest = blackWhite(bitmapTest);
        bitmapOutputTemplate = blackWhite(bitmapTemplate);

        imageViewTemplate.setImageBitmap(bitmapOutputTemplate);
        imageViewTest.setImageBitmap(bitmapOutputTest);
    }


    // 0 -> hitam , 255 putih
    public void templateMatchingListener(View view) {

        Rect roiTest, roiTemplate;

        List<Mat> listMatPiecesTest = new ArrayList<>();
        List<Mat> listMatPiecesTemplate = new ArrayList<>();

        List<Bitmap> listBitmapPiecesTest = new ArrayList<>();
        List<Bitmap> listBitmapPiecesTemplate = new ArrayList<>();

        List<Rect> rectListTemplate = new ArrayList<>();
        List<Rect> rectListTest = new ArrayList<>();

        Mat matTest = new Mat();
        Mat matTemplate = new Mat();

        Utils.bitmapToMat(bitmapOutputTest, matTest);
        Utils.bitmapToMat(bitmapOutputTemplate, matTemplate);

        int xTest = matTest.cols()/(bitmapOutputTest.getWidth()/10);
        int yTest = matTest.rows()/(bitmapOutputTest.getHeight()/10);

        int xTemplate = matTemplate.cols()/(bitmapOutputTemplate.getWidth()/10);
        int yTemplate = matTemplate.rows()/(bitmapOutputTemplate.getHeight()/10);

        for (int i =0;i<bitmapOutputTest.getWidth()/10;i++){
            for (int j=0;j<bitmapOutputTest.getHeight()/10;j++){
                roiTest = new Rect(new Point(i*xTest, j*yTest), new Point((i+1)*xTest, (j+1)*yTest));
                Mat submatTest = matTest.submat(roiTest);
                listMatPiecesTest.add(submatTest);

                rectListTest.add(roiTest);

                Mat imageTest = new Mat(roiTest.size(), CvType.CV_8UC1);
                Bitmap newBitmap = Bitmap.createBitmap(imageTest.cols(), imageTest.rows(), Bitmap.Config.RGB_565);

                listBitmapPiecesTest.add(newBitmap);
            }
        }

        for (int i =0;i<bitmapOutputTemplate.getWidth()/10;i++){
            for (int j=0;j<bitmapOutputTemplate.getHeight()/10;j++){
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
                Utils.bitmapToMat(bitmapOutputTest, matTest);

                Imgproc.rectangle(matTest, new Point(rectListTest.get(i-2).x, rectListTest.get(i-1).y), new Point(rectListTest.get(i-2).x+30,rectListTest.get(i-1).y+30), new Scalar(0,0,255), 1);

                Utils.matToBitmap(matTest, bitmapOutputTest);
            }
        }



    }
}
