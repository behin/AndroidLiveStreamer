package edu.sharif.behin.androidstreamer.multimedia;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int previewWidth=640;
    public static final int previewHeight=480;
    Camera camera;
    SurfaceHolder surfaceHolder;
    VideoEncoder encoder;
    boolean isBackFacing;

    public CameraPreview(Context context) {
        super(context);
        setFields();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFields();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFields();
    }

    public Camera getCurrentCamera() {
        return camera;
    }

    private void setFields() {
        camera = Camera.open();
        //Todo: sensitivity
        isBackFacing = true;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        swapCamera();

    }

    void refreshCamera()
    {
        try {

            // set preview size and make any resize, rotate or
            // reformatting changes her
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    bytes = YV12toYUV420PackedSemiPlanar(bytes);
                    if(isBackFacing){
                        bytes = rotateYUV420Degree90(bytes,previewWidth,previewHeight);
                    }else {
                        bytes = rotateYUV420Degree270(bytes,previewWidth,previewHeight);
                    }
                    if(encoder!=null){

                        encoder.offerEncoder(bytes);
                    }
                }
            });
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.YV12);
            //Exchange for Rotation
//            List<Camera.Size> sizes =  parameters.getSupportedPreviewSizes();
//            int minDiff = VideoEncoder.VIDEO_PIXEL_COUNT*100;
//            int loc=0;
//            for(int i=0;i<sizes.size();i++){
//                int curSize = sizes.get(i).width*sizes.get(i).height;
//                if(Math.abs(curSize-VideoEncoder.VIDEO_PIXEL_COUNT)<minDiff){
//                    minDiff = Math.abs(curSize-VideoEncoder.VIDEO_PIXEL_COUNT);
//                    loc=i;
//                }
//            }
//            previewWidth  = sizes.get(loc).width;
//            previewHeight = sizes.get(loc).height;
            parameters.setPreviewSize(previewWidth, previewHeight);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(CameraPreview.class.getName(), "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
//        The Surface has been created, now tell the camera where to draw the preview.
        refreshCamera();
    }

    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
    {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // _startPoint preview with new settings
        refreshCamera();
    }

    public void swapCamera()
    {
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        stop();

        for (int i = 0; i < cameraCount; i++)
        {
            Camera.getCameraInfo(i,cameraInfo);
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && isBackFacing)
            {
                try
                {
                    camera = Camera.open(i);

                }catch (RuntimeException e)
                {
                    Log.e("Error","Camera failed to open: " + e.getLocalizedMessage());
                }
            }

            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK && !isBackFacing)
            {
                try
                {
                    camera = Camera.open(i);
                }catch (RuntimeException e)
                {
                    Log.e("Error","Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        isBackFacing = !isBackFacing;
        refreshCamera();
    }

    public void setEncoder(VideoEncoder encoder) {
        this.encoder = encoder;
    }

    private static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = imageWidth-1;x >= 0 ;x--)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = 0; x< imageWidth;x=x+2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x+1)];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;

            }
        }
        return yuv;
    }


    public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input) {
    /*
     * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
     * We convert by putting the corresponding U and V bytes together (interleaved).
     */
        byte[] output = new byte[input.length];
        int vidPixelCount = previewHeight*previewWidth;
        int vidPixelQuarterCount = vidPixelCount/4;

        System.arraycopy(input, 0, output, 0, vidPixelCount);
        // Y

        for (int i = 0; i < vidPixelQuarterCount; i++) {
            output[vidPixelCount  + i*2] = input[vidPixelCount + i + vidPixelQuarterCount]; // Cb (U)
            output[vidPixelCount + i*2 + 1] = input[vidPixelCount + i]; // Cr (V)
        }
        return output;
    }

    public void restart() {
        isBackFacing = !isBackFacing;
        swapCamera();
    }

    public void stop() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}