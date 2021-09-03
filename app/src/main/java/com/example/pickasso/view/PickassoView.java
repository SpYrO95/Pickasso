package com.example.pickasso.view;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static android.graphics.Color.WHITE;

public class PickassoView extends View
{
    public static final float TOUCH_TOLERANCE = 0;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen;
    private Paint paintLine;
    private HashMap<Integer, Path> pathMap;
    private HashMap<Integer, Point> previousPointMap;

    public PickassoView(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        init();
    }

    void init(){

        paintScreen = new Paint();
        paintScreen.setColor(WHITE);
        paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(7);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        pathMap = new HashMap<>();
        previousPointMap = new HashMap<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(WHITE);
        bitmapCanvas.drawRGB(255,255,255);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        for(Integer key: pathMap.keySet())
        {
            canvas.drawPath(pathMap.get(key), paintLine);
        }
    }

    /*
        Touch functions.
    */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked(); //event type;
        int actionIndex = event.getActionIndex(); //pointer (finger, mouse...);

        if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP){
            touchStarted(event.getX(actionIndex),
                    event.getY(actionIndex),
                    event.getPointerId(actionIndex));
        }
        else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP){
            touchEnded(event.getPointerId(actionIndex));
        }
        else{
            touchMoved(event);
        }

        invalidate();// redraw the screen

        return true;
    }

    private void touchMoved(MotionEvent event) {

        for (int i = 0; i < event.getPointerCount(); i++) {

            int pointerId = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerId);

            if (pathMap.containsKey(pointerId)) {
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathMap.get(pointerId);
                Point point = previousPointMap.get(pointerId);

                //Calculate how far the user moved from the  last update
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                //If the distance is significant enough to be considered a movement
                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                    //Move the path to the new location
                    path.quadTo(point.x, point.y,
                            (newX + point.x) / 2,
                            (newY + point.y) / 2);

                    //Store the new coordinates
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }

            invalidate();
        }
    }

    private void touchEnded(int pointerId) {
        Path path = pathMap.get(pointerId); //Get the corresponding Path
        bitmapCanvas.drawPath(path, paintLine); //Draw to bitmapCanvas
        path.reset();
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path; //store the path for given touch
        Point point; //store the last point in path

        if(pathMap.containsKey(pointerId)){
            path = pathMap.get(pointerId);
            point = previousPointMap.get(pointerId);
        }else{
            path = new Path();
            pathMap.put(pointerId, path);
            point = new Point();
            previousPointMap.put(pointerId, point);
        }

        //move to the coordinates of the touch
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
    }

    /*
        Menu functions.
    */

    public void saveImageToGallery(ContentResolver cr){

        MediaStore.Images.Media.insertImage(cr, bitmap, "title", "description");

        Toast message = Toast.makeText(getContext(), "Image Saved In Gallery", Toast.LENGTH_LONG);
        message.setGravity(Gravity.CENTER, message.getXOffset()/2,message.getYOffset() / 2);

        message.show();
    }

    public void clear(){
        pathMap.clear(); // Remove all of the paths
        previousPointMap.clear();
        bitmapCanvas.drawRGB(255,255,255);
        bitmap.eraseColor(0xFFFFFFFF);
        invalidate(); // Refresh the screen
    }

    public void setDrawingColor(int color){
        paintLine.setColor(color);
    }

    public int getDrawingColor(){
        return paintLine.getColor();
    }

    public void setLineWidth(int width){
        paintLine.setStrokeWidth(width);
    }

    public Bitmap getBitMap(){return bitmap;}

}
