package radiotaxipavill.radiotaxipavillapp.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CircularImageView extends AppCompatImageView {

    private Paint paint;
    private BitmapShader shader;
    private Matrix shaderMatrix;

    public CircularImageView(Context context) {
        super(context);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        shaderMatrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = getBitmapFromDrawable();
        if (bitmap != null) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            float radius = Math.min(viewWidth / 2.0f, viewHeight / 2.0f);

            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            shaderMatrix.set(null);

            // Escalar el bitmap para que se ajuste completamente al círculo
            float scale;
            float dx = 0, dy = 0;
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            if (bitmapWidth * viewHeight > viewWidth * bitmapHeight) {
                scale = (float) viewHeight / bitmapHeight;
                dx = (viewWidth - bitmapWidth * scale) * 0.5f;
            } else {
                scale = (float) viewWidth / bitmapWidth;
                dy = (viewHeight - bitmapHeight * scale) * 0.5f;
            }

            shaderMatrix.setScale(scale, scale);
            shaderMatrix.postTranslate(dx, dy);

            shader.setLocalMatrix(shaderMatrix);
            paint.setShader(shader);

            canvas.drawCircle(viewWidth / 2.0f, viewHeight / 2.0f, radius, paint);
        }
    }

    private Bitmap getBitmapFromDrawable() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
