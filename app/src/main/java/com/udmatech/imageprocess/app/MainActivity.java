package com.udmatech.imageprocess.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageView image1, image2;
    private ImageView imgOutput;
    private Button btnCompare, btnCrop;
    private int PICK_IMAGE_REQUEST;
    private Bitmap bitmap;
    private Button btnExtractText;
    private TessBaseAPI tessBaseApi;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String lang = "eng";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/ImageProcessing/";
    String result = "empty";
    private static final String TESSDATA = "tessdata";
    Uri outputFileUri;
    TextView touchedXY, invertedXY, imgSize, colorRGB, extractedText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image1 = (ImageView) findViewById(R.id.image1);
        image2 = (ImageView) findViewById(R.id.image2);
        imgOutput = (ImageView) findViewById(R.id.ivoutput);
        btnCompare = (Button) findViewById(R.id.btncompare);
        btnCrop = (Button) findViewById(R.id.btncrop);
        btnExtractText = (Button) findViewById(R.id.btnextract);
        touchedXY = (TextView) findViewById(R.id.xy);
        invertedXY = (TextView) findViewById(R.id.invertedxy);
        imgSize = (TextView) findViewById(R.id.size);
        colorRGB = (TextView) findViewById(R.id.colorrgb);
        extractedText = (TextView) findViewById(R.id.extract);

        imgOutput.setOnTouchListener(imgSourceOnTouchListener);
        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PICK_IMAGE_REQUEST = 1;
                showFileChooser(PICK_IMAGE_REQUEST);
            }
        });
        image2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PICK_IMAGE_REQUEST = 2;
                showFileChooser(PICK_IMAGE_REQUEST);
            }
        });
        btnCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitImage1 = ((BitmapDrawable) image1.getDrawable()).getBitmap();
                Bitmap bitImage2 = ((BitmapDrawable) image2.getDrawable()).getBitmap();
                if (bitImage1.getHeight() != bitImage2.getHeight() || bitImage1.getWidth() != bitImage2.getWidth())
                    Toast.makeText(MainActivity.this, "Images size are not same", Toast.LENGTH_LONG).show();
                else
                    findDifference(bitImage1, bitImage2);
            }
        });

        btnExtractText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String IMGS_PATH = Environment.getExternalStorageDirectory().toString() + "/ImageProcessing/imgs";
                prepareDirectory(IMGS_PATH);
                String img_path = IMGS_PATH + "/ocr.jpg";
                outputFileUri = Uri.fromFile(new File(img_path));
                doOCR();
               /*Bitmap img = ((BitmapDrawable) imgOutput.getDrawable()).getBitmap();
                result = extractText(img);
                extractedText.setText("extracted text: " + result);*/
            }
        });

        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap img = ((BitmapDrawable) imgOutput.getDrawable()).getBitmap();
                Uri uri = getImageUri(getApplicationContext(), img);
                performCrop(uri);
            }
        });
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void findDifference(Bitmap firstImage, Bitmap secondImage) {
        Bitmap bmp = secondImage.copy(secondImage.getConfig(), true);
        if (firstImage.getWidth() != secondImage.getWidth()
                || firstImage.getHeight() != secondImage.getHeight()) {
            return;
        }
        for (int i = 0; i < firstImage.getWidth(); i++) {
            for (int j = 0; j < firstImage.getHeight(); j++) {
                if (firstImage.getPixel(i, j) != secondImage.getPixel(i, j)) {
                    bmp.setPixel(i, j, Color.RED);
                }
            }
        }
        imgOutput.setImageBitmap(bmp);
    }

    private void showFileChooser(int PICK_IMAGE_REQUEST) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            /*try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 1024, 768, true);
                image1.setImageBitmap(resized);
                Picasso.with(getApplicationContext()).load(filePath).into(image1);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            Picasso.with(getApplicationContext()).load(filePath).into(image1);
        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            /*try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 1024, 768, true);
                image2.setImageBitmap(resized);

            } catch (IOException e) {
                e.printStackTrace();
            }*/
            Picasso.with(getApplicationContext()).load(filePath).into(image2);
        } else if (requestCode == PIC_CROP) {
            if (data != null) {
                // get the returned data
                Bundle extras = data.getExtras();
                // get the cropped bitmap
                //Bitmap selectedBitmap = extras.getParcelable("data");
                // Bitmap resized = Bitmap.createScaledBitmap(selectedBitmap, 1024, 768, true);
                //imgOutput.setImageBitmap(selectedBitmap);
                Picasso.with(getApplicationContext()).load((Uri) extras.getParcelable("data")).into(image2);
            }
        }
    }


    View.OnTouchListener imgSourceOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {

            float eventX = event.getX();
            float eventY = event.getY();
            float[] eventXY = new float[]{eventX, eventY};

            Matrix invertMatrix = new Matrix();
            ((ImageView) view).getImageMatrix().invert(invertMatrix);

            invertMatrix.mapPoints(eventXY);
            int x = Integer.valueOf((int) eventXY[0]);
            int y = Integer.valueOf((int) eventXY[1]);

            touchedXY.setText(
                    "touched position: "
                            + String.valueOf(eventX) + " / "
                            + String.valueOf(eventY));
            invertedXY.setText(
                    "touched position: "
                            + String.valueOf(x) + " / "
                            + String.valueOf(y));

            Drawable imgDrawable = ((ImageView) view).getDrawable();
            Bitmap bitmap = ((BitmapDrawable) imgDrawable).getBitmap();

            imgSize.setText(
                    "drawable size: "
                            + String.valueOf(bitmap.getWidth()) + " / "
                            + String.valueOf(bitmap.getHeight()));

            //Limit x, y range within bitmap
            if (x < 0) {
                x = 0;
            } else if (x > bitmap.getWidth() - 1) {
                x = bitmap.getWidth() - 1;
            }

            if (y < 0) {
                y = 0;
            } else if (y > bitmap.getHeight() - 1) {
                y = bitmap.getHeight() - 1;
            }

            int touchedRGB = bitmap.getPixel(x, y);

            colorRGB.setText("touched color: " + "#" + Integer.toHexString(touchedRGB));
            colorRGB.setTextColor(touchedRGB);

            return true;
        }
    };

    private String extractText(Bitmap bitmap) {
        try {
            tessBaseApi = new TessBaseAPI();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (tessBaseApi == null) {
                Log.e(TAG, "TessBaseAPI is null. TessFactory not returning tess object.");
            }
        }

        tessBaseApi.init(DATA_PATH, lang);

//       //EXTRA SETTINGS
//        //For example if we only want to detect numbers
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
//
//        //blackList Example
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
//                "YTRWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");

        Log.d(TAG, "Training file loaded");
        tessBaseApi.setImage(bitmap);
        tessBaseApi.setPageSegMode(1);

        String extractedText = "empty result";
        try {
            extractedText = tessBaseApi.getUTF8Text();
        } catch (Exception e) {
            Log.e(TAG, "Error in recognizing text.");
        }
        tessBaseApi.end();
        return extractedText;
    }

    private void doOCR() {
        prepareTesseract();
        startOCR(outputFileUri);
    }

    private void startOCR(Uri imgUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 1 - means max size. 4 - means maxsize/4 size. Don't use value <4, because you need more memory in the heap to store your data.
            //Bitmap bitmap = BitmapFactory.decodeFile(imgUri.getPath(), options);
            //Bitmap i = BitmapFactory.decodeResource(this.getResources(), R.drawable.h);
            Bitmap img = ((BitmapDrawable) imgOutput.getDrawable()).getBitmap();
            result = extractText(img);
            extractedText.setText("extracted text: " + result);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void prepareTesseract() {
        try {
            prepareDirectory(DATA_PATH + TESSDATA);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyTessDataFiles(TESSDATA);
    }

    private void prepareDirectory(String path) {

        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }
    }

    private void copyTessDataFiles(String path) {
        try {
            String fileList[] = getAssets().list(path);

            for (String fileName : fileList) {

                // open file within the assets folder
                // if it is not already there copy it to the sdcard
                String pathToDataFile = DATA_PATH + path + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {

                    InputStream in = getAssets().open(path + "/" + fileName);

                    OutputStream out = new FileOutputStream(pathToDataFile);

                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();

                    Log.d(TAG, "Copied " + fileName + "to tessdata");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to copy files to tessdata " + e.toString());
        }
    }

    final int PIC_CROP = 1;

    private void performCrop(Uri picUri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            // set crop properties here
            cropIntent.putExtra("crop", true);
            // indicate aspect of desired crop
            //cropIntent.putExtra("aspectX", 16);
            //cropIntent.putExtra("aspectY", 9);
            // indicate output X and Y
            //cropIntent.putExtra("outputX", 1024);
            //cropIntent.putExtra("outputY", 768);
            // retrieve data on return
            cropIntent.putExtra("return-data", true);
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            // display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
