package com.lihang.andro.scanner.activity;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.lihang.andro.scanner.R;
import com.lihang.andro.scanner.ui.ArcProcessBar;
import com.lihang.andro.scanner.utils.Utils;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 0;      // request code for permission

    static final String[] permissions = new String[] {     // the permissions which this app needs
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ArcProcessBar arcProgressBar;
    private Button btn_scan;
    private Button btn_share;
    private ScrollView container_result;
    private TextView tv_status;
    private TextView tv_top_size;
    private TextView tv_avg;
    private TextView tv_frequent_ext;

    private boolean isStart = false;          // check if scan is started
    private boolean isStop = false;           // check if scan is stopped
    private boolean isFinish = false;         // check if scan is finished
    private boolean isClose = false;          // check if the app is closed
    private boolean isShareVisible = false;   // check if the share button is visible;
    private boolean isTextVisible = false;    // check if the status area is visible
    private boolean isResultVisible = false;  // check if the result area is visible

    private String content_scan;              // the text in the scan button
    private String content_status;            // the text in the status area
    private String content_size;              // the text of top 10 file size
    private String content_avg;               // the text of average size
    private String content_ext;               // the text of 5 most frequent extension
    private String shareContent;
    private int progress = 0;                 // progress of the scan process
    private long position = 0L;               // cursor position
    private long total = 0L;                  // the total size od the files have been scanned

    class SerializableMap extends TreeMap<String, Long> implements Serializable {

    }
    // key:name  value:size
    SerializableMap sizeMap = new SerializableMap();

    // key:extension name value:amount
    SerializableMap extensionMap= new SerializableMap();


    private scanTask task;

    NotificationManager nm;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasPermission(permissions)) {

        } else {
            requestPermission(REQUEST_CODE, permissions);
        }

        setContentView(R.layout.activity_main);

        arcProgressBar = (ArcProcessBar) findViewById(R.id.arcProgressBar);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_share = (Button) findViewById(R.id.btn_share);
        btn_share.setVisibility(View.INVISIBLE);
        tv_status = (TextView) findViewById(R.id.tv_status);

        container_result = (ScrollView) findViewById(R.id.container_result);
        container_result.setVisibility(View.INVISIBLE);
        tv_top_size = (TextView) findViewById(R.id.tv_top_size);
        tv_avg = (TextView) findViewById(R.id.tv_avg);
        tv_frequent_ext = (TextView) findViewById(R.id.tv_frequent_ext);

        setArcProgressBar();

        // restore the status
        if (savedInstanceState != null) {
            isStart = savedInstanceState.getBoolean("isStart");
            isStop = savedInstanceState.getBoolean("isStop");

            isFinish = savedInstanceState.getBoolean("isFinish");
            isClose = savedInstanceState.getBoolean("isClose");

            isShareVisible = savedInstanceState.getBoolean("isShareVisible");
            if (isShareVisible) {
                btn_share.setVisibility(View.VISIBLE);
            } else {
                btn_share.setVisibility(View.INVISIBLE);
            }

            isTextVisible = savedInstanceState.getBoolean("isTextVisible");
            isResultVisible = savedInstanceState.getBoolean("isResultVisible");

            content_scan = savedInstanceState.getString("content_scan");
            btn_scan.setText(content_scan);

            content_status = savedInstanceState.getString("content_status");
            tv_status.setText(content_status);

            content_size = savedInstanceState.getString("content_size");
            content_avg = savedInstanceState.getString("content_avg");
            content_ext = savedInstanceState.getString("content_ext");
            if (isResultVisible) {
                container_result.setVisibility(View.VISIBLE);
                tv_top_size.setText(content_size);
                tv_avg.setText(content_avg);
                tv_frequent_ext.setText(content_ext);
            }  else {
                container_result.setVisibility(View.INVISIBLE);
            }

            shareContent = savedInstanceState.getString("shareContent");

            progress = savedInstanceState.getInt("progress");
            arcProgressBar.setProgress(progress);

            position = savedInstanceState.getLong("position");

            total = savedInstanceState.getLong("total");

            sizeMap = ((SerializableMap)savedInstanceState.getSerializable("size"));
            extensionMap = ((SerializableMap)savedInstanceState.getSerializable("ext"));

            if (task == null && !isFinish) {
                task = new scanTask();
                task.execute(position);
            }

        }

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isStart && !isStop ) {
                    // send the notification
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        nm = (NotificationManager)getSystemService(Activity.NOTIFICATION_SERVICE);
                        NotificationCompat.Builder notification = new NotificationCompat.Builder(MainActivity.this)
                                .setContentTitle("Scanner" )
                                .setContentText("Scan the sd card")
                                .setSmallIcon(R.drawable.icon_scan);
                        nm.notify(0, notification.build());

                        btn_scan.setText("Stop");
                        content_scan = "Stop";
                        tv_status.setText("Scanning...");
                        content_status = "Scanning";
                        btn_share.setVisibility(View.INVISIBLE);
                        isShareVisible = false;
                        container_result.setVisibility(View.INVISIBLE);
                        isResultVisible = false;
                        isStart = true;
                        isFinish = false;
                        task = new scanTask();
                        task.execute(0L);

                    } else {
                        Toast.makeText(MainActivity.this, "No SD card.", Toast.LENGTH_SHORT);
                    }
                } else {
                    if (isStart && !isStop) {
                        btn_scan.setText("Scan");
                        content_scan = "Scan";
                        tv_status.setText("Stopped...");
                        content_status = "Stopped...";
                        isStop = true;
                    } else {
                        if (isStart && isStop) {
                            btn_scan.setText("Stop");
                            content_scan = "Stop";
                            tv_status.setText("Scanning...");
                            content_status = "Scanning...";
                            isStop = false;
                        }
                    }
                }


            }
        });

        btn_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/*");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Share");
                intent.putExtra(Intent.EXTRA_TEXT, shareContent);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, getTitle()));
            }
        });

    }

    private void setArcProgressBar() {
        arcProgressBar.setMax(100);
        arcProgressBar.setTitle("SD");
    }

    private class scanTask extends AsyncTask<Long, Integer, Integer> {

/*        // key:name  value:size
        Map<String, Long> sizeMap = new TreeMap<>();

        // key:extension name value:amount
        Map<String, Long> extensionMap= new TreeMap<>();*/


        long current = 0;    // current file number
        long count = 0;      // total number of files
        long totalSize = total;  // total size of files

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Long... values) {
            queryFiles(values[0]);
            // File root = Environment.getExternalStorageDirectory();
            // getAllFiles(root);
            return null;
        }

        /**
         * use ContentProvider to get all the files
         * it's faster than traverse through each file in SD card
         */
        private void queryFiles(long point) {
            String[] projection = new String[] {
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    //MediaStore.Files.FileColumns.MIME_TYPE
            };

            Cursor cursor = getContentResolver().query(
                    Uri.parse("content://media/external/file"),   // it contains non-media files
                    projection,
                    null,
                    null,
                    null
            );

            count = cursor.getCount();
            current = point;

            boolean flag;


            if (cursor != null) {
                if (flag = cursor.moveToPosition((int)point)) {
                    int index_id = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                    int index_data = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    int index_size = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
                    int index_name = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    //int index_mime = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
                    while (flag) {
                        if (!isStop) {
                            current++;
                            position = current;
                            publishProgress((int) ((double) current / count * 100));          // update progress par
                            progress = (int) ((double) current / count * 100);
                            String id = cursor.getString(index_id);
                            String path = cursor.getString(index_data);
                            long size = cursor.getLong(index_size);
                            String name = cursor.getString(index_name);
                            String extension = Utils.getExtensionName(name + "");
                            if (sizeMap.containsKey(name + "")) {
                                // if two files have the same name, chose the bigger one
                                if (sizeMap.get(name + "") > size) {
                                    sizeMap.put(name + "", size);
                                }
                            } else {
                                sizeMap.put(name + "", size);
                            }
                            if (extensionMap.containsKey(extension)) {
                                extensionMap.put(extension, extensionMap.get(extension) + 1);
                            } else {
                                extensionMap.put(extension, 0L);
                            }
                            totalSize += size;
                            total = totalSize;
                            flag = cursor.moveToNext();
                        } else {
                            while (isStop) {
                                if (isClose) {
                                    flag = false;
                                    return;
                                }
                            }
                        }
                    }
                    Log.i("Alisa", isClose + "");
                    cursor.close();
                    if (!isClose) {
                        current = 0;
                        position = current;
                        isFinish = true;
                        isStop = false;
                        isStart = false;

                        final double size = ( (double) totalSize) / 1024 / 1024 / 1024;     // GB
                        final DecimalFormat df = new DecimalFormat("0.00");

                        // order the sizeMap
                        List<Map.Entry<String, Long>> sizeList = new ArrayList<Map.Entry<String, Long>>(sizeMap.entrySet());
                        Collections.sort(sizeList, new Comparator<Map.Entry<String, Long>>() {
                            //desc
                            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                                return o2.getValue().compareTo(o1.getValue());
                            }
                        });

                        // order the extensionMap
                        List<Map.Entry<String, Long>> extList = new ArrayList<Map.Entry<String, Long>>(extensionMap.entrySet());
                        Collections.sort(extList, new Comparator<Map.Entry<String, Long>>() {
                            //desc
                            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                                return o2.getValue().compareTo(o1.getValue());
                            }
                        });

                        final String topSize;
                        final String avg = new DecimalFormat("0.00")
                                .format(((float) totalSize) / count / 1024);     // KB
                        final String topExt;

                        int i = 0;
                        StringBuilder sb1 = new StringBuilder();
                        for (Map.Entry<String, Long> e : sizeList) {
                            i++;
                            sb1.append(i + ".Name:" + e.getKey() + "\n   Size:"
                                    + new DecimalFormat("0.00").format(((double) e.getValue()) / 1024 / 1024 / 1024) + "GB\n");
                            if (i == 10)
                                break;
                        }

                        i = 0;
                        StringBuilder sb2 = new StringBuilder();
                        for (Map.Entry<String, Long> e : extList) {
                            i++;
                            sb2.append(i + "." + e.getKey() + "\n");
                            if (i == 5)
                                break;
                        }

                        topSize = sb1.toString();
                        topExt = sb2.toString();

                        shareContent = "Top 10 file size:\n" + topSize + "Average size:" + avg
                                + "\n5 most frequent file extension:\n" + topExt + "\n" +
                                "From scanner";

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                tv_status.setText(count + " files\n" + df.format(size) + "GB");
                                content_status = count + " files\n" + df.format(size) + "GB";
                                btn_scan.setText("Scan");
                                content_scan = "Scan";
                                btn_share.setVisibility(View.VISIBLE);
                                isShareVisible = true;
                                container_result.setVisibility(View.VISIBLE);
                                isResultVisible = true;
                                tv_top_size.setText(topSize);
                                content_size = topSize;
                                tv_avg.setText(avg + "KB");
                                content_avg = avg + "KB";
                                tv_frequent_ext.setText(topExt);
                                content_ext = topExt;
                                if (nm != null)
                                    nm.cancel(0);
                            }
                        });
                        totalSize = 0;
                        total = totalSize;
                        sizeMap.clear();
                        extensionMap.clear();
                    }
                }
            }
        }

        /**
         * get all files from sub directory with giving a path
         * @param root
         */
        private void getAllFiles(File root){
            File files[] = new File(root.getAbsolutePath()).listFiles();
            if(files != null){
                for (File f : files){
                    if(f.isDirectory()){
                        getAllFiles(f);
                    }else{
                        count = count++;
                        totalSize += f.length();
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //Log.d("progress", values[0] + "");
            arcProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    // when user press the back button, stop the scan and exit the app
    @Override
    public void onBackPressed() {
        isClose = true;
        if (isStart && !isStop) {
            btn_scan.setText("Scan");
            tv_status.setText("Stopped...");
            isStop = true;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        isClose = true;                  // close the task if is still running
        if (nm != null) {
            nm.cancel(0);
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * check if the app has permission
     * @param permissions
     * @return
     */
    public boolean hasPermission(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    public void requestPermission(int code, String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case  REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "If you denied this permission, this app will not work.", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }

    // save the status
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isStart", isStart);
        outState.putBoolean("isStop", isStop);
        outState.putBoolean("isFinish", isFinish);
        outState.putBoolean("isClose", isClose);
        outState.putBoolean("isShareVisible", isShareVisible);
        outState.putBoolean("isTextVisible", isTextVisible);
        outState.putBoolean("isResultVisible", isResultVisible);
        outState.putString("content_scan", content_scan);
        outState.putString("content_status", content_status);
        outState.putString("content_size", content_size);
        outState.putString("content_avg", content_avg);
        outState.putString("content_ext", content_ext);
        outState.putString("shareContent", shareContent);
        outState.putInt("progress", progress);
        outState.putLong("position", position);
        outState.putLong("total", total);
        outState.putSerializable("size", sizeMap);
        outState.putSerializable("ext", extensionMap);
    }

}
