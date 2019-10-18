package com.windimg.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.windimg.camera.databinding.ActivityMainBinding;
import com.windimg.camera.utils.camera.CaptureStrategy;
import com.windimg.camera.utils.camera.MediaStoreCompat;
import com.windimg.camera.utils.permission.PermissionListener;
import com.windimg.camera.utils.permission.PermissionRequest;
import com.windimg.camera.utils.permission.PermissionUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_CAPTURE = 24;
    private static final int REQUEST_CODE_PHOTO = 23;
    private static final int REQUEST_CODE_CLIP = 22;

    private ActivityMainBinding binding;
    private MediaStoreCompat compat;
    private PermissionRequest permissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setOnClickListener(this);

        permissionRequest = new PermissionRequest(this);
        compat = new MediaStoreCompat(this);
        compat.setCaptureStrategy(new CaptureStrategy(true, getPackageName() + ".fileProvider", "test"));
    }

    @Override
    public void onClick(View v) {
        requestPermission(v);
    }


    private void requestPermission(final View v) {
        permissionRequest.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, new PermissionListener() {
            @Override
            public void onGranted() {
                switch (v.getId()) {
                    case R.id.btn_photo:
                        compat.dispatchPhotosIntent(REQUEST_CODE_PHOTO);
                        break;
                    case R.id.btn_camera:
                        compat.dispatchCaptureIntent(MainActivity.this, REQUEST_CODE_CAPTURE);
                        break;
                }
            }

            @Override
            public void onDenied(List<String> deniedPermission) {

            }

            @Override
            public void onShouldShowRationale(List<String> deniedPermission) {
                PermissionUtils.goManage(MainActivity.this);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        switch (requestCode) {
            case REQUEST_CODE_CAPTURE:
                compat.startPhotoZoom(compat.getCurrentPhotoUri(), REQUEST_CODE_CLIP);
                break;
            case REQUEST_CODE_PHOTO:
                if (data != null) compat.startPhotoZoom(data.getData(), REQUEST_CODE_CLIP);
                break;
            case REQUEST_CODE_CLIP:
                if (compat.getCurrentClipFile() != null) {
                    Toast.makeText(this, compat.getCurrentClipFile().getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
