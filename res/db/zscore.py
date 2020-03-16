import random
import math
import numpy as np


with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase.data') as file:
    data = []
    temp = []
    for line in file:
        lst = line.split(',')
        lst[-1] = lst[-1][:-1]
        
        lst = list(map(float, lst))
        temp.append(int(lst[-1]))
        data.append(lst[:-1])

    
    data = np.insert(data, 0, temp, 1)
    np.random.shuffle(data)
    train_data = data[:4140]
    eval_data = data[4140:]

    mean = np.mean(train_data, axis=0)
    std = np.std(train_data, axis=0, ddof=1)
    train_data = (train_data-mean)/std
    eval_data = (eval_data-mean)/std
    
    
    with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase_train.dat', 'w') as train_file:
        for i in train_data:
            i = list(map(str, i))
            train_file.write(str(int(float(i[0]))))
            for j in range(len(i[1:])):
                train_file.write(' {}:{}'.format(j+1, i[j+1]))
            train_file.write('\n')
    
    with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase_eval.dat', 'w') as eval_file:
        for i in eval_data:
            i = list(map(str, i))
            eval_file.write(str(int(float(i[0]))))
            for j in range(len(i[1:])):
                eval_file.write(' {}:{}'.format(j+1, i[j+1]))
            eval_file.write('\n')