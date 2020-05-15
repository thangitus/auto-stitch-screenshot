package com.demo.autostitchscreenshot.utils;

public interface Callback {
    void run();

    interface With<T> {
        void run(T t);
    }

    interface WithPair<T, V> {
        void run(T t, V v);
    }

}
