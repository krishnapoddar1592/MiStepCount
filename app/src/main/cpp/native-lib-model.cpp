//
// Created by Krishna on 3/1/2023.
//


#include <jni.h>
#include <string>
#include<vector>
#include <numeric>
#include <iostream>
#include<cmath>


// Define the sigmoid activation function
double sigmoid(double x) {
    return 1.0 / (1.0 + exp(-x));
}

// Define the softmax activation function
void softmax(double* x, int size) {
    double max = x[0];
    for (int i = 1; i < size; i++) {
        if (x[i] > max) {
            max = x[i];
        }
    }
    double sum = 0.0;
    for (int i = 0; i < size; i++) {
        x[i] = exp(x[i] - max);
        sum += x[i];
    }
    for (int i = 0; i < size; i++) {
        x[i] /= sum;
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mistepcount_NativeLibraryModel_passingDataToJni(JNIEnv *env, jobject thiz,
                                                                 jdoubleArray tmp_array,
                                                                 jint tmp_int, jint tmp_double) {
    // TODO: implement passingDataToJni()
    jdouble *tmpArray = env->GetDoubleArrayElements(tmp_array, nullptr);
    double *input =tmpArray;

    // Define the input vector
//    double input[3] = {-0.0635, 0.6158, 0.141};

    // Define the weights and biases for the neural network
    double weights[6][6] ={
            {-0.41824317, -0.39353704,  0.77229846,  0.04026904,  0.5455875 ,
                    0.22484505},
            {0.32595626, -0.27814877, -0.72809345, -0.8610597 ,  0.2196164 ,
                    -0.9432441 },
            { 0.40018764,  0.337537  , -0.42208102,  0.2513303 , -0.57823   ,
                    0.7259865 },
            { -0.32541445,  0.86695474, -0.74960655,  0.1781777 , -0.16892354,
                    0.06061861},
            { 0.15862009, -0.40477827,  0.8076944 ,  0.88186044,  0.15543662,
                    -0.78333634},
            {-0.08557282, -0.34685653, -0.3723629 , -1.2388456 ,  0.49214515,
                    0.3822176 }
    };
    double biases[6] = {-0.09251881,  0.07049023,  0.00565998, -0.08850232,  0.00353327,
                        0.08052758};

    // Compute the output of the neural network
    double output[6];
    for (int i = 0; i < 6; i++) {
        output[i] = biases[i];
        for (int j = 0; j < 3; j++) {
            output[i] += weights[i][j] * input[j];
        }
        output[i] = sigmoid(output[i]);
    }
    softmax(output, 6);

    // Print the output
//    std::cout << "Output: [";
    double maximum = -500;
    int index =0;
    for (int i = 0; i < 6; i++) {
        if(output[i]>maximum){
            maximum=output[i];
            index=i;
        }
    }
//    std::cout << index;
//    std::cout << "]" << std::endl;
    return index;
}