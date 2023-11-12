package de.androidcrypto.easychat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class Okhttp3ProgressDownloader_v1 {

    private String downloadUrl;
    private String storageFilename;
    private LinearProgressIndicator progressIndicator;

    public Okhttp3ProgressDownloader_v1(String downloadUrl, String storageFilename, LinearProgressIndicator progressIndicator) {
        this.downloadUrl = downloadUrl;
        this.storageFilename = storageFilename;
        this.progressIndicator = progressIndicator;
        /*
        try {
            run(downloadUrl, storageFilename);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }

         */
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                //.url("https://publicobject.com/helloworld.txt")
                .url(downloadUrl)
                .build();

        final Okhttp3ProgressCallback.ProgressListener progressListener = new Okhttp3ProgressCallback.ProgressListener() {
            boolean firstUpdate = true;

            @Override public void update(long bytesRead, long contentLength, boolean done) {
                if (done) {
                    System.out.println("completed");
                } else {
                    if (firstUpdate) {
                        firstUpdate = false;
                        if (contentLength == -1) {
                            System.out.println("content-length: unknown");
                        } else {
                            System.out.format("content-length: %d\n", contentLength);
                        }
                    }
                    System.out.println(bytesRead);
                    if (contentLength != -1) {
                        System.out.format("%d%% done\n", (100 * bytesRead) / contentLength);
                        progressIndicator.setProgress((int) ((100 * bytesRead) / contentLength));
                    }
                }
            }
        };

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to download file: " + response);
                } else {
                    //Log.d(TAG, "Success");
                    System.out.println("SUCCESS");
                }
                FileOutputStream fos = new FileOutputStream(storageFilename);
                fos.write(response.body().bytes());
                fos.close();
            }
        });

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            //System.out.println(response.body().string());
        }
    }

    private class ProgressResponseBody extends ResponseBody {
        private final ResponseBody responseBody;
        private final Okhttp3ProgressCallback.ProgressListener progressListener;
        private BufferedSource bufferedSource;
        ProgressResponseBody(ResponseBody responseBody, Okhttp3ProgressCallback.ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @Override public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

}
