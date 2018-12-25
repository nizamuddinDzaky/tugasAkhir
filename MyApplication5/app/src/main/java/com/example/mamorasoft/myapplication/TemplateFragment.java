package com.example.mamorasoft.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.constraint.solver.Cache;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.github.kimkevin.cachepot.CachePot;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class TemplateFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private static final int RESULT_LOAD_IMAGE_TEMPLATE = 1;

    public static Bitmap bitmapTemplate, bitmapBlackWhiteTemplate;

    private ImageView imageViewTemplate;
    private Button btnLoadTemplate, btnBlackWhite;

    public TemplateFragment() {
    }

    public static TemplateFragment newInstance() {
        TemplateFragment fragment = new TemplateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.loadLibrary("opencv_java3");
        Log.e("lalala", "oncreate template fragment");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bitmapTemplate ==null){
            bitmapTemplate = CachePot.getInstance().pop("bitmapSelected_template");
        }
//        bitmapTemplate = Bitmap.createBitmap(bitmapTemplate);
        imageViewTemplate.setImageBitmap(bitmapTemplate);
//        Log.e("lalala", "onresume template fragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View viewRoot = inflater.inflate(R.layout.fragment_template, container, false);

        imageViewTemplate = (ImageView) viewRoot.findViewById(R.id.iv_templateImage);
        btnLoadTemplate = (Button) viewRoot.findViewById(R.id.btn_loadTemplate);
        btnBlackWhite = (Button) viewRoot.findViewById(R.id.btn_blackWhiteTemplate);

        btnLoadTemplate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intentLoadTemplate = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(intentLoadTemplate, RESULT_LOAD_IMAGE_TEMPLATE);
                Intent toMain = new Intent(getActivity(), MainActivity.class);
                toMain.putExtra("fragmen", 0);
                startActivity(toMain);
            }
        });

        btnBlackWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            bitmapTemplate = getResizedBitmap(bitmapTemplate);
            bitmapTemplate = edgeDetection(bitmapTemplate);
            bitmapBlackWhiteTemplate = blackWhite(bitmapTemplate);
            imageViewTemplate.setImageBitmap(bitmapBlackWhiteTemplate);
            CachePot.getInstance().push("bitmapTemplate",bitmapBlackWhiteTemplate);
            }
        });

        return viewRoot;
    }

    private Bitmap edgeDetection(Bitmap bm) {
        Mat edges = new Mat();
        Mat src = new Mat();
        Utils.bitmapToMat(bm, src);
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, new Size(5, 5), 0);
        Imgproc.Canny(edges, edges, 80, 240);
        Utils.matToBitmap(edges,bm);
        return bm;
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

        if (requestCode == RESULT_LOAD_IMAGE_TEMPLATE){
            if (resultCode == RESULT_OK && data != null){
                Uri imageUri = data.getData();

                try {
                    bitmapTemplate = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    bitmapTemplate = getResizedBitmap(bitmapTemplate);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageViewTemplate.setImageBitmap(bitmapTemplate);
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, 200, 300, false);
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
