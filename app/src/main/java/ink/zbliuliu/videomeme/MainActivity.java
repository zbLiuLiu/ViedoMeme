package ink.zbliuliu.videomeme;


import android.Manifest;
import android.content.Context;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.AppBarLayout;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private IjkMediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Toolbar toolbar;
    private Timer timer;
    private TimerTask timerTask;
    private String videoPath;
    private boolean isSeekbarOnDragging;
    private static int REQUEST_CODE = 23333;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private int surfaceWidth,surfaceHeight;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {

            }
        };
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            checkPermission();
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        BottomNavigationView bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(false);
                switch (item.getItemId()){
                    case R.id.open:
                        openNewVideo();
                        break;
                    case R.id.play:
                        if(mediaPlayer!=null) play();
                        break;
                    case R.id.screenscan:
                        break;
                    default:
                }
                return false;
            }
        });

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        seekBar = (SeekBar)findViewById(R.id.seekBar2);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekbarOnDragging=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int nowProgress = seekBar.getProgress();
                Toast.makeText(MainActivity.this,String.valueOf(nowProgress),Toast.LENGTH_LONG).show();
                mediaPlayer.seekTo(nowProgress);
            }
        });
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toogleUI();
            }
        });
        surfaceHolder = surfaceView.getHolder();
        View appbar = findViewById(R.id.appbar);
        appbar.setPadding(appbar.getPaddingLeft(),getStatusBarHeight()+appbar.getPaddingTop(),appbar.getPaddingRight(),appbar.getPaddingBottom());
        View controlor = findViewById(R.id.bottomControl);
        controlor.setPadding(controlor.getPaddingLeft(),controlor.getPaddingTop(),controlor.getPaddingRight(),(checkDeviceHasNavigationBar(MainActivity.this))?getNavigationBarHeight()+controlor.getPaddingBottom():controlor.getPaddingBottom());



        surfaceWidth = surfaceView.getWidth();
        surfaceHeight = surfaceView.getHeight();


    }

    private void toogleUI(){
        AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbar);
        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.bottomControl);
        if(appBarLayout.getVisibility()==View.VISIBLE&&linearLayout.getVisibility()==View.VISIBLE){
            appBarLayout.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }else {
            appBarLayout.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.VISIBLE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(lp);
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
    private void openNewVideo(){
        timer.cancel();
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i,REQUEST_CODE);

    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && null != data) {
            Uri selectedVideo = data.getData();
            String[] filePathColumn = {MediaStore.Video.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedVideo, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            videoPath = cursor.getString(columnIndex);
            cursor.close();
            Toast.makeText(MainActivity.this,videoPath,Toast.LENGTH_LONG).show();
        }else {
            toolbar.setTitle("23333");
            toolbar.setSubtitle("");
            return;
        }
        //Toast.makeText(this, "111", Toast.LENGTH_LONG).show();
        File file = new File(videoPath.trim());
        if (!file.exists()) {
            Toast.makeText(this, "视频文件路径错误", Toast.LENGTH_LONG).show();
            toolbar.setTitle("23333");
            toolbar.setSubtitle("");
            return;
        }
        toolbar.setTitle(file.getName());
        toolbar.setSubtitle(file.getPath());
        try{
            surfaceView.setLayoutParams(new ConstraintLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
            mediaPlayer = new IjkMediaPlayer();
            mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,"enable-accurate-seek",1);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(file.getAbsolutePath());
            surfaceHolder.addCallback(new SurfaceCallBack());
            mediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer mp) {
                    mediaPlayer.setDisplay(surfaceHolder);
                    mediaPlayer.start();
                    Toast.makeText(MainActivity.this,"start to play",Toast.LENGTH_LONG).show();
                    mediaPlayer.setLooping(true);

                    seekBar.setMax((int)mediaPlayer.getDuration());
                    timer = new Timer();
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            //TODO 判断seekbar是否正在被拖动
                            if(mediaPlayer!=null&&!isSeekbarOnDragging) seekBar.setProgress(
                                    (int)mediaPlayer.getCurrentPosition()
                            );
                        }
                    };
                    timer.schedule(timerTask,1,1);
                    mediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(IMediaPlayer mp) {
                            mediaPlayer.release();
                            timer.cancel();
                            timerTask.cancel();
                        }
                    });
                    mediaPlayer.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(IMediaPlayer mp) {
                            isSeekbarOnDragging=false;
                        }
                    });
                }
            });
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {
                    changeVideoSize();
                }

            });
            mediaPlayer.prepareAsync();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private class SurfaceCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            play();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private void checkPermission() {
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
        }
        openNewVideo();
    }


    private void play(){
        BottomNavigationView bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottomNavigationView);
        BottomNavigationItemView bottomNavigationItemView = (BottomNavigationItemView)bottomNavigationView.getChildAt(0);
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
//            bottomNavigationItemView.setIcon(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        }
        else {
            mediaPlayer.start();
//            bottomNavigationItemView.setIcon(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
        }
    }
    public void changeVideoSize() {
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();

        //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        float max;
        if (getResources().getConfiguration().orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //竖屏模式下按视频宽度计算放大倍数值
            max = Math.max((float) videoWidth / (float) surfaceView.getWidth(),(float) videoHeight / (float) surfaceView.getHeight());
        } else{
            //横屏模式下按视频高度计算放大倍数值
            max = Math.max((float) videoWidth/(float) surfaceView.getHeight(),(float) videoHeight/(float) surfaceView.getWidth());
        }

        //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
        videoWidth = (int) Math.ceil((float) videoWidth / max);
        videoHeight = (int) Math.ceil((float) videoHeight / max);

        //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) surfaceView.getLayoutParams();
        layoutParams.width=videoWidth;
        layoutParams.height=videoHeight;
        layoutParams.constrainedWidth=true;
        layoutParams.constrainedHeight=true;
        layoutParams.topToTop=ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.bottomToBottom=ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.leftToLeft=ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.rightToRight=ConstraintLayout.LayoutParams.PARENT_ID;

        surfaceView.setLayoutParams(layoutParams);

    }

    private int getStatusBarHeight() {
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen","android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Status height:" + height);
        return height;
    }

    private int getNavigationBarHeight() {
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height","dimen", "android");
        int height = resources.getDimensionPixelSize(resourceId);
        Log.v("dbw", "Navi height:" + height);
        return height;
    }

    public static boolean checkDeviceHasNavigationBar(Context context) {
        boolean hasNavigationBar = false;
        Resources rs = context.getResources();
        int id = rs.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id);
        }
        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception e) {

        }
        return hasNavigationBar;

    }

}
