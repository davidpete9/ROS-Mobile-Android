package com.schneewittchen.rosandroid.ui.fragments.details;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.node.ImagePublisherNode;

public class CameraXFragment extends Fragment {
    private static final String logtag = CameraXFragment.class.getName();
    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;
    private View view = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private int rotation;
    private int screenAspectRatio;
    private DisplayManager displayManager;
    private int displayid=-1;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    //private ImageCapture imageCapture;
    private ConstraintLayout container;
    private View controls;
    //private String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private ExecutorService cameraExecutor;
    private Handler handler;
    private ImagePublisherNode talkerNode;
    private boolean isWorking=true;
    //private int count=1;
    private int width =640;
    private int height = 480;
    private String imageformat = "JPG";
    private String qosfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view==null){
            view = inflater.inflate(R.layout.fragment_camera_x, null);
            Log.d("CAMTEST","load fragment_camerax");
        }
        displayManager=(DisplayManager)requireContext().getSystemService(Context.DISPLAY_SERVICE);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isWorking", isWorking);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container=view.findViewById(R.id.container);
        previewView=view.findViewById(R.id.view_finder);
        //ViewModel
        //setting
        // load_setting();
        //background executor
        cameraExecutor= Executors.newSingleThreadExecutor();
        handler = new Handler();
        if (savedInstanceState != null) {
            Log.d("savedInstanceState","load isWorking");
            isWorking = savedInstanceState.getBoolean("isWorking");
        }
        if(talkerNode==null){
            talkerNode = ImagePublisherNode.getInstance();
        }
        //setup RenderScript
        talkerNode.setup_script(getContext(),width,height);
        talkerNode.setup_imageformat(imageformat);
        displayManager.registerDisplayListener(displayListener, null);
        previewView.post(new Runnable() {
            @Override
            public void run() {
                displayid = previewView.getDisplay().getDisplayId();
                //設定UI control
               //  updateCameraUi();
                //設定Camera
                setUpCamera();
            }
        });
    }

    /*@Override
    public void onPause() {
        super.onPause();
        if(isWorking){
            ((MainActivity) requireActivity()).getExecutor().removeNode(talkerNode);
        }
    }*/

    @Override
    public void onDestroy() {
        super.onDestroy();
        talkerNode.release_script();
        cameraExecutor.shutdown();
        displayManager.unregisterDisplayListener(displayListener);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //重新設定UI control
        // updateCameraUi();
        //TODO updateCameraSwitchButton
    }

    private void setUpCamera(){
        //Request a CameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        //Check for CameraProvider availability
        cameraProviderFuture.addListener(new Runnable(){
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    //TODO updateCameraSwitchButton
                    //Unbind use cases before rebinding
                    cameraProvider.unbindAll();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    //.setTargetAspectRatio(screenAspectRatio) .setTargetRotation(rotation) .setTargetResolution(get_size(1920,1080))
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        Log.d(logtag, "Preview aspect ratio: "+metrics.widthPixels+"X"+metrics.heightPixels);
        screenAspectRatio=aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.d(logtag, "screenAspectRatio:"+screenAspectRatio);
        rotation = previewView.getDisplay().getRotation();
        Log.d("rotation",""+rotation);
        //preview
        preview = new Preview.Builder()
                .setTargetResolution(get_size(width, height))
                .setTargetRotation(rotation)
                .build();
        //imageAnalysis
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(get_size(width, height))
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        //TODO setAnalysis
        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if(isWorking){
                    if(image.getFormat()!= ImageFormat.YUV_420_888){
                        Log.d("CAMTEST","NOTWORKING");
                        /*Log.e(logtag,"Image Format NOT Support");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(requireContext(),"Imaga not supported",Toast.LENGTH_LONG).show();
                            }
                        });
                        ((MainActivity) requireActivity()).getExecutor().removeNode(talkerNode);
                        //Drawable drawable=ContextCompat.getDrawable(requireContext(),R.drawable.ic_capture_start);
                        //controls.findViewById(R.id.camera_stream_button).setBackground(drawable);
                        controls.findViewById(R.id.camera_stream_button).setSelected(false);
                        image.close();*/
                        isWorking=false;
                    }
                    else {
                        //String aa1=requireContext().getExternalMediaDirs()[0]+"/Picture/"+ String.format(Locale.ENGLISH,"test%d.jpg", count);
                        //Log.d(logtag,aa1);
                        //File tmp = new File(requireContext().getExternalMediaDirs()[0]+"/Picture/"+ String.format(Locale.ENGLISH,"test%d.jpg", count));
                        //count=count+1;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            talkerNode.start_stream(image);
                        }
                    }
                }
                else {
                    image.close();
                }
            }
        });
        //imageCapture
        // .setTargetResolution(get_size(640, 480))
        /*imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();*/
        //Select Camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        //TODO bind imageAnalysis
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview,imageAnalysis);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    private Size get_size(int w, int h){
        if(rotation== Surface.ROTATION_0 || rotation==Surface.ROTATION_180){
            return new Size(h,w);
        }
        else return new Size(w,h);
    }

    private int aspectRatio(int w,int h){
        double previewRatio=(double)Math.max(w, h)/(double)Math.min(w, h);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        else return AspectRatio.RATIO_16_9;
    }

    private DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if(getView()!=null){
                if(displayId==CameraXFragment.this.displayid){
                    //Display display = CameraxFragment_view.getDisplay();
                    if(imageAnalysis!=null){
                        //CameraxFragment_view.gerdisplay.getre
                        imageAnalysis.setTargetRotation(getView().getDisplay().getRotation());
                    }
                    /*if(imageCapture!=null){
                        imageCapture.setTargetRotation(getView().getDisplay().getRotation());
                    }*/
                }
            }
        }
    };

    /*
    private void updateCameraUi(){
        container.removeView(container.findViewById(R.id.camera_ui_container));
        controls=View.inflate(requireContext(), R.layout.canera_ui, container);
        controls.findViewById(R.id.camera_stream_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWorking= !isWorking;
                if(isWorking){
                    Log.e(logtag,"isWorking");
                    ((MainActivity) requireActivity()).getExecutor().addNode(talkerNode);
                    //Drawable drawable=ContextCompat.getDrawable(requireContext(),R.drawable.ic_capture_stop);
                    //controls.findViewById(R.id.camera_stream_button).setBackground(drawable);
                    controls.findViewById(R.id.camera_stream_button).setSelected(true);
                }
                else {
                    Log.e(logtag,"not isWorking");
                    ((MainActivity) requireActivity()).getExecutor().removeNode(talkerNode);
                    //Drawable drawable=ContextCompat.getDrawable(requireContext(),R.drawable.ic_capture_start);
                    //controls.findViewById(R.id.camera_stream_button).setBackground(drawable);
                    controls.findViewById(R.id.camera_stream_button).setSelected(false);
                }
            }
        });
        if(isWorking){
            Log.e(logtag,"start isWorking");
            ((MainActivity) requireActivity()).getExecutor().addNode(talkerNode);
            //Drawable drawable=ContextCompat.getDrawable(requireContext(),R.drawable.ic_capture_stop);
            //controls.findViewById(R.id.camera_stream_button).setBackground(drawable);
            controls.findViewById(R.id.camera_stream_button).setSelected(true);
        }
        TextView settingbar=controls.findViewById(R.id.textView_setting);
        String text=String.format(Locale.ENGLISH,"%dX%d  %s  %s",width,height,imageformat,qosfile);
        settingbar.setText(text);
        controls.findViewById(R.id.camera_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });
    }
     */

    private List<Integer> getresolution(String type){
        List<Integer> resolutionlist=new ArrayList<>();
        switch (type){
            default:
                resolutionlist.add(640);
                resolutionlist.add(480);
                return resolutionlist;
            case "HD":
                resolutionlist.add(1280);
                resolutionlist.add(720);
                return resolutionlist;
            case "FullHD":
                resolutionlist.add(1920);
                resolutionlist.add(1080);
                return resolutionlist;
        }
    }
}
