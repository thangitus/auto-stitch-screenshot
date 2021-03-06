#include "stitch_img.h"
#include <android/log.h>
#include <chrono>
#include <sstream>
#include <algorithm>

const float DEVIATION_X = 2;
const float DEVIATION_Y = 100;
const int MIN_MATCH = 30;

float ratio_scale = 1;
int minWidth = INT_MAX;
unordered_map<string, MatchDetail> *cacheMatchDetail;
vector<int> order;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_zhihu_matisse_StitchImgPresenter_checkNativeStitch(JNIEnv *env, jobject thiz,
                                                            jobjectArray selected_paths,
                                                            jobjectArray list_Src) {
    if (cacheMatchDetail == nullptr)
        cacheMatchDetail = new unordered_map<string, MatchDetail>;
    vector<string> paths = objectArrayToVectorString(env, selected_paths);
    vector<Mat> listSrc = objectArrayToVectorMat(env, list_Src);
    if (paths.size() < 5) {
        order.clear();
        return checkSmallSize(paths, listSrc);
    } else return checkLargeSize(paths, listSrc);
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_zhihu_matisse_StitchImgPresenter_stitchNative(JNIEnv *env, jobject thiz,
                                                       jobjectArray selected_paths,
                                                       jobjectArray list_Src) {
    vector<string> paths = objectArrayToVectorString(env, selected_paths);
    vector<Mat> src = objectArrayToVectorMat(env, list_Src);

    if (!order.empty() && paths.size() < 5) {
        vector<string> tmpPath;
        vector<Mat> tmpSrc;
        tmpPath.reserve(order.size());
        for (int i:order) {
            tmpPath.push_back(paths[i]);
            tmpSrc.push_back(src[i]);
        }
        paths = tmpPath;
        src = tmpSrc;
    }

    vector<vector<KeyPoint>> keypoints(paths.size());
    vector<Mat> decryptions(paths.size());
    prepare(paths, src, keypoints, decryptions);
    vector<MatchDetail> matchDetails;
    for (int i = 1; i < src.size(); i++) {
        MatchDetail tmp{i - 1, i, 0, 0, 0};
        tmp = (*cacheMatchDetail)[paths[i - 1] + paths[i]];
        if (tmp.numberMatch != 0) {
            matchDetails.push_back(tmp);
            continue;
        }
        computeMatchDetail(decryptions[i - 1], keypoints[i - 1], decryptions[i], keypoints[i], tmp);
        matchDetails.push_back(tmp);
    }
    cropImg(src, matchDetails);
    Mat res = stitchImagesVertical(src);
    jobject bitmap = matToBitmap(env, res);
    cacheMatchDetail->clear();
    delete cacheMatchDetail;
    cacheMatchDetail = nullptr;
    order.clear();
    return bitmap;
}

void cropImg(vector<Mat> &src, vector<MatchDetail> matchDetails) {
    vector<Mat> res;
    vector<pair<int, int>> rowCutImgs(src.size());
    int height = src[0].rows;

    rowCutImgs[0].first = 0;
    rowCutImgs[rowCutImgs.size() - 1].second = height;
    for (int i = 0; i < matchDetails.size(); i++) {
        rowCutImgs[i].second = matchDetails[i].rowImg1 / ratio_scale;
        rowCutImgs[i + 1].first = matchDetails[i].rowImg2 / ratio_scale;
    }

    for (int i = 0; i < src.size(); i++) {
        int top = rowCutImgs[i].first;
        int bottom = rowCutImgs[i].second - rowCutImgs[i].first;
        int width = src[i].cols;
        if (top + bottom > height)
            bottom = height - top;
        cv::Rect roi(0, top, width, bottom);
        res.push_back(src[i](roi));
    }
    src = res;
}

Mat stitchImagesVertical(const vector<Mat> &src) {
    Mat res;
    vconcat(src, res);
    return res;
}


void
detectAndCompute(const Mat &imgSrc, vector<vector<KeyPoint> > &keypoints, vector<Mat> &decryptions,
                 int index) {
    Ptr<FeatureDetector> detector = FastFeatureDetector::create();
    Ptr<DescriptorExtractor> extractor = ORB::create();
    Mat decryption;
    vector<KeyPoint> keypoint;
    detector->detect(imgSrc, keypoint);
    extractor->compute(imgSrc, keypoint, decryption);

    keypoints[index] = keypoint;
    decryptions[index] = decryption;

}

vector<Mat> scaleAndGray(vector<Mat> &src) {
    vector<Mat> res;
    float scale;
    for (int i = 0; i < src.size(); i++) {
        Mat tmp;
        scale = (float) minWidth / src[i].cols;
        resize(src[i], src[i], cv::Size(), scale, scale);
        Mat img = src[i];
        cvtColor(img, tmp, COLOR_BGR2GRAY);
        resize(tmp, tmp, cv::Size(), ratio_scale, ratio_scale);
        res.push_back(tmp);
    }
    return res;
}

void computeMatchDetail(const Mat &descriptors_object, vector<KeyPoint> &keypoints_object,
                        const Mat &descriptors_scene,
                        vector<KeyPoint> &keypoints_scene, MatchDetail &matchDetail) {
    BFMatcher matcher;
    vector<DMatch> good_matches, matches;
    unordered_map<int, int> parallel;
    matcher.match(descriptors_object, descriptors_scene, matches);

    int maxFrequency = 0;
    bool isSwap = false;
    for (DMatch match : matches) {
        if (abs(keypoints_object[match.queryIdx].pt.x - keypoints_scene[match.trainIdx].pt.x) <=
            DEVIATION_X) {
            int distanceY =
                    keypoints_object[match.queryIdx].pt.y - keypoints_scene[match.trainIdx].pt.y;
            if (abs(distanceY) > DEVIATION_Y) {
                int count;
                if (parallel.find(distanceY) == parallel.end())
                    count = parallel[distanceY] = 1;
                else
                    count = ++parallel[distanceY];

                if (count > maxFrequency) {
                    maxFrequency = count;
                    good_matches.push_back(match);
                    isSwap = distanceY < 0;
                }
            }
        }
    }
    matchDetail.numberMatch = maxFrequency;

    for (DMatch match : good_matches) {
        int sub = keypoints_object[match.queryIdx].pt.y - keypoints_scene[match.trainIdx].pt.y;
        if (parallel[sub] == maxFrequency) {
            matchDetail.rowImg1 = keypoints_object[match.queryIdx].pt.y;
            matchDetail.rowImg2 = keypoints_scene[match.trainIdx].pt.y;
        }
    }
    if (isSwap) {
        swap(matchDetail.img1, matchDetail.img2);
        swap(matchDetail.rowImg1, matchDetail.rowImg2);
    }
}


vector<string> objectArrayToVectorString(JNIEnv *env, jobjectArray obj) {
    vector<string> res;
    int size = env->GetArrayLength(obj);
    for (int i = 0; i < size; i++) {
        jstring jstr = static_cast<jstring>(env->GetObjectArrayElement(obj, i));
        res.emplace_back(env->GetStringUTFChars(jstr, JNI_FALSE));
    }
    return res;
}

vector<Mat> objectArrayToVectorMat(JNIEnv *env, jobjectArray list_src) {
    vector<Mat> res;
    int size = env->GetArrayLength(list_src);
    Mat tmp;
    for (int i = 0; i < size; i++) {
        jobject bitmap = env->GetObjectArrayElement(list_src, i);
        tmp = bitmapToMat(env, bitmap);
        res.push_back(tmp);
        if (tmp.cols < minWidth) {
            minWidth = tmp.cols;
            ratio_scale = (float) 480 / minWidth;
        }

    }
    return res;
}

bool checkLargeSize(vector<string> &paths, vector<Mat> &src) {
    if (!cache(paths, src))
        return false;
    else if (paths.empty())
        return true;

    vector<vector<KeyPoint>> keypoints(paths.size());
    vector<Mat> decryptions(paths.size());
    prepare(paths, src, keypoints, decryptions);
    for (int i = 1; i < src.size(); i++) {
        MatchDetail matchDetail{i - 1, i, 0, 0, 0};
        computeMatchDetail(decryptions[i - 1], keypoints[i - 1],
                           decryptions[i], keypoints[i],
                           matchDetail);
        string key;
        if (matchDetail.img1 == i)
            key = paths[i] + paths[i - 1];
        else
            key = paths[i - 1] + paths[i];
        (*cacheMatchDetail)[key] = matchDetail;

        if (matchDetail.numberMatch < MIN_MATCH)
            return false;
    }
    return true;
}

bool checkSmallSize(vector<string> &paths, vector<Mat> &src) {
    vector<vector<KeyPoint>> keypoints(paths.size());
    vector<Mat> decryptions(paths.size());
    prepare(paths, src, keypoints, decryptions);
    vector<MatchDetail> matchDetailList;
    for (int i = 0; i < src.size() - 1; i++)
        for (int j = i + 1; j < src.size(); j++) {
            MatchDetail matchDetail{i, j, 0};
            int numberMatch = getNumberKeyPointMatch(paths[i], paths[j]);
            if (numberMatch != INT8_MIN) {
                if (numberMatch < 0) {
                    swap(matchDetail.img1, matchDetail.img2);
                    matchDetail.numberMatch = -(numberMatch);
                } else
                    matchDetail.numberMatch = numberMatch;
                matchDetailList.push_back(matchDetail);
                continue;
            }
            computeMatchDetail(decryptions[i], keypoints[i],
                               decryptions[j], keypoints[j],
                               matchDetail);
            matchDetailList.push_back(matchDetail);
            string key;
            if (matchDetail.img1 == j)
                key = paths[j] + paths[i];
            else
                key = paths[i] + paths[j];
            (*cacheMatchDetail)[key] = matchDetail;
        }
    return computeOrder(paths.size(), matchDetailList);
}

void prepare(const vector<string> &paths, vector<Mat> &src,
             vector<vector<KeyPoint>> &keypoints, vector<Mat> &decryptions) {
    vector<Mat> srcScaleAndGray;
    srcScaleAndGray = scaleAndGray(src);
    for (int i = 0; i < srcScaleAndGray.size(); i++)
        detectAndCompute(srcScaleAndGray[i], keypoints, decryptions, i);
}

bool cache(vector<string> &paths, vector<Mat> &src) {
    vector<string> pathRes;
    vector<Mat> srcRes;
    for (int i = 0; i < paths.size(); i++) {
        string currentFile = paths[i];
        if (i < paths.size() - 1) {
            string nextFile = paths[i + 1];
            int numberKeyPointMatch = getNumberKeyPointMatch(currentFile, nextFile);
            if (abs(numberKeyPointMatch) < MIN_MATCH)
                return false;
            else if (numberKeyPointMatch > MIN_MATCH)
                if (i == 0 ||
                    (i > 0 && getNumberKeyPointMatch(paths[i - 1], currentFile) > MIN_MATCH)) {
                    continue;
                }
        }
        if (i > 0 && i == paths.size() - 1 &&
            getNumberKeyPointMatch(paths[i - 1], currentFile) > MIN_MATCH) {
            continue;
        }
        srcRes.push_back(src[i]);
        pathRes.push_back(currentFile);
    }
    src = srcRes;
    paths = pathRes;
    return paths.size() != 1;
}

int getNumberKeyPointMatch(string img1, string img2) {
    int res;
    res = (*cacheMatchDetail)[img1 + img2].numberMatch;
    if (res)
        return res;
    res = (*cacheMatchDetail)[img2 + img1].numberMatch;
    if (res)
        return -res;
    return INT8_MIN;
}

bool
computeOrder(unsigned int size, vector<MatchDetail> matchList) {
    vector<NODE *> nodes;
    for (int i = 0; i < size; i++) {
        NODE *node = new NODE;
        node->val = i;
        node->next = NULL;
        node->prev = NULL;
        nodes.push_back(node);
    }
    int edge = 0;
    sort(matchList.begin(), matchList.end(), compareMatch);
    for (auto matchDetail : matchList) {
        if (matchDetail.numberMatch > MIN_MATCH && !nodes[matchDetail.img1]->next &&
            !nodes[matchDetail.img2]->prev) {
            edge++;
            nodes[matchDetail.img1]->next = nodes[matchDetail.img2];
            nodes[matchDetail.img2]->prev = nodes[matchDetail.img1];
        }
    }
    if (edge < size - 1)
        return false;
    NODE *cur = nodes[1];
    while (cur->prev)
        cur = cur->prev;
    while (cur) {
        order.push_back(cur->val);
        cur = cur->next;
        if (cur) delete cur->prev;
    }

    return true;
}

bool compareMatch(MatchDetail matchDetail1, MatchDetail matchDetail2) {
    return matchDetail1.numberMatch > matchDetail2.numberMatch;
}

jobject matToBitmap(JNIEnv *env, Mat &src) {
    jclass java_bitmap_class = (jclass) env->FindClass("android/graphics/Bitmap");
    jclass bmpCfgCls = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID bmpClsValueOfMid = env->GetStaticMethodID(bmpCfgCls, "valueOf",
                                                        "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject jBmpCfg = env->CallStaticObjectMethod(bmpCfgCls, bmpClsValueOfMid,
                                                  env->NewStringUTF("ARGB_8888"));

    jmethodID mid = env->GetStaticMethodID(java_bitmap_class,
                                           "createBitmap",
                                           "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jobject bitmap = env->CallStaticObjectMethod(java_bitmap_class,
                                                 mid, src.size().width, src.size().height,
                                                 jBmpCfg);
    AndroidBitmapInfo info;
    void *pixels = 0;

    try {
        //validate
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);

        //type mat
        Mat tmp(info.height, info.width, CV_8UC4, pixels);
        cvtColor(src, tmp, CV_RGB2RGBA);
        AndroidBitmap_unlockPixels(env, bitmap);
        return bitmap;
    } catch (cv::Exception e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return bitmap;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return bitmap;
    }
}


Mat bitmapToMat(JNIEnv *env, jobject &bitmap) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;
    Mat dst;

    try {
        CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
        CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                  info.format == ANDROID_BITMAP_FORMAT_RGB_565);
        CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
        CV_Assert(pixels);
        dst.create(info.height, info.width, CV_8UC4);
        if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            Mat tmp(info.height, info.width, CV_8UC4, pixels);
            tmp.copyTo(dst);
        } else {
            Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return dst;
    } catch (const cv::Exception &e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("org/opencv/core/CvException");
        if (!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return dst;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return dst;
    }
}
