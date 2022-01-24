package com.kennie.http.callbacks;

/**
 * <p>
 * Description : 上传和下载请求回调
 */
public interface TransmitCallback {

    /**
     * 读写完成
     */
    void done(boolean success);


    /**
     * 上传/下载失败
     *
     * @param e 异常类
     */
    void onFailed(Exception e);

    /**
     * 上传/下载进度
     *
     * @param progress 进度 % 单位
     */
    void inProgress(int progress);
}
