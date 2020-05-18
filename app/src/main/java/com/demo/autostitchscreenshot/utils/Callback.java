package com.demo.autostitchscreenshot.utils;

public interface Callback {
    void run();

    interface With<T> {
        void run(T t);
    }

    interface WithPair<T, V> {
        void run(T t, V v);
    }

    interface ItemTouchListener{
        void onMove(int fromPos, int toPos);
        void swipe(int position, int direction);
    }

}
