package org.ar.screenshare

import android.media.projection.MediaProjection
import android.util.Log
import org.ar.rtc.video.ARVideoFrame

/**
 * 采集并发送数据到rtc
 */
class VideoCapturer(mp: MediaProjection,
                    var width: Int, var height: Int,
                    var videoBitrate: Int, var videoFrameRate: Int
) : H264Encoder.EncoderListener {
    val TAG = VideoCapturer::class.java.name
    var mediaReader: MediaReader = MediaReader(width, height, videoBitrate,
            videoFrameRate, this, mp)


    override fun onH264(buffer: ByteArray, type: Int, ts: Long) {
        val datas = ByteArray(buffer.size)
        System.arraycopy(buffer, 0, datas, 0, buffer.size)
        val videoFrame = ARVideoFrame()
        videoFrame.bufType = ARVideoFrame.BUFFER_TYPE_H264
        videoFrame.format = if (type ==1){ARVideoFrame.FORMAT_VIDEO_KEY_FRAME}else{ARVideoFrame.FORMAT_VIDEO_NOR_FRAME}
        videoFrame.buf=datas
        videoFrame.height=height
        App.app.rtcEngine.pushExternalVideoFrame(videoFrame)//发送屏幕共享流
    }

    override fun onError(t: Throwable) {

    }

    fun exit() {
        Log.d(TAG, "正在退出")
        mediaReader.exit()
    }

    override fun onCloseH264() {
        Log.d(TAG, "退出完成")
    }

    init {
        mediaReader.init()
        mediaReader.start()
    }
}