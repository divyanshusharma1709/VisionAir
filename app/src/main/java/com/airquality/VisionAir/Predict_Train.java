package com.airquality.VisionAir;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.Tensors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;

public class Predict_Train extends AppCompatActivity {
    byte[] graphDef;
    Session sess;
    Graph graph;
    File file;
    int epochs = 3;
    float distance;
    Tensor<String> checkpointPrefix;
    String checkpointDir;
    float[][] features;
    float[] labels;

    public void finalSave() {
        ArrayList<ArrayList<Tensor<?>>> at = getWeights();
        int ctr = 0;
        ArrayList<float[]> diff = new ArrayList<>();
        for (int x = 0; x < 6; x++) {

            float[] aw = flattenedWeight(at.get(x).get(0));

            diff.add(aw);
            for(float[] f: diff)
            {
                for(float z: f)
                {
                    if(z == 0)
                    {
                        ctr++;
                    }
                }

            }
        }
        Log.i("COUNTER: ", String.valueOf(ctr));
        save(diff);
    }

    public ArrayList<ArrayList<Tensor<?>>> getWeights() {
        ArrayList<Tensor<?>> w1 = (ArrayList<Tensor<?>>) sess.runner().fetch("w1:0").run();
        ArrayList<Tensor<?>> b1 = (ArrayList<Tensor<?>>) sess.runner().fetch("b1:0").run();
        ArrayList<Tensor<?>> w2 = (ArrayList<Tensor<?>>) sess.runner().fetch("w2:0").run();
        ArrayList<Tensor<?>> b2 = (ArrayList<Tensor<?>>) sess.runner().fetch("b2:0").run();
        ArrayList<Tensor<?>> w3 = (ArrayList<Tensor<?>>) sess.runner().fetch("wo:0").run();
        ArrayList<Tensor<?>> b3 = (ArrayList<Tensor<?>>) sess.runner().fetch("bo:0").run();

        ArrayList<ArrayList<Tensor<?>>> ls = new ArrayList<>();
        ls.add(w1);
        ls.add(b1);
        ls.add(w2);
        ls.add(b2);
        ls.add(w3);
        ls.add(b3);
        Log.i("Shapes: ", w1.get(0).shape()[0] + ", " + w1.get(0).shape()[1]);
        Log.i("Shapes: ", b1.get(0).shape()[0] + ", " + b1.get(0).shape()[1]);
        Log.i("Shapes: ", w2.get(0).shape()[0] + ", " + w2.get(0).shape()[1]);
        Log.i("Shapes: ", b2.get(0).shape()[0] + ", " + b2.get(0).shape()[1]);
        Log.i("Shapes: ", w3.get(0).shape()[0] + ", " + w3.get(0).shape()[1]);
        Log.i("Shapes: ", b3.get(0).shape()[0] + ", " + b3.get(0).shape()[1]);

        return ls;
    }

    public void save(ArrayList<float[]> diff) {

        float[] d1 = diff.get(0);
        float[] d2 = diff.get(1);
        float[] d3 = diff.get(2);
        float[] d4 = diff.get(3);
        float[] d5 = diff.get(4);
        float[] d6 = diff.get(5);
        int l1 = diff.get(0).length;
        int l2 = diff.get(1).length;
        int l3 = diff.get(2).length;
        int l4 = diff.get(3).length;
        int l5 = diff.get(4).length;
        int l6 = diff.get(5).length;
        int ctr = 0;
        int i = 0;
        int j = 0;
        float[] result = new float[l1 + l2 + l3 + l4 + l5 + l6];
        for(i = 0, j = 0; j < l1; i++, j++)
        {
            result[i] = d1[j];
        }
        for(int k = 0; k < l2; i++, k++)
        {
            result[i] = d2[k];
        }
        for(int l = 0; l < l3; i++, l++)
        {
            result[i] = d3[l];
        }
        for(int m = 0; m < l4; i++, m++)
        {
            result[i] = d4[m];
        }
        for(int n = 0; n < l5; i++, n++)
        {
            result[i] = d5[n];
        }
        for(int o = 0; o < l6; i++, o++)
        {
            result[i] = d6[o];
        }
        for(float x: result)
        {
            if(x == 0.0)
                ctr++;
        }
        Log.i("COUNTER_A: ", String.valueOf(ctr));
        Log.i("Result Length:  ", String.valueOf(ctr));

        saveWeights(result, "Weights.bin");
    }

    //Divide by distance to weight weights
    public void saveWeights(float[] diff, String name) {
        byte[] byteArray = new byte[diff.length * 4];
        // wrap the byte array to the byte buffer
        ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
        for(byte b: byteBuf.array())
        {
            Log.i("ByteBuffer: ", String.valueOf(b));

        }
        // create a view of the byte buffer as a float buffer
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();


        // now put the float array to the float buffer,
        // it is actually stored to the byte array
        floatBuf.put(diff);
        saveFile(byteArray, name);
    }

    public void saveFile(byte[] byteArray, String name) {
        file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), name);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e("Error", "Error: FILE" + "File not Created!");
            }
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.i("TAG", "Error: FILE" + "File not found!");
        }
        try {
            os.write(byteArray);
            Log.i("TAG", "FileWriter" + "File written successfully");
        } catch (IOException e) {
            Log.i("TAG", "Error: FILE" + "File not written!");
        }
    }

    public void initializeGraph() {
        checkpointPrefix = Tensors.create((getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/FINAL_GRAPH.ckpt"));
        checkpointDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        graph = new Graph();
        sess = new Session(graph);
        InputStream inputStream;
        try {
            inputStream = getAssets().open("FINAL_GRAPH.pb");
            byte[] buffer = new byte[inputStream.available()];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            graphDef = output.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        graph.importGraphDef(graphDef);
        try {
            sess.runner().feed("save/Const", checkpointPrefix).addTarget("save/restore_all").run();
            //Toast.makeText(this, "Checkpoint Found and Loaded!", Toast.LENGTH_SHORT).show();
            Log.i("Init: ", "Checkpoint Found and Loaded!");
        } catch (Exception e) {
            sess.runner().addTarget("init").run();
            Log.i("TAG", "Checkpoint: " + "Graph Initialized");
            Log.i("GraphLoadError", String.valueOf(e));
        }
    }

    private float[] flattenedWeight(Tensor t) {
        float[] flat = new float[(int) (t.shape()[0]) * (int) t.shape()[1]];
        float[][] arr = new float[(int) (t.shape()[0])][(int) t.shape()[1]];
        t.copyTo(arr);
        int x = 0;
        for (int i = 0; i < t.shape()[0]; i++) {
            for (int j = 0; j < t.shape()[1]; j++) {
                flat[x] = arr[i][j];
                x++;
            }
        }
        return flat;
    }

    private String train(float[][] features, float[] label, int epochs) {
        Tensor x_train = Tensor.create(features);
        Tensor y_train = Tensor.create(label);
        int ctr = 0;
        while (ctr < epochs) {
            sess.runner().feed("input", x_train).feed("target", y_train).addTarget("train_op").run();
            ctr++;
        }
        Log.i("TAG", "Model Trained");
        return "Model Trained";
    }


    private float predict(float[][] features) {
        Tensor input = Tensor.create(features);
        float[][] output = new float[1][1];
        Tensor op_tensor = sess.runner().feed("input", input).fetch("output").run().get(0).expect(Float.class);
        Log.i("Tensor Shape", op_tensor.shape()[0] + ", " + op_tensor.shape()[1]);
        op_tensor.copyTo(output);
        return output[0][0];
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        ArrayList<Float> featFirebase = new ArrayList<>();
        Object[] featureSerialized = (Object[]) getIntent().getExtras().getSerializable("Features");
        labels = getIntent().getExtras().getFloatArray("Labels");
        distance = getIntent().getExtras().getFloat("Distance");
        if (featureSerialized != null) {
            features = new float[featureSerialized.length][];
            for (int i = 0; i < featureSerialized.length; i++) {
                features[i] = (float[]) featureSerialized[i];
            }
        }
        for (int i = 0; i < 10; i++) {
            featFirebase.add(features[0][i]);
        }
        featFirebase.add(labels[0]);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("FeaturesLabels");
        ref.push().setValue(featFirebase);

        //Check if new Model is available
        MyAsyncTask isGlobalModelUpdated = new MyAsyncTask(Predict_Train.this, file, "ismodelUpdated", new MyAsyncTask.AsyncResponse() {
            @Override
            public void processFinish(String result) {
                //If True, get Global Model
                if (result.equals("True")) {
                    MyAsyncTask getGlobalModel = new MyAsyncTask(Predict_Train.this, file, "getModel", new MyAsyncTask.AsyncResponse() {
                        @Override
                        public void processFinish(String result) {
                            Log.i("TAG", "Output of GetGlobalModel: " + result);
                            if (result.equals("Download Succeeded")) {
                                //Predict AQI
                                initializeGraph();

                                float aqi_pred = predict(features);
                                TextView predbox;
                                if(aqi_pred <= 60){
                                    setContentView(R.layout.activity_predict_green);
                                    predbox = findViewById(R.id.pred_green);
                                }
                                else if(aqi_pred < 120 && aqi_pred > 60){
                                setContentView(R.layout.activity_predict_ui);
                                predbox = findViewById(R.id.pred_orang);}
                                else{
                                    setContentView(R.layout.activity_predict_red);
                                    predbox = findViewById(R.id.pred_red);
                                }
                                Button retry = findViewById(R.id.retryBtn);
                                retry.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(Predict_Train.this, MainActivity.class);
                                        startActivity(intent);
                                    }
                                });
                                String featString = "";
                                for (float[] arr : features) {
                                    for (float el : arr) {
                                        featString += (el + ", ");
                                    }
                                }
//                                TextView featBox = findViewById(R.id.featureBox);
//                                featBox.setText(featString);
                                NumberFormat formatter = NumberFormat.getNumberInstance();
                                formatter.setMinimumFractionDigits(1);
                                formatter.setMaximumFractionDigits(1);


                                predbox.setText(Float.toString(Float.parseFloat(formatter.format(aqi_pred))));
                                SharedPreferences pref = getApplicationContext().getSharedPreferences("Pref", 0);
                                boolean cont = pref.getBoolean("Contrib", false);
                                Log.i("Contrib: ", String.valueOf(cont));
                                //Run training epoch
                                if(cont) {
                                    train(features, labels, epochs);
//                                    int iter = pref.getInt("n_samples", 0);
//                                    iter++;
//                                    SharedPreferences.Editor editor = pref.edit();
//                                    editor.putInt("n_samples", iter);
//                                    editor.commit();
                                    //Save Weights in Private Storage
                                    finalSave();
                                    //Upload Weights to Server
                                    MyAsyncTask uploadWeights = new MyAsyncTask(Predict_Train.this, file, "uploadWeights", new MyAsyncTask.AsyncResponse() {
                                        @Override
                                        public void processFinish(String result) {
                                            Log.i("TAG", "Output: uploadWeights" + result);
                                        }
                                    });
                                    uploadWeights.execute();
                                }

                            }
                        }
                    });
                    getGlobalModel.execute();
                }
                Log.i("TAG", "Output: isModelUpdated" + result);

            }
        });
        isGlobalModelUpdated.execute();


    }

    public void logLargeString(String str) {
        if (str.length() > 3000) {
            Log.i("Array: ", str.substring(0, 3000));
            logLargeString(str.substring(3000));
        } else {
            Log.i("Array: ", str); // continuation
        }
    }

    public void logWeight(float[] flat) {
        String s = "";
        for (int z = 0; z < flat.length; z++) {
            s += "  " + flat[z];
        }
        logLargeString(s);
        Log.i("Array Length: ", String.valueOf(flat.length));
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

}
