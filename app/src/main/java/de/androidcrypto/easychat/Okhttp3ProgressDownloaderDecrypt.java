package de.androidcrypto.easychat;

import android.content.Context;
import android.net.Uri;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

public class Okhttp3ProgressDownloaderDecrypt {

    private String downloadUrl;
    private LinearProgressIndicator progressIndicator;
    private Context context;
    private Uri storageUri;
    private char[] password;
    private int iterations;

    public Okhttp3ProgressDownloaderDecrypt(String downloadUrl, LinearProgressIndicator progressIndicator, Context context, Uri storageUri, char[] password, int iterations) {
        this.downloadUrl = downloadUrl;
        this.progressIndicator = progressIndicator;
        this.context = context;
        this.storageUri = storageUri;
        this.password = password;
        this.iterations = iterations;
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                .url(downloadUrl)
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
                        //progressIndicator.setProgress((int) ((100 * bytesRead) / contentLength));
                        progressIndicator.setProgressCompat((int) ((100 * bytesRead) / contentLength), true);
                        //progressIndicator.setProgress((int) ((100 * bytesRead) / contentLength), true);
/*
                        Handler progressBarHandler = new Handler();
                        progressBarHandler .post(new Runnable() {
                            public void run() {
                                progressIndicator.setProgress((int) ((100 * bytesRead) / contentLength), true);
                            }
                        });
*/
                    }
                }
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                            .build();
                })
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            //System.out.println(response.body().string());
            /*
            // this is using the complete data in bytes()
            FileOutputStream fos = new FileOutputStream(storageFilename);
            fos.write(response.body().bytes());
            fos.close();
*/
            // this is using a stream
            InputStream is = response.body().byteStream();
            boolean success = Cryptography.decryptGcmFileBufferedCipherInputStreamFromInputStream(context, is, storageUri, password, iterations);
            System.out.println("decryption success: " + success);
        }

    }

    private class ProgressResponseBody extends ResponseBody {
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
