package com.schneewittchen.rosandroid.model.repositories.rosRepo.node;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;

import com.schneewittchen.rosandroid.model.entities.widgets.BaseEntity;
import com.schneewittchen.rosandroid.model.entities.widgets.PublisherLayerEntity;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.internal.message.Message;
import org.ros.internal.message.MessageBuffers;
import org.ros.internal.message.RawMessage;
import org.ros.message.Time;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import std_msgs.Bool;
import std_msgs.Header;

public class ImagePublisherNode extends PubNode {

    private static ImagePublisherNode instance = null;
    private static int FRAME_ID = 0;
    private static String TAG = ImagePublisherNode.class.getName();
    private static final int TEXTTYPE = 1;
    private static final int IMAGETYPE = 2;
    private static int SEQ = 0;
    private int count=0;

    private long lastPublishedTimestamp = 0;
    private RenderScript rs;

    private int FPS = 5;
    private Allocation yuvAllocation;
    private Allocation rgbAllocation;
    private ScriptIntrinsicYuvToRGB scriptYuvToRgb;
    private Bitmap.CompressFormat compressFormat;

    private ImagePublisherNode() {
        super();
        Log.d("CAMTEST","INIT");
    };

    public static ImagePublisherNode getInstance() {
        if (instance == null) {
            instance = new ImagePublisherNode();
            return instance;
        }
        return instance;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(ImageProxy image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }


    //Compressed image
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start_stream(ImageProxy image){
        //image Width and Height
        int w = image.getWidth();
        int h = image.getHeight();
        ChannelBufferOutputStream stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        byte[] data = NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100);
        try {
            stream.write(data);
        } catch (Exception e) {
            Log.e("CAMTEST","STream write error");
        }
        if (this.publisher != null) {
            long t = System.currentTimeMillis();
            if (t-this.lastPublishedTimestamp > (int)(1000/this.FPS)) {
                CompressedImage msg = (CompressedImage) publisher.newMessage();
                msg.setFormat("jpeg");
                msg.getHeader().setFrameId("camera");
                msg.getHeader().setSeq(FRAME_ID++);
                Time stamp = new Time((int) System.currentTimeMillis(), (int) (System.nanoTime() % 1000000000));
                Log.d("CAMTEST",""+stamp.nsecs);
                msg.getHeader().setStamp(stamp);
                // Convert the byte array to a ByteBuffer
                msg.setData(stream.buffer().copy());

                // Image message = (Image) publisher.newMessage();
                publisher.publish(msg);
                this.lastPublishedTimestamp = t;

                Log.i("CAMTEST", "Image published " + FRAME_ID);
            }
        }

        image.close();
    }

    //Uncompressed image => very slow
    public void start_stream_yuv(ImageProxy image) {
        int w = image.getWidth();
        int h = image.getHeight();



        //buffer
        ByteBuffer Wbuffer=ByteBuffer.allocate(4).putInt(w);
        ByteBuffer Hbuffer=ByteBuffer.allocate(4).putInt(h);
        ByteBuffer Ybuffer=image.getPlanes()[0].getBuffer();
        ByteBuffer Vbuffer=image.getPlanes()[2].getBuffer();

        //buffer size
        int Wr=Wbuffer.rewind().remaining();
        int Hr=Hbuffer.rewind().remaining();
        int Yr=Ybuffer.remaining();
        int Vr=Vbuffer.remaining();

        //buffer into a byte array
        byte[] nv21 = new byte[Wr + Hr + Yr + Vr];
        Wbuffer.get(nv21, 0, Wr);
        Hbuffer.get(nv21, Wr, Hr);
        Ybuffer.get(nv21, Wr + Hr, Yr);
        Vbuffer.get(nv21, Wr + Hr +Yr, Vr);

        List<Byte> img_list = new ArrayList<>();
        for (byte item : nv21) {
            img_list.add(item);
        }
        img_list.add(nv21[0]);//307200+153599+1

        Log.i("CAMTEST", "AAAAAAHHHHHH");

        if (this.publisher != null) {
            CompressedImage msg = (CompressedImage) publisher.newMessage();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //msg.setHeader(new ImageHeader());
            }
            msg.setFormat("jpg");
            // msg.setData();

            // msg.setHeight(image.getHeight());
            // msg.setWidth(image.getWidth());
            //msg.setEncoding();
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             //   msg.setHeader(new ImageHeader());
           // }
            // Convert the byte array to a ByteBuffer
            publisher.publish(msg);
            Log.i("CAMTEST", "Image publushed");
        }

        image.close();
    }


    private Bitmap rotate_bitmap(Bitmap img,int w,int h,int rotate){
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(img, 0, 0, w, h, matrix, true);
    }

    //ScriptIntrinsicYuvToRGB
    public void setup_script(Context context,int w,int h){
        rs = RenderScript.create(context);
        int len=(w*h)+(w*h/2-1)+(w*h/2-1);
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(len);
        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(w)
                .setY(h);
        yuvAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        rgbAllocation = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.YUV(rs));
    }

    public void release_script(){
        yuvAllocation.destroy();
        rgbAllocation.destroy();
        rs.destroy();
    }

    public void setup_imageformat(String imageformat){
        switch (imageformat){
            case "PNG":
                compressFormat=Bitmap.CompressFormat.PNG;
                break;
            case "JPG":
                compressFormat=Bitmap.CompressFormat.JPEG;
                break;
        }
    }


}
