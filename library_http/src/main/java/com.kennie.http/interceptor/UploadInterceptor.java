package com.kennie.http.interceptor;

import com.kennie.http.callbacks.TransmitCallback;
import com.kennie.http.models.ProgressUploadBody;

import java.io.IOException;

import io.reactivex.rxjava3.annotations.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * <p>
 * Description :进度监听拦截器
 */
public class UploadInterceptor implements Interceptor {

    private TransmitCallback mCallback;

    public UploadInterceptor(@NonNull TransmitCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        ProgressUploadBody requestBody = new ProgressUploadBody(request.body(), mCallback);
        Request finalRequest = request.newBuilder()
                .addHeader("Connection", "alive")
                .method(request.method(), requestBody)
                .build();
        return chain.proceed(finalRequest);
    }
}
