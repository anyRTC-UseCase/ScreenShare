package org.ar.screenshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import org.ar.rtc.video.VideoCanvas

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var isScreenShare = false
    private var isJoinChannel = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        App.app.registerRtcEvent(RtcEvent())
    }

    private fun checkPermission(){
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE
        )
        permissions.forEach {
            val isOk = ActivityCompat.checkSelfPermission(this,it)
            if (isOk != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, permissions, 1)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            18->if (resultCode == Activity.RESULT_OK && data != null){
                val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                if (mediaProjection == null) {
                    Log.e(this.javaClass.name, "media projection is null")
                    return
                }
                App.app.mediaProjection = mediaProjection
                startServer()//开启屏幕共享服务
                App.app.rtcEngine.stopPreview()//停止相机采集
            }
        }
    }

    fun onClick(view: View) {
        when(view.id){
            R.id.tv_join->{
                if (isJoinChannel){
                    if (isScreenShare){
                        stopServer()
                    }
                    App.app.rtcEngine.leaveChannel()
                    rl_local_video.removeAllViews()
                    tv_join.text = "加入频道"
                    tv_join.isSelected = false
                    isScreenShare = false
                }else{
                    cameraCap()//默认打开相机采集
                    tv_join.text = "离开频道"
                    tv_join.isSelected = true
                    App.app.rtcEngine.joinChannel("","12345","","")
                }
                isJoinChannel = !isJoinChannel
            }
            R.id.tv_camera->{
                if (isScreenShare){
                    stopServer()//暂停屏幕共享
                    isScreenShare = false
                }
                App.app.rtcEngine.setExternalVideoSource(false,false,false)//不再允许外部流输入
                App.app.rtcEngine.startPreview()//开始相机采集
            }

            R.id.tv_screen->{
                App.app.rtcEngine.setExternalVideoSource(true,true,true)//设置允许外部视频流
                requestCapture()//打开屏幕录制
                isScreenShare = true
            }
        }
    }

    private fun cameraCap(){
        App.app.rtcEngine.enableVideo()
        var localVideo = RtcEngine.CreateRendererView(this);
        App.app.rtcEngine.setupLocalVideo(VideoCanvas(localVideo))
        App.app.rtcEngine.startPreview()
        rl_local_video.removeAllViews()
        rl_local_video.addView(localVideo)
    }

    private fun requestCapture() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图
            mediaProjectionManager = getSystemService(
                Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                18)
        } else {
            Toast.makeText(this, "系统版本低于5.0!", Toast.LENGTH_SHORT).show()
        }
    }

    fun startServer() {
        val intent= Intent(this,MediaReaderService::class.java)
        intent.putExtra("CMD",1)
        startService(intent)
    }

    fun stopServer(){
        val intent= Intent(this,MediaReaderService::class.java)
        intent.putExtra("CMD",2)
        startService(intent)
    }

    inner class RtcEvent:IRtcEngineEventHandler(){
        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            runOnUiThread {
                toast("加入频道成功")
            }
        }

        override fun onError(err: Int) {
            super.onError(err)
            runOnUiThread {
                toast("Error = $err")
            }
        }

        override fun onFirstRemoteVideoDecoded(
            uid: String?,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            runOnUiThread {
                val textureView = RtcEngine.CreateRendererView(this@MainActivity)
                rl_remote_video.removeAllViews()
                rl_remote_video.addView(textureView)
                App.app.rtcEngine.setupRemoteVideo(VideoCanvas(textureView,Constants.RENDER_MODE_FIT,uid))
            }

        }

        override fun onUserOffline(uid: String?, reason: Int) {
            super.onUserOffline(uid, reason)
            runOnUiThread {
                rl_remote_video.removeAllViews()
            }
        }
    }

    fun Activity.toast(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isScreenShare){
            stopServer()
        }
        RtcEngine.destroy()
    }
}