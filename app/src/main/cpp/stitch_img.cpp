#include "stitch_img.h"
#include <android/log.h>
#include <chrono>
#include <sstream>
#include <algorithm>

template<typename T>
std::string to_string(T value) {
    std::ostringstream os;
    os << value;
    return os.str();
}


const float DEVIATION_X = 2;
const float DEVIATION_Y = 100;
const int MIN_MATCH = 30;

float ratio_scale = 1;
int minWidth = INT_MAX;
unordered_map<string, int> cacheNumberKeyPointMatch;
vector<int> order;

using namespace std::chrono;
high_resolution_clock::time_point start;


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_zhihu_matisse_StitchImgPresenter_checkNativeStitch(JNIEnv *env, jobject thiz,
                                                            jobjectArray selected_paths) {
    vector<string> paths = objectArrayToVectorString(env, selected_paths);
    readImg(paths);

    if (paths.size() < 2)
        return false;
    else if (paths.size() < 5)
        return checkSmallSize(paths);
    else return checkLargeSize(paths);
}

void logTime(string msg) {
    high_resolution_clock::time_point now;
    milliseconds duration;
    now = high_resolution_clock::now();
    duration = duration_cast<milliseconds>(now - start);
    msg += to_string(duration.count());
    __android_log_print(ANDROID_LOG_DEBUG, "Native", "%s ", msg.c_str());
}

unordered_map<string, Mat> image_src;

void detectAndCompute(Mat imgSrc, vector<vector<KeyPoint> > &keypoints, vector<Mat> &decryptions,
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

pair<int, int>
getPairCutRow(Mat descriptors_object, vector<KeyPoint> &keypoints_object, Mat descriptors_scene,
              vector<KeyPoint> &keypoints_scene, int &numberKeyPointMatch) {
    BFMatcher matcher;
    vector<DMatch> good_matches, matches;
    unordered_map<int, int> parallel;
    matcher.match(descriptors_object, descriptors_scene, matches);

    int maxFrequency, rowChoiceObject, rowChoiceScene;
    maxFrequency = rowChoiceObject = rowChoiceScene = 0;
    int sub;
    for (DMatch matche : matches) {
        if (abs(keypoints_object[matche.queryIdx].pt.x - keypoints_scene[matche.trainIdx].pt.x) <=
            DEVIATION_X) {
            sub = keypoints_object[matche.queryIdx].pt.y - keypoints_scene[matche.trainIdx].pt.y;
            if (sub > DEVIATION_Y) {
                int count;
                if (parallel.find(sub) == parallel.end())
                    count = parallel[sub] = 1;
                else
                    count = ++parallel[sub];
                maxFrequency = count > maxFrequency ? count : maxFrequency;
                good_matches.push_back(matche);
            }
        }

    }
    int flag, count;
    flag = count = 0;
    for (DMatch matche : good_matches) {
        sub = keypoints_object[matche.queryIdx].pt.y - keypoints_scene[matche.trainIdx].pt.y;
        if (parallel[sub] == maxFrequency) {
            if (flag == 0) {
                flag = sub;
                count++;
            } else if (flag == sub) {
                rowChoiceObject = keypoints_object[matche.queryIdx].pt.y;
                rowChoiceScene = keypoints_scene[matche.trainIdx].pt.y;
                count++;
            }
        }
    }
    numberKeyPointMatch = count;
    return make_pair(rowChoiceObject, rowChoiceScene);
}

void computeMatchDetail(Mat descriptors_object, vector<KeyPoint> &keypoints_object,
                        Mat descriptors_scene,
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
                    isSwap = distanceY < 0;
                }
            }
        }
    }
    if (isSwap)
        swap(matchDetail.img1, matchDetail.img2);
    matchDetail.numberMatch = maxFrequency;
}

void readImg(const vector<string> &selected_paths) {
    for (const string &str:selected_paths) {
        if (image_src.find(str) != image_src.end())
            continue;
        Mat img = imread(str);
        image_src[str] = img;
        if (img.cols < minWidth) {
            minWidth = img.cols;
            ratio_scale = (float) 480 / minWidth;
        }
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

bool checkLargeSize(vector<string> &paths) {
    sort(paths.begin(), paths.end());
    if (!cache(paths))
        return false;
    else if (paths.empty())
        return true;
    vector<Mat> src;
    vector<Mat> srcScaleAndGray;
    vector<vector<KeyPoint>> keypoints(paths.size());
    vector<Mat> decryptions(paths.size());
    prepare(paths, src, srcScaleAndGray, keypoints, decryptions);

    for (int i = 1; i < srcScaleAndGray.size(); i++) {
        MatchDetail matchDetail{i - 1, i, 0};
        computeMatchDetail(decryptions[i - 1], keypoints[i - 1],
                           decryptions[i], keypoints[i],
                           matchDetail);
        string key;
        if (matchDetail.img1 == i)
            key = paths[i] + paths[i - 1];
        else
            key = paths[i - 1] + paths[i];
        cacheNumberKeyPointMatch[key] = matchDetail.numberMatch;

        if (matchDetail.numberMatch < MIN_MATCH)
            return false;
    }
    return true;
}

bool checkSmallSize(vector<string> &paths) {
    vector<Mat> src;
    vector<Mat> srcScaleAndGray;
    vector<vector<KeyPoint>> keypoints(paths.size());
    vector<Mat> decryptions(paths.size());
    prepare(paths, src, srcScaleAndGray, keypoints, decryptions);
    vector<MatchDetail> matchDetailList;
    for (int i = 0; i < srcScaleAndGray.size() - 1; i++)
        for (int j = i + 1; j < srcScaleAndGray.size(); j++) {
            MatchDetail matchDetail{i, j, 0};
            int numberMatch = getNumberKeyPointMatch(paths[i], paths[j]);
            if (numberMatch != INT_MIN) {
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
            cacheNumberKeyPointMatch[key] = matchDetail.numberMatch;
        }
    return computeOrder(paths.size(), matchDetailList);
}

void prepare(const vector<string> &paths, vector<Mat> &src, vector<Mat> &srcScaleAndGray,
             vector<vector<KeyPoint>> &keypoints, vector<Mat> &decryptions) {
    src.reserve(paths.size());
    for (const string &str:paths)
        src.push_back(image_src[str]);
    srcScaleAndGray = scaleAndGray(src);
    for (int i = 0; i < srcScaleAndGray.size(); i++)
        detectAndCompute(srcScaleAndGray[i], keypoints, decryptions, i);
}

bool cache(vector<string> &paths) {
    vector<string> res;
    for (int i = 0; i < paths.size(); i++) {
        string currentFile = paths[i];
        if (i < paths.size() - 1) {
            string nextFile = paths[i + 1];
            int numberKeyPointMatch = getNumberKeyPointMatch(currentFile, nextFile);
            if (numberKeyPointMatch > 0 && numberKeyPointMatch < MIN_MATCH)
                return false;
            else if (numberKeyPointMatch > MIN_MATCH)
                if (i == 0 ||
                    (i > 0 && getNumberKeyPointMatch(paths[i - 1], currentFile) > MIN_MATCH))
                    continue;
        }
        if (i > 0 && i == paths.size() - 1 &&
            getNumberKeyPointMatch(paths[i - 1], currentFile) > MIN_MATCH)
            continue;
        res.push_back(currentFile);
    }
    paths = res;
    return paths.size() != 1;
}

int getNumberKeyPointMatch(string img1, string img2) {
    if (cacheNumberKeyPointMatch.find(img1 + img2) != cacheNumberKeyPointMatch.end())
        return cacheNumberKeyPointMatch[img1 + img2];
    if (cacheNumberKeyPointMatch.find(img2 + img1) != cacheNumberKeyPointMatch.end())
        return cacheNumberKeyPointMatch[img2 + img1] * (-1);
    return INT_MIN;
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
    order.clear();
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
    }
    for(int i:order)
        __android_log_print(ANDROID_LOG_DEBUG,"Native","%s", to_string(i+1).c_str());
    return true;
}

bool compareMatch(MatchDetail matchDetail1, MatchDetail matchDetail2) {
    return matchDetail1.numberMatch > matchDetail2.numberMatch;
}

