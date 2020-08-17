//
// Created by cpu11499-local on 16/07/2020.
//

#ifndef AUTOSTITCHSCREENSHOT_STITCH_IMG_H
#define AUTOSTITCHSCREENSHOT_STITCH_IMG_H

#include <jni.h>
#include <string>
#include <iostream>
#include <vector>
#include <mutex>
#include <unordered_map>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/bitmap.h>

using namespace std;
using namespace cv;

struct MatchDetail {
    int img1, img2;
    int rowImg1, rowImg2;
    int numberMatch;
};
struct NODE {
    int val;
    NODE *prev;
    NODE *next;
};

void detectAndCompute(const Mat& imgSrc, vector<vector<KeyPoint> > &keypoints, vector<Mat> &decryptions,
                      int index);

vector<Mat> scaleAndGray(vector<Mat> &src);

void computeMatchDetail(const Mat& descriptors_object, vector<KeyPoint> &keypoints_object,
                        const Mat& descriptors_scene,
                        vector<KeyPoint> &keypoints_scene, MatchDetail &matchDetail);

jobject matToBitmap(JNIEnv *env, Mat &src);

void cropImg(vector<Mat> &src, vector<MatchDetail> matchDetails);

Mat stitchImagesVertical(const vector<Mat>& src);


vector<string> objectArrayToVectorString(JNIEnv *env, jobjectArray obj);

vector<Mat> objectArrayToVectorMat(JNIEnv *env, jobjectArray list_src);

int getNumberKeyPointMatch(string img1, string img2);

bool cache(vector<string> &paths, vector<Mat> &src);

bool checkSmallSize(vector<string> &paths, vector<Mat> &src);

bool checkLargeSize(vector<string> &paths, vector<Mat> &src);


bool
computeOrder(unsigned int size, vector<MatchDetail> matchList);

bool compareMatch(MatchDetail matchDetail1, MatchDetail matchDetail2);

void prepare(const vector<string> &paths, vector<Mat> &src,
             vector<vector<KeyPoint>> &keypoints, vector<Mat> &decryptions);

Mat bitmapToMat(JNIEnv *env, jobject &bitmap);

#endif //AUTOSTITCHSCREENSHOT_STITCH_IMG_H
