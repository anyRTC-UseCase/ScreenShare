package org.ar.screenshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import org.ar.rtc.VideoEncoderConfiguration
import org.ar.rtc.video.ARVideoFrame
import org.ar.rtc.video.VideoCanvas
import org.loka.screensharekit.ScreenShareKit
import org.loka.screensharekit.callback.H264CallBack
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {


    private var isJoinChannel = false
    private var isJoinSuccess = false
    private val rtcEngine by lazy { RtcEngine.create(this,Config.appId,RtcEvent()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rtcEngine.enableVideo()
        rtcEngine.setExternalVideoSource(true,true,true)
        rtcEngine.setVideoEncoderConfiguration(
            VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720)
        )

    }

    fun onClick(view: View) {
        when(view.id){
            R.id.tv_join->{
                if (isJoinChannel){
                    ScreenShareKit.stop()
                    rtcEngine.leaveChannel()
                    isJoinSuccess =false
                    tv_join.text = "加入频道"
                    tv_join.isSelected = false
                }else{
                    requestCapture()//打开屏幕录制
                    tv_join.text = "离开频道"
                    tv_join.isSelected = true
                    rtcEngine.joinChannel("","12345","","")
                }
                isJoinChannel = !isJoinChannel
            }
        }
    }


    private fun requestCapture() {
       ScreenShareKit.init(this).onH264({ buffer, isKeyFrame, ts ->
           if (isJoinSuccess) {
               rtcEngine.pushExternalVideoFrame(ARVideoFrame().apply {
                   val array = ByteArray(buffer.remaining())
                   buffer.get(array)
                   bufType = ARVideoFrame.BUFFER_TYPE_H264
                   format = if (isKeyFrame){ARVideoFrame.FORMAT_VIDEO_KEY_FRAME}else{ARVideoFrame.FORMAT_VIDEO_NOR_FRAME}
                   timeStamp = ts
                   buf = array
                   height = Resources.getSystem().displayMetrics.heightPixels
                   stride = Resources.getSystem().displayMetrics.widthPixels
               })
           }
       }).start()
    }


    inner class RtcEvent:IRtcEngineEventHandler(){
        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            runOnUiThread {
                isJoinSuccess = true
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenShareKit.stop()
        RtcEngine.destroy()
    }
}