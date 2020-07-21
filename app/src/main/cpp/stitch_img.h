//
// Created by cpu11499-local on 16/07/2020.
//

#ifndef AUTOSTITCHSCREENSHOT_STITCH_IMG_H
#define AUTOSTITCHSCREENSHOT_STITCH_IMG_H

#include <jni.h>
#include <string>
#include <iostream>
#include <vector>
#include <pthread.h>
#include <fstream>
#include <thread>
#include <mutex>
#include <map>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>
#include <android/bitmap.h>
#include <android/log.h>

using namespace std;
using namespace cv;

struct MatchDetail {
    int img1, img2;
    int numberMatch;
};
struct NODE {
    int val;
    NODE *prev;
    NODE *next;
};

void detectAndCompute(Mat imgSrc, vector<vector<KeyPoint> > &keypoints, vector<Mat> &decryptions,
                      int index);

void detectAndWrite(vector<Mat> src);

vector<Mat> scaleAndGray(vector<Mat> &src);

pair<int, int>
getPairCutRow(Mat descriptors_object, vector<KeyPoint> &keypoints_object, Mat descriptors_scene,
              vector<KeyPoint> &keypoints_scene, int &numberKeyPointMatch);

void computeMatchDetail(Mat descriptors_object, vector<KeyPoint> &keypoints_object,
                        Mat descriptors_scene,
                        vector<KeyPoint> &keypoints_scene, MatchDetail &matchDetail);

vector<DMatch>
getDMatch(Mat descriptors_object, vector<KeyPoint> &keypoints_object, Mat descriptors_scene,
          vector<KeyPoint> &keypoints_scene);

vector<Mat> cropImg(vector<Mat> src, vector<pair<int, int> > row);

Mat stitchImagesVertical(vector<Mat> src);

void readImg(const vector<string> &selected_paths);

vector<string> objectArrayToVectorString(JNIEnv *env, jobjectArray obj);

int getNumberKeyPointMatch(string img1, string img2);

bool cache(vector<string> &paths);

bool checkSmallSize(vector<string> &paths);

bool checkLargeSize(vector<string> &paths);


bool
computeOrder(unsigned int size, vector<MatchDetail> matchList);

bool compareMatch(MatchDetail matchDetail1, MatchDetail matchDetail2);

void prepare(const vector<string> &paths, vector<Mat> &src, vector<Mat> &srcScaleAndGray,
             vector<vector<KeyPoint>> &keypoints, vector<Mat> &decryptions);

#endif //AUTOSTITCHSCREENSHOT_STITCH_IMG_H
