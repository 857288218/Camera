/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.windimg.camera.utils.camera;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.os.EnvironmentCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaStoreCompat {

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> fragment;
    private CaptureStrategy captureStrategy;
    //选中的图片地址
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    //裁剪后图片地址
    private Uri currentClipUri;
    private File currentClipFile;

    public MediaStoreCompat(Activity activity) {
        mContext = new WeakReference<>(activity);
        fragment = null;
    }

    public MediaStoreCompat(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        this.fragment = new WeakReference<>(fragment);
    }

    /**
     * Checks whether the device has a camera feature or not.
     *
     * @param context a context to check for camera feature.
     * @return true if the device has a camera feature. false otherwise.
     */
    public static boolean hasCameraFeature(Context context) {
        PackageManager pm = context.getApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void setCaptureStrategy(CaptureStrategy strategy) {
        captureStrategy = strategy;
    }

    /**
     * 调起相机
     */
    public void dispatchCaptureIntent(Context context, int requestCode) {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(context.getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoPath = photoFile.getAbsolutePath();
                currentPhotoUri = FileProvider.getUriForFile(mContext.get(),
                        captureStrategy.authority, photoFile);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(requestCode, captureIntent);
            }
        }
    }

    /**
     * 调起相册
     */
    public void dispatchPhotosIntent(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(requestCode, intent);
    }

    public void startPhotoZoom(Uri uri, int request) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 1000);
        intent.putExtra("outputY", 1000);
        intent.putExtra("scale", true);
//        intent.putExtra("circleCrop", true);//圆形裁剪区域
        currentClipFile = createImageFile();
        if (currentClipFile != null) {
            currentClipUri = Uri.fromFile(currentClipFile);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri(MediaStore.EXTRA_OUTPUT, uri));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentClipUri);
            intent.putExtra("return-data", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            startActivityForResult(request, intent);
        }
    }

    private void startActivityForResult(int request, Intent intent) {
        if (fragment != null) {
            fragment.get().startActivityForResult(intent, request);
        } else {
            mContext.get().startActivityForResult(intent, request);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File createImageFile() {
        // Create an image file name
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = String.format("JPEG_%s.jpg", timeStamp);
        File storageDir;
        if (captureStrategy.isPublic) {
            storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            if (!storageDir.exists()) storageDir.mkdirs();
        } else {
            storageDir = mContext.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        if (captureStrategy.directory != null) {
            storageDir = new File(storageDir, captureStrategy.directory);
            if (!storageDir.exists()) storageDir.mkdirs();
        }

        // Avoid joining path components manually
        File tempFile = new File(storageDir, imageFileName);

        // Handle the situation that user's external storage is not ready
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }

        return tempFile;
    }

    public Uri getCurrentClipUri() {
        return currentClipUri;
    }

    public File getCurrentClipFile() {
        return currentClipFile;
    }

    public Uri getCurrentPhotoUri() {
        return currentPhotoUri;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }
}
