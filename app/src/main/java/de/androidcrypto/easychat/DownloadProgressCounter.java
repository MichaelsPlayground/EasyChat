package de.androidcrypto.easychat;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.database.Cursor;
import android.widget.ProgressBar;

/**
 * Fetches how many bytes have been downloaded so far and updates ProgressBar
 */
class DownloadProgressCounter extends Thread {
    private Activity activity;
    private final long downloadId;
    private final DownloadManager.Query query;
    private Cursor cursor;
    private int lastBytesDownloadedSoFar;
    private int totalBytes;
    private ProgressBar progressBar;

    public DownloadProgressCounter(Activity activity, long downloadId) {
        this.activity = activity;
        this.downloadId = downloadId;
        this.query = new DownloadManager.Query();
        query.setFilterById(this.downloadId);
    }

    @SuppressLint("Range")
    @Override
    public void run() {
        DownloadManager manager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        while (downloadId > 0) {
            try {
                Thread.sleep(300);

                cursor = manager.query(query);
                if (cursor.moveToFirst()) {

                    //get total bytes of the file
                    if (totalBytes <= 0) {
                        totalBytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    }

                    final int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                    if (bytesDownloadedSoFar == totalBytes && totalBytes > 0) {
                        this.interrupt();
                    } else {
                        //update progress bar
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(progressBar.getProgress() + (bytesDownloadedSoFar - lastBytesDownloadedSoFar));
                                lastBytesDownloadedSoFar = bytesDownloadedSoFar;
                            }
                        });
                    }

                }
                cursor.close();
            } catch (Exception e) {
                return;
            }
        }
    }

}