#include <jni.h>
#include <string>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>
#include <android/bitmap.h>
#include <android/log.h>
using namespace cv;
using namespace std;
void debug(Mat path);

extern "C"
JNIEXPORT jint JNICALL
Java_com_zhihu_matisse_StitchImgPresenter_canny(JNIEnv *env, jobject thiz, jstring src) {
    const char *path = env->GetStringUTFChars(src, JNI_FALSE);
    string str = path;
    Mat mat = imread(str);
    Mat mat2 = Mat(Size(100, 100), 3);
    debug(mat);
    return mat.rows;
}

void debug(Mat path) {
    cout << path << endl;
}
