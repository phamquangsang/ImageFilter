package org.wysaid.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGEFrameRenderer;
import org.wysaid.texUtils.TextureRenderer;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by wangyang on 15/11/26.
 */

public class VideoPlayerGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public static final String LOG_TAG = Common.LOG_TAG;

    private OnCreateCallback mOnCreateCallback;
    private SurfaceTexture mSurfaceTexture;
    private int mVideoTextureID;
    private CGEFrameRenderer mFrameRenderer;
    private TextureRenderer.Viewport mRenderViewport = new TextureRenderer.Viewport();

    private boolean mFitFullView = false;
    private int mVideoWidth = 1000;
    private int mVideoHeight = 1000;
    private float mMaskAspectRatio = 1.0f;
    private int mViewWidth = 1000;
    private int mViewHeight = 1000;
    private float[] mTransformMatrix = new float[16];
    private boolean mIsUsingMask = false;
    private boolean mHandleCurrentFrame = false;
    private String lastConfig = "";

    public VideoPlayerGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(LOG_TAG, "MyGLSurfaceView Construct...");
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 8, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setZOrderOnTop(true);
        Log.i(LOG_TAG, "MyGLSurfaceView Construct OK...");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(LOG_TAG, "video player onSurfaceCreated...");

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);

        createSurfaceTextureInternal();

        if (mOnCreateCallback != null) {
            mOnCreateCallback.createOK();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        mViewWidth = width;
        mViewHeight = height;

        calcViewport();
    }

    public void release() {
        Log.i(LOG_TAG, "Video player view release...");

        //must be in the OpenGL thread!
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG, "Video player view release run...");
                if (mFrameRenderer != null) {
                    mFrameRenderer.release();
                    mFrameRenderer = null;
                }

                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                    mSurfaceTexture = null;
                }

                if (mVideoTextureID != 0) {
                    GLES20.glDeleteTextures(1, new int[]{mVideoTextureID}, 0);
                    mVideoTextureID = 0;
                }
                mIsUsingMask = false;
                Log.i(LOG_TAG, "Video player view release OK");
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "surfaceview onPause ...");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture == null || mFrameRenderer == null) {
            return;
        }
        mSurfaceTexture.updateTexImage();

        if (!mHandleCurrentFrame) {
            return;
        }

        mSurfaceTexture.getTransformMatrix(mTransformMatrix);
        mFrameRenderer.update(mVideoTextureID, mTransformMatrix);
        mFrameRenderer.runProc();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_BLEND);
        mFrameRenderer.render(mRenderViewport.x, mRenderViewport.y, mRenderViewport.width, mRenderViewport.height);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private long mTimeCount2 = 0;
    private long mFramesCount2 = 0;
    private long mLastTimestamp2 = 0;

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();

        if (mLastTimestamp2 == 0)
            mLastTimestamp2 = System.currentTimeMillis();

        long currentTimestamp = System.currentTimeMillis();

        ++mFramesCount2;
        mTimeCount2 += currentTimestamp - mLastTimestamp2;
        mLastTimestamp2 = currentTimestamp;
        if (mTimeCount2 >= 1e3) {
            Log.i(LOG_TAG, String.format("播放帧率: %d", mFramesCount2));
            mTimeCount2 -= 1e3;
            mFramesCount2 = 0;
        }
    }

    public void createSurfaceTexture() {
        if (mFrameRenderer != null) {
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    createSurfaceTextureInternal();
                }
            });
        }
    }

    private void createSurfaceTextureInternal() {
        if (mSurfaceTexture == null || mVideoTextureID == 0) {
            mVideoTextureID = Common.genSurfaceTextureID();
            mSurfaceTexture = new SurfaceTexture(mVideoTextureID);
            mSurfaceTexture.setOnFrameAvailableListener(VideoPlayerGLSurfaceView.this);
        }
    }

    public synchronized void setFilterWithConfig(final String config) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                lastConfig = config;
                if (mFrameRenderer != null) {
                    mFrameRenderer.setFilterWidthConfig(config);
                    if (mHandleCurrentFrame)
                        requestRender();
                } else {
                    Log.e(LOG_TAG, "setFilterWithConfig after release!!");
                }
            }
        });
    }

    public void setFilterIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRenderer != null) {
                    mFrameRenderer.setFilterIntensity(intensity);
                } else {
                    Log.e(LOG_TAG, "setFilterIntensity after release!!");
                }
            }
        });
    }

    public void setMaskBitmap(final Bitmap bmp, final boolean shouldRecycle) {
        setMaskBitmap(bmp, shouldRecycle, null);
    }

    //注意， 当传入的bmp为null时， SetMaskBitmapCallback 不会执行.
    public void setMaskBitmap(final Bitmap bmp, final boolean shouldRecycle, final SetMaskBitmapCallback callback) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRenderer == null) {
                    Log.e(LOG_TAG, "setMaskBitmap after release!!");
                    return;
                }

                if (bmp == null) {
                    mFrameRenderer.setMaskTexture(0, 1.0f);
                    mIsUsingMask = false;
                    calcViewport();
                    return;
                }

                int texID = Common.genNormalTextureID(bmp, GLES20.GL_NEAREST, GLES20.GL_CLAMP_TO_EDGE);

                mFrameRenderer.setMaskTexture(texID, bmp.getWidth() / (float) bmp.getHeight());
                mIsUsingMask = true;
                mMaskAspectRatio = bmp.getWidth() / (float) bmp.getHeight();

                if (callback != null) {
                    callback.setMaskOK(mFrameRenderer);
                }

                if (shouldRecycle)
                    bmp.recycle();

                calcViewport();
            }
        });
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setFitFullView(boolean fit) {
        mFitFullView = fit;
        if (mFrameRenderer != null)
            calcViewport();
    }

    public int getViewWidth() {
        return mViewWidth;
    }

    public int getViewheight() {
        return mViewHeight;
    }

    public boolean isUsingMask() {
        return mIsUsingMask;
    }

    public void setHandleCurrentFrame(boolean handleCurrentFrame) {
        mHandleCurrentFrame = handleCurrentFrame;
    }

    //定制一些初始化操作
    public void setOnCreateCallback(final OnCreateCallback callback) {
        assert callback != null : "无意义操作!";

        if (mFrameRenderer == null) {
            mOnCreateCallback = callback;
        } else {
            // 已经创建完毕， 直接执行
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    callback.createOK();
                }
            });
        }
    }

    private void calcViewport() {
        float scaling;

        if (mIsUsingMask) {
            scaling = mMaskAspectRatio;
        } else {
            scaling = mVideoWidth / (float) mVideoHeight;
        }

        float viewRatio = mViewWidth / (float) mViewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (mFitFullView) {
            //撑满全部view(内容大于view)
            if (s > 1.0) {
                w = (int) (mViewHeight * scaling);
                h = mViewHeight;
            } else {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            }
        } else {
            //显示全部内容(内容小于view)
            if (s > 1.0) {
                w = mViewWidth;
                h = (int) (mViewWidth / scaling);
            } else {
                h = mViewHeight;
                w = (int) (mViewHeight * scaling);
            }
        }

        mRenderViewport.width = w;
        mRenderViewport.height = h;
        mRenderViewport.x = (mViewWidth - mRenderViewport.width) / 2;
        mRenderViewport.y = (mViewHeight - mRenderViewport.height) / 2;
        Log.i(LOG_TAG, String.format("View port: %d, %d, %d, %d", mRenderViewport.x, mRenderViewport.y, mRenderViewport.width, mRenderViewport.height));
    }

    public void createFrameRenderer(int videoWidth, int videoHeight) {
        if(mVideoWidth == videoWidth && mVideoHeight == videoHeight) {
            return;
        }
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;

        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRenderer == null) {
                    mFrameRenderer = new CGEFrameRenderer();
                }
                if (mFrameRenderer.init(mVideoWidth, mVideoHeight, mVideoWidth, mVideoHeight)) {
                    //Keep right orientation for source texture blending
                    mFrameRenderer.setSrcFlipScale(1.0f, -1.0f);
                    mFrameRenderer.setRenderFlipScale(1.0f, -1.0f);
                    mFrameRenderer.setFilterWidthConfig(lastConfig);
                } else {
                    Log.e(LOG_TAG, "Frame Recorder init failed!");
                }
                calcViewport();
            }
        });
    }


    public synchronized void takeShot(final TakeShotCallback callback) {
        assert callback != null : "callback must not be null!";

        if (mFrameRenderer == null) {
            Log.e(LOG_TAG, "Drawer not initialized!");
            callback.takeShotOK(null);
            return;
        }

        queueEvent(new Runnable() {
            @Override
            public void run() {
                IntBuffer buffer = IntBuffer.allocate(mRenderViewport.width * mRenderViewport.height);

                GLES20.glReadPixels(mRenderViewport.x, mRenderViewport.y, mRenderViewport.width, mRenderViewport.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                Bitmap bmp = Bitmap.createBitmap(mRenderViewport.width, mRenderViewport.height, Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(buffer);

                Bitmap bmp2 = Bitmap.createBitmap(mRenderViewport.width, mRenderViewport.height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bmp2);
                Matrix mat = new Matrix();
                mat.setTranslate(0.0f, -mRenderViewport.height / 2.0f);
                mat.postScale(1.0f, -1.0f);
                mat.postTranslate(0.0f, mRenderViewport.height / 2.0f);

                canvas.drawBitmap(bmp, mat, null);
                bmp.recycle();

                callback.takeShotOK(bmp2);
            }
        });
    }

    public interface OnCreateCallback {
        void createOK();
    }

    public interface SetMaskBitmapCallback {
        void setMaskOK(CGEFrameRenderer recorder);
    }

    public interface TakeShotCallback {
        //传入的bmp可以由接收者recycle
        void takeShotOK(Bitmap bmp);
    }
}
