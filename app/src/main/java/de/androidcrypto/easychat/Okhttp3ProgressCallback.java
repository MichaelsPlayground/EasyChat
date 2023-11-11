package de.androidcrypto.easychat;

import android.content.Context;

import java.io.File;
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

public class Okhttp3ProgressCallback {
// source: https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java
    public void run(String url, String storageFilename) throws Exception {
        Request request = new Request.Builder()
                //.url("https://publicobject.com/helloworld.txt")
                .url(url)
                .build();

        final ProgressListener progressListener = new ProgressListener() {
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
/*
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                            .build();
                })
                .build();
*/

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            //System.out.println(response.body().string());
        }

    }

    public static void main(String... args) throws Exception {
        String storageFilename = args[0];
        System.out.println("*** Okhttp3ProgressCallback args[0]: " + storageFilename);
        new Okhttp3ProgressCallback().run("https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2FMt_Cook_LC0247.jpg?alt=media&token=4db57606-0285-4ea6-b957-aaaf9e120d27", storageFilename);
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
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
