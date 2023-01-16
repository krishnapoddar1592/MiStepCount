//
// Created by Hi on 1/14/2023.
//

#include <jni.h>
#include <string>
#include<vector>
#include <numeric>
#include <iostream>
#include<cmath>
std::vector<double> lowPassFilter(std::vector<double> input, double cutoffFrequency, double sampleRate, int order) {
    // calculate the filter coefficients
    double omega = 2 * M_PI * cutoffFrequency / sampleRate;
    double alpha = sin(omega) / (2 * order);
    double a0 = 1 + alpha;
    double a1 = -2 * cos(omega);
    double a2 = 1 - alpha;
    double b0 = (1 - cos(omega)) / 2;
    double b1 = 1 - cos(omega);
    double b2 = (1 - cos(omega)) / 2;

    // apply the filter to the input data
    int n = input.size();
    std::vector<double> output(n);
    output[0] = b0 / a0 * input[0];
    output[1] = b0 / a0 * input[1] + b1 / a0 * input[0] - a1 / a0 * output[0];
    for (int i = 2; i < n; i++) {
        output[i] = b0 / a0 * input[i] + b1 / a0 * input[i-1] + b2 / a0 * input[i-2]
                    - a1 / a0 * output[i-1] - a2 / a0 * output[i-2];
    }

    return output;
}

extern "C"
jstring JNICALL
Java_com_example_mistepcount_NativeLibrary_stringFromJNI(JNIEnv *env, jobject instance) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}



extern "C" jint
Java_com_example_mistepcount_NativeLibrary_passingDataToJni(JNIEnv *env,
                                                               jobject instance,
                                                               jdoubleArray tmpArray_,
                                                               jint size,
                                                               jdouble prevMean) {

    jdouble *tmpArray = env->GetDoubleArrayElements(tmpArray_, nullptr);
    double *p =tmpArray;
    std::vector<double> input;
    double threshold=0.0087;
    for(int i =0;i<size;i++){
        input.push_back(*p);
        p++;
    }
    for (auto it = input.begin() + 1; it != input.end(); )
    {
        if (abs(*it - *(it - 1)) < threshold)
        {
            it = input.erase(it);
        }
        else
        {
            ++it;
        }
    }


    double stdDevthreshold = 0.8;  // Change this value to adjust the sensitivity of the heuristic
    double cutoffFrequency = 0.78;  // Hz
    double sampleRate = 40.0;      // Hz


//     Calculate the standard deviation of the input data
    double inputMean = std::accumulate(input.begin(), input.end(), 0.0) / input.size();
    double sumSquaredDiff = std::accumulate(input.begin(), input.end(), 0.0,
                                            [&](double acc, double x) { return acc + (x - inputMean) * (x - inputMean); });
    double variance = sumSquaredDiff / (input.size() - 1);
    double stdDev = sqrt(variance);

    std::vector<double> filtered;
    if (stdDev > stdDevthreshold) {
        // User is moving, apply low pass filter
        filtered = lowPassFilter(input, cutoffFrequency, sampleRate, 50);
    } else {
        // User is stationary, just push empty array
        filtered.push_back(0.0);
    }

    auto count =(double) filtered.size();
    double mean =accumulate(filtered.begin(),filtered.end(),0.0)/count;
    std::vector<double> finalArr;
    for(int i =1; i<filtered.size()-1;i++){
        if(filtered.at(i)>filtered.at(i-1) && filtered.at(i)>filtered.at(i+1)){
            if(filtered.at(i)>mean){
                finalArr.push_back(filtered.at(i));
            }
        }
    }
    env->ReleaseDoubleArrayElements(tmpArray_, tmpArray, 0);

    // return the steps
    return  finalArr.size()*2;
}
extern "C"
__unused JNIEXPORT jstring JNICALL
Java_com_example_mistepcount_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
}