package com.example.findinimage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.GridView;
import android.widget.SearchView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // image to process
    private InputImage input_img;
    public static final int REQUEST_PERMISSION = 300;
    private File[] allFiles;
    private HashMap<String, String> AllImagesPaths;
    private HashMap<String, String> TextQueryTexts;
    private HashMap<String, String> ObjectQueryTexts;
    private File text_query_file;
    private File objects_query_file;
    private GridView gdview;
    private CustomAdap customAdapter;
    private FileOutputStream writer1;
    private FileOutputStream writer2;
    private ProgressDialog progressDialog;
    private ArrayList<String> found_image_paths;
    private List<String> classList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setTheme(R.style.AppTheme);

        gdview = findViewById(R.id.gridview);
        AllImagesPaths = new HashMap<>();
        TextQueryTexts = new HashMap<>();
        ObjectQueryTexts = new HashMap<>();
        found_image_paths = new ArrayList<>();

        // request permission to read data (aka images) from the user's external storage of their phone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        gdview.setOnItemClickListener((parent, view, position, id) -> {
            // set an Intent to Another Activity
            Intent intent = new Intent(MainActivity.this, SelectedImageView.class);
            intent.putExtra("image", found_image_paths.get(position)); // put image data in Intent
            startActivity(intent); // start Intent
        });

        // progress bar
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Recognizing images...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setGravity(Gravity.BOTTOM);

        // main function
        try {
            text_query_file = getFileStreamPath("text_query_file.txt"); // text to save text search keywords
            objects_query_file = getFileStreamPath("objects_query_file.txt"); // text to save objects search keywords
            if (!text_query_file.exists() && objects_query_file.exists()) {
                // file does not exist, first run

                boolean f_status = text_query_file.createNewFile(); // file status
                Log.d("File text_query_file.txt Created: ", String.valueOf(f_status));
                f_status = objects_query_file.createNewFile(); // file status
                Log.d("File objects_query_file.txt Created: ", String.valueOf(f_status));

                progressDialog.show();
                Thread mThread = new Thread() {
                    @Override
                    public void run() {
                        get_all_images();
                        progressDialog.setMessage("This might take a while....");
                        find_all_text();
                        find_all_objects();
                        progressDialog.dismiss();
                    }
                };
                mThread.start();

            } else {
                // /file exists, load existing query file

                progressDialog.show();
                Thread mThread = new Thread() {
                    @Override
                    public void run() {
                        get_all_images();
                        progressDialog.setMessage("This might take a while....");
                        if (!AllImagesPaths.isEmpty()) {
                            // check if directory has new files

                            int total_dir_images = AllImagesPaths.size();
                            int total_text_images_found = read_all_text();
                            int total_objects_images_found = read_all_objects();
                            if (total_dir_images > total_text_images_found
                                    && total_dir_images > total_objects_images_found) {
                                // rescan for text again

                                find_all_text();
                                find_all_objects();
                            }
                        }
                        progressDialog.dismiss();
                    }
                };
                mThread.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        // search bar to fetch images
        @Override
        public boolean onCreateOptionsMenu (Menu menu){
            // searchview operation to fetch results

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu, menu);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.searchBar).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setMaxWidth(Integer.MAX_VALUE);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (customAdapter != null) {
                        customAdapter.clear();
                    }
                    progressDialog.show();
                    Thread mThread = new Thread() {
                        @Override
                        public void run() {
                            read_all_text();
                            read_all_objects();
                            fetch_results(query);
                            progressDialog.dismiss();
                        }
                    };
                    mThread.start();
                    searchView.onActionViewCollapsed();

                    // fetch into gridview
                    if (found_image_paths.size() != 0) {
                        customAdapter = new CustomAdap(getApplicationContext(), 0, found_image_paths);
                        gdview.setAdapter(customAdapter);
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    return false;
                }
            });
            searchView.setOnCloseListener(() -> false);
            return true;
        }

        private void get_all_images () {
            // finds all files from folder which are jpg/jpeg/png and make HashMap(img_name, img_path) of them

            try {
                File directory1 = new File(Environment.getExternalStorageDirectory().getPath() + "/Download");
                File directory2 = new File(Environment.getExternalStorageDirectory().getPath() + "/bluetooth");
                File directory3 = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM");
                File directory4 = new File(Environment.getExternalStorageDirectory().getPath() + "/Edited");
                File directory5 = new File(Environment.getExternalStorageDirectory().getPath() + "/PICTURES");

                File[] directories = new File[5];
                directories[0] = directory1;
                directories[1] = directory2;
                directories[2] = directory3;
                directories[3] = directory4;
                directories[4] = directory5;
                int total_images_found = 0;

                for (File directory : directories) {

                    if (directory.isDirectory() && directory.listFiles().length != 0) {
                        allFiles = directory.listFiles();
                    }
                    Log.d("dir path", directory.getAbsolutePath());

                    for (File f : allFiles) {
                        String image_path = f.getAbsolutePath();
                        if (image_path.endsWith("png") || image_path.endsWith("jpg") || image_path.endsWith("jpeg") ||
                                image_path.endsWith("PNG") || image_path.endsWith("JPG") || image_path.endsWith("JPEG")) {
                            AllImagesPaths.put(f.getName(), image_path); // save names,paths for search query
                        }
                    }
                    if (!AllImagesPaths.isEmpty()) {
                        total_images_found = total_images_found + AllImagesPaths.size();
                    }
                }
                Log.d("Got files: ", String.valueOf(total_images_found));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }

        private void find_all_text () {
            // reads all images, finds texts and write to text_query_file.txt file

            Log.d("Finding all text", "...............................");
            try {
                writer1 = openFileOutput(text_query_file.getName(), Context.MODE_PRIVATE);
            } // initiate writer
            catch (IOException e) {
                Log.d("Error loading writer", "...........................");
                e.printStackTrace();
            }
            try {
                TextRecognizer recognizer = TextRecognition.getClient(); // create text recognizer object
                for (Map.Entry m : AllImagesPaths.entrySet()) {
                    Bitmap mBitmap = BitmapFactory.decodeFile(m.getValue().toString());
                    input_img = InputImage.fromBitmap(mBitmap, 0);
                    Task<Text> result =
                            recognizer.process(input_img)
                                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                                        @Override
                                        public void onSuccess(Text visionText) {
                                            // Task completed successfully
                                            try {
                                                writer1.write((m.getKey().toString() + ":").getBytes());
                                                for (Text.TextBlock block : visionText.getTextBlocks()) {
                                                    for (Text.Line line : block.getLines()) {
                                                        for (Text.Element element : line.getElements()) {
                                                            String search_keyword = element.getText();
                                                            if (search_keyword.length() > 3) {
                                                                writer1.write(search_keyword.getBytes());
                                                                writer1.write(",".getBytes());
                                                                writer1.flush();
                                                            }
                                                        }

                                                    }
                                                }
                                                writer1.write("\n".getBytes());
                                            } catch (IOException e) {
                                                Log.d("Error in writer", ".............................................");
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Task failed with an exception
                                                    // ...
                                                    e.printStackTrace();
                                                    Log.d("Not found", "........................................");
                                                }
                                            });
                }
            } catch (Exception e) {
                Log.d("Error in finding text", "............................");
                e.printStackTrace();
            }
            Log.d("Done finding text", "...............................");
        }

        private int read_all_text () {
            // reads from text_query_file.txt file to HashMap(img_name, text_found_in_them)

            int total_lines_found = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(text_query_file));

                String line;
                while ((line = br.readLine()) != null) {
                    total_lines_found++;
                    if (line.split(":").length != 1) { // check for empty query
                        TextQueryTexts.put(line.split(":")[0], line.toLowerCase().split(":")[1]); //image name and query value
                    }
                }
                br.close();
            } catch (Exception e) {
                Log.d("Error in read_all_text", "................................................");
                e.printStackTrace();
            }
            Log.d("Done Reading text", "................");
            return total_lines_found;
        }

        private void find_all_objects () {
            // perform image classification and save results to objects_query_file.txt file

            Log.d("Finding all objects", "...............................");
            try {
                writer2 = openFileOutput(objects_query_file.getName(), Context.MODE_PRIVATE);
                classList = load_labels();
            } // initiate writer
            catch (IOException e) {
                Log.d("Error loading labels file/writer", "........................");
                e.printStackTrace();
            }
            LocalModel localModel =
                    new LocalModel.Builder().setAssetFilePath("mobilenetV2_model.tflite").build();
            CustomImageLabelerOptions customImageLabelerOptions =
                    new CustomImageLabelerOptions.Builder(localModel)
                            .setConfidenceThreshold(0.5f)
                            .setMaxResultCount(5)
                            .build();
            ImageLabeler labeler = ImageLabeling.getClient(customImageLabelerOptions);
            try {
                for (Map.Entry m : AllImagesPaths.entrySet()) {
                    Bitmap mBitmap = BitmapFactory.decodeFile(m.getValue().toString());
                    input_img = InputImage.fromBitmap(mBitmap, 0);
                    labeler.process(input_img)
                            .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                                @Override
                                public void onSuccess(List<ImageLabel> labels) {
                                    // Task completed successfully

                                    try {
                                        writer2.write((m.getKey().toString() + ":").getBytes());
                                        float confidence = 1;
                                        int index = 0;
                                        for (ImageLabel label : labels) {
                                            if (label.getConfidence() >= confidence) {
                                                confidence = label.getConfidence();
                                                index = label.getIndex();
                                            }
                                        }
                                        writer2.write(classList.get(index).getBytes());
                                        writer2.write("\n".getBytes());
                                        writer2.flush();
                                    } catch (IOException e) {
                                        Log.d("Error in writer", "...................");
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    Log.d("Error in image classification", "...............................");
                                    e.printStackTrace();
                                }
                            });
                }
            } catch (Exception e) {
                Log.d("Error in finding objects", ".............................");
                e.printStackTrace();
            }

            Log.d("Done finding all objects", "...............................");
        }

        private int read_all_objects () {
            // reads from objects_query_file.txt file to HashMap(img_name, label)

            int total_lines_found = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(objects_query_file));
                String line;
                while ((line = br.readLine()) != null) {
                    total_lines_found++;
                    ObjectQueryTexts.put(line.split(":")[0], line.toLowerCase().split(":")[1]); //image name and query value
                }
                br.close();
            } catch (Exception e) {
                Log.d("Error in read_all_objects", "................................................");
                e.printStackTrace();
            }
            Log.d("Done Reading objects", "................");
            return total_lines_found;
        }


        private void find_all_faces () {

        }

        private void fetch_results (String query){
            // searches the query, matches and saves result image_name to ArrayList(image_name)

            Log.d("Looking for ", query);

            // look for text query
            for (Map.Entry m : TextQueryTexts.entrySet()) {
                String[] queries = m.getValue().toString().split(",");
                for (String q : queries) {
                    if (q.equals(query.toLowerCase())) {
                        String s = m.getKey().toString();
                        String img_path = AllImagesPaths.get(s);
                        found_image_paths.add(img_path);
                        break;
                    }
                }
            }

            // look for objects query
            for (Map.Entry m : ObjectQueryTexts.entrySet()) {
                String object_label = m.getValue().toString().toLowerCase();
                if (object_label.equals(query.toLowerCase())) {
                    String s = m.getKey().toString();
                    String img_path = AllImagesPaths.get(s);
                    found_image_paths.add(img_path);
                }
            }

            Log.d("Results Fetched", String.valueOf(found_image_paths.size()));
        }

        private List<String> load_labels () throws IOException {
            // loads Label from labels file
            List<String> labelList = new ArrayList<String>();
            try {
                AssetFileDescriptor fileDescriptor = getAssets().openFd("labels.txt");
                FileInputStream inputStream = fileDescriptor.createInputStream();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    labelList.add(line);
                }
                reader.close();
            } catch (Exception e) {
                Log.d("Error label loader", ".....................");
                e.printStackTrace();
            }
            return labelList;
        }

        private void del_search_keywords_file () {
            // delete search_keywords.txt file

            boolean s = text_query_file.delete();
            Log.d("File text_query_file Deleted ", String.valueOf(s));
            s = objects_query_file.delete();
            Log.d("File objects_query_file Deleted ", String.valueOf(s));
        }
    }