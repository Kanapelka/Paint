package com.vgermonenko.paint;


import android.graphics.Path;
/*просто пустой класс без расширений или имплементаций или ещё чего-то*/
public class Draw {
    public int color;
    public int strokeWidth;
    public Path path;

/*создали конструкторы*/
    public Draw(int color, int strokeWidth, Path path) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}
