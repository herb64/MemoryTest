package de.herb64.memtesting;

import android.app.ActivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity{
    private static final int MIB = 1024 * 1024;
    private TextView mOutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memClass = activityManager.getMemoryClass();
        mOutView = (TextView) findViewById(R.id.tv_main);
        mOutView.setText(Build.BRAND + " " + Build.MODEL + "\n" +
                "Release: " + Build.VERSION.RELEASE +
                " (SDK "+ Build.VERSION.SDK_INT + ")\n" +
                "Memory Class: " + memClass + "\n");
        // Run tests in asynctask to avoid "too much work in main thread" conditions and to allow
        // updates of textview after each iteration
        new testerTask().execute(memClass/2);       // start with half of memoryclass value
    }

    private class testerTask extends AsyncTask<Integer, Integer, Void> {
        private int maxSuccess = 0;

        // RECURSIVE test function for memory allocations
        private void runTest(int size, int step) {
            int result = makeBigData(size*MIB);     // used as factor for increase/decrease
            if (size > maxSuccess && result == 1) {
                maxSuccess = size;
            }
            publishProgress(size, result);
            if (step > 1) {                         // stopping here, not going to the last byte
                runTest(size + step * result, step / 2);
            }
        }

        // Allocate a byte array of given size in bytes and fill with data to show in memory monitor
        // Despite many discussions on catching OOM - I can catch them consistently w/o crash
        private int makeBigData(int size) {
            try {
                byte[] bigData = new byte[size];    // Alloc with new already sufficient for OOM
                for (int i = 0; i < size; i+=10) {  // Fill of data needed to actually show
                    bigData[i] = 0;                 // up in memory monitor - (this could be
                }                                   // theoretically skipped...)
                return 1;                           // return +1 on successful allocation
            } catch (OutOfMemoryError e) {          // e.toString() - to be logged in logcat
                return -1;                          // return -1 on OOM condition
            }
        }

        @Override
        protected Void doInBackground(Integer... params) {
            runTest(params[0], params[0]/2);        // RECURSIVE test function
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mOutView.append("Finished. Max successful allocation: " + maxSuccess + "MiB");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            String status = "OK\n";
            if (values[1] == -1) {
                status = "Failed\n";
            }
            mOutView.append("Alloc " + values[0] + "MiB: " + status);
        }
    }
}
