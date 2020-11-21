package com.vgermonenko.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/*мы создали новый класс*/
public class PaintView extends View {
    /*константы*/
    public static int BRUSH_SIZE = 1;
    public static final int DEFAULT_COLOR = Color.BLACK;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    /*переменные*/
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    /*Включить сглаживание*/
    /*Сглаживание влияет на то, как снижаются цвета,
     которые являются более высокой точностью, чем устройство.
     Без сглаживания обычно быстрее, но цвета с более высокой точностью просто усекаются.
    */
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    private ArrayList<Draw> paths = new ArrayList<>();
    private ArrayList<Draw> undo = new ArrayList<>();

    /*первый конструктор*/
    public PaintView(Context context){
        super(context,null);
    }

    /*второй конструктор*/
    public PaintView(Context context, AttributeSet attrs){

        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        /*setStrokeJoin позволяет установить три режима рендеринга соединения толстых
        линий. КРУГЛЫЙ означает, что круглая секция используется
        для обхода изгиба,*/
        /*"стиль" или то как выглядят углы на соединении линий*/
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        /*закруглённый конец линии*/
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        /*Xfermode - это базовый класс для объектов,
        которые вызываются для реализации настраиваемых «режимов передачи»
        в конвейере рисования.*/
        mPaint.setXfermode(null);
        /*нужно установить для корректной работы*/
        /*смешивания или накладывания цветов*/
        mPaint.setAlpha(0xff);

    }
    /*функция инициализации*/
    public void initialise (DisplayMetrics displayMetrics){

        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }
    /*методы*/

    @Override /*переопределение*/
    protected void onDraw(Canvas canvas) {

        mCanvas.drawColor(backgroundColor);/*рисовка задника*/

        for (Draw draw : paths){/*просто использует пути,что мы создали*/
            mPaint.setColor(draw.color);
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setMaskFilter(null);
            /*рисуем путь*/
            mCanvas.drawPath(draw.path, mPaint);

        }
        canvas.drawBitmap(mBitmap, 0 , 0 , mBitmapPaint);

    }

    /*обработчики касаний*/
    /*touch events*/
    private void touchStart (float x, float y) {

        mPath = new Path();

        Draw draw = new Draw(currentColor, strokeWidth, mPath );/*реализуем класс Draw*/
        paths.add(draw);/*добавляем массив*/

        mPath.reset();
        mPath.moveTo(x,y);/*хотим переити на х у , куда мы кликнули*/

        mX = x;
        mY = y;

    }

    private void touchMove (float x, float y){

        float dx = Math.abs(x - mX);/*разница между иксами*/
        float dy = Math.abs(y - mY);/*разница между игриками*/

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE){
            /*QuadTo сгладит неровности на поворотах.*/
            mPath.quadTo(mX, mY, (x+mX)/2,(y+mY)/2);

            mX=x;
            mY=y;

        }
    }


    private void touchUp (){

        mPath.lineTo(mX, mY);

    }


    /*обработчик касания, который будет случаем float x */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){
            /*MotionEvent описывает движения в терминах кода действия и набора значений оси*/
            /*Когда пользователь впервые касается экрана,
             система доставляет событие касания соответствующему представлению
             с кодом действия ACTION_DOWN и набором значений оси,
             которые включают координаты X и Y касания и информацию о давлении,
             размере и ориентация контактной площадки.*/
            case MotionEvent.ACTION_DOWN:
                 touchStart(x, y);
                 invalidate();
                 break;
            /*пользователь снимает нажатие*/
            case  MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
                /*событие движения*/
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;

        }

        return true;

    }

    public void clear () {

        backgroundColor = DEFAULT_BG_COLOR;

        paths.clear();
        invalidate();

    }

    public void undo () {

        if (paths.size() > 0){

            undo.add(paths.remove(paths.size() - 1));
            invalidate();

        }else {
            Toast.makeText(getContext(), "Nothing to undo", Toast.LENGTH_LONG).show();
        }

    }

    public void redo () {

        if (undo.size() > 0){

            paths.add(undo.remove(undo.size() - 1));
            invalidate();

        }else {
            Toast.makeText(getContext(), "Nothing to redo", Toast.LENGTH_LONG).show();
        }

    }

    public void setStrokeWidth(int width){

        strokeWidth = width;

    }

    public void setColor (int color){

        currentColor = color;
    }

    public void saveImage(){

        int count = 0;

        File sdDirectory = Environment.getExternalStorageDirectory();
        File subDirectory = new File(sdDirectory.toString() + "/Pictures/Paint");

        if (subDirectory.exists()){
/*если файл с уществует, то мы хотим пройтись и подсчитать файлы */
            File[] existing = subDirectory.listFiles();

            for (File file : existing){
                /*проверим тип файла*/
                if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png")){

                    count++;

                }

            }

        }else {

            subDirectory.mkdir();
        }

        if (subDirectory.exists()){
            File image = new File(subDirectory, "/drawing_" + (count + 1) + ".png");
            /*Класс FileOutputStream предназначен для записи байтов в файл*/
            FileOutputStream fileOutputStream;

            try {

                fileOutputStream = new FileOutputStream(image);
/*мы хотим формат в PNG*/
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

                fileOutputStream.flush();
                fileOutputStream.close();

                Toast.makeText(getContext(), "saved", Toast.LENGTH_LONG).show();

            }catch (FileNotFoundException e) {

            }catch (IOException e){

            }


        }



    }



}
