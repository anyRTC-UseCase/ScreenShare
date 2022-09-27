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
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import org.ar.rtc.VideoEncoderConfiguration
import org.ar.rtc.video.ARVideoFrame
import org.ar.rtc.video.VideoCanvas
import org.ar.screenshare.databinding.ActivityMainBinding
import org.loka.screensharekit.ScreenShareKit
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {


    private var isJoinChannel = false
    private var isJoinSuccess = false
    private val rtcEngine by lazy { RtcEngine.create(this,Config.appId,RtcEvent()) }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        rtcEngine.enableVideo()
        rtcEngine.setExternalVideoSource(true,true,true)

        binding.run {
            tvJoin.setOnClickListener({
                if (isJoinChannel){
                    ScreenShareKit.stop()
                    rtcEngine.leaveChannel()
                    isJoinSuccess =false
                    tvJoin.text = "加入频道"
                    tvJoin.isSelected = false
                }else{
                    requestCapture()//打开屏幕录制
                    tvJoin.text = "离开频道"
                    tvJoin.isSelected = true
                    rtcEngine.joinChannel("","12345","","")
                }
                isJoinChannel = !isJoinChannel
            })
        }
    }



    private fun requestCapture() {
       ScreenShareKit.init(this).onH264({ buffer, isKeyFrame, w, h, ts ->
           if (isJoinSuccess) {
               rtcEngine.pushExternalVideoFrame(ARVideoFrame().apply {
                   val array = ByteArray(buffer.remaining())
                   buffer.get(array)
                   bufType = ARVideoFrame.BUFFER_TYPE_H264
                   format = if (isKeyFrame){ARVideoFrame.FORMAT_VIDEO_KEY_FRAME}else{ARVideoFrame.FORMAT_VIDEO_NOR_FRAME}
                   timeStamp = ts
                   buf = array
                   height = h
                   stride = w
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