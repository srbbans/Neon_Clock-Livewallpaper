package bans.liveneon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;

import androidx.core.content.res.ResourcesCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;


public class ClockLiveWallpaperService extends WallpaperService {

    private static final int UPDATE_TIME_MILLIS = 40;

    private final Handler handler = new Handler();
    private Context mcontext;
    private Bitmap mBackground;

    @Override
    public Engine onCreateEngine() {
        mcontext = this;
        mBackground = BitmapFactory.decodeResource(getResources(), R.drawable.back_wall);
        return new ClockEngine();
    }

    private class ClockEngine extends Engine {

        private final Matrix matrix = new Matrix();
        private final Camera camera = new Camera();
        private final Paint paint = new Paint();
        private final Paint paint_stroke = new Paint();
        private final Paint _paintBlur = new Paint();
        private final int height;
        private final int width;
        private float centerX;
        private float centerY;
        private boolean isVisible;
        private final Runnable drawRunnable = this::drawFrame;

        public ClockEngine() {
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(getResources().getColor(R.color.text_color));
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTypeface(ResourcesCompat.getFont(mcontext, R.font.champagne_limousines));

            paint_stroke.setAntiAlias(true);
            paint_stroke.setDither(true);
            paint_stroke.setColor(getResources().getColor(R.color.text_color_stroke));
            paint_stroke.setStyle(Paint.Style.STROKE);
            paint_stroke.setStrokeWidth(5f);
            paint_stroke.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.OUTER));
            paint_stroke.setStrokeJoin(Paint.Join.ROUND);
            paint_stroke.setStrokeCap(Paint.Cap.ROUND);
            paint_stroke.setTypeface(ResourcesCompat.getFont(mcontext, R.font.champagne_limousines_bold));

            _paintBlur.set(paint);
            _paintBlur.setStrokeWidth(30f);
            _paintBlur.setColor(getResources().getColor(R.color.text_color_trans));
            _paintBlur.setMaskFilter(new BlurMaskFilter(45, BlurMaskFilter.Blur.OUTER));
            _paintBlur.setTypeface(ResourcesCompat.getFont(mcontext, R.font.champagne_limousines_bold));

            DisplayMetrics displayMetrics = mcontext.getApplicationContext().getResources().getDisplayMetrics();
            height = displayMetrics.heightPixels;
            width = displayMetrics.widthPixels;

        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            isVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            centerX = width / 2.0f;
            centerY = height / 4.0f;//2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            isVisible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);

            drawFrame();
        }

        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawTime(canvas);
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            // Reschedule the next redraw
            handler.removeCallbacks(drawRunnable);
            if (isVisible) {
                handler.postDelayed(drawRunnable, UPDATE_TIME_MILLIS);
            }
        }

        void drawTime(Canvas canvas) {
            canvas.save();

            Rect src = new Rect(0, 0, mBackground.getWidth() - 1, mBackground.getHeight() - 1);
            Rect dest = new Rect(0, 0, width - 1, height - 1);
            canvas.drawBitmap(mBackground, src, dest, null);

            camera.save();
            camera.getMatrix(matrix);

            matrix.postTranslate(centerX, centerY);

            canvas.concat(matrix);
            camera.restore();

            paint.setTextSize(getResources().getDimension(R.dimen.text_size));
            paint.setTextAlign(Paint.Align.CENTER);

            paint_stroke.setTextSize(getResources().getDimension(R.dimen.text_size));
            paint_stroke.setTextAlign(Paint.Align.CENTER);

            _paintBlur.setTextSize(getResources().getDimension(R.dimen.text_size));
            _paintBlur.setTextAlign(Paint.Align.CENTER);

            String text = getTimeIn12AmPm(System.currentTimeMillis());//getTimeString();

            canvas.drawText(text, 0.0f, 0.0f, _paintBlur);
            canvas.drawText(text, 0.0f, 0.0f, paint_stroke);
            canvas.drawText(text, 0.0f, 0.0f, paint);

            canvas.restore();
        }

        /**
         * returns the time in 12 hrs format
         */
        private String getTimeIn12AmPm(long timestamp) {
            try {
                DateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                java.sql.Date netDate = (new java.sql.Date(timestamp));
                return sdf.format(netDate);
            } catch (Exception ex) {
                return "xx";
            }
        }
    }
}