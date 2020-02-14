import random
import math
import numpy as np

def Normalize(data):
    m = np.mean(data)
    mx = max(data)
    mn = min(data)
    return [(float(i) - m) / (mx - mn) for i in data]

with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase.data') as file:
    data = []
    temp = []
    for line in file:
        lst = line.split(',')
        lst[-1] = lst[-1][:-1]
        
        lst = list(map(float, lst))
        
        temp.append(int(lst[-1]))
        # mean = np.mean(lst[:-1])
        # std = np.std(lst[:-1], ddof=1)
        
        # for i in range(len(lst)-1):
            # temp.append(' {}:{:.17f}'.format(i+1, lst[i]))
        data.append(lst[:-1])

    mean = np.mean(data, axis=0)
    std = np.std(data, axis=0, ddof=1)
    data = (data-mean)/std
    data = np.insert(data, 0, temp, 1)
    random.shuffle(data)
    
    with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase_train.dat', 'w') as train_file:
        for i in data[:4140]:
            i = list(map(str, i))
            train_file.write(str(int(float(i[0]))))
            for j in range(len(i[1:])):
                train_file.write(' {}:{}'.format(j+1, i[j+1]))
            train_file.write('\n')
    
    with open('/Users/huangjiaming/Documents/developer/ETreeLearning/res/db/spambase_eval.dat', 'w') as eval_file:
        for i in data[4140:]:
            i = list(map(str, i))
            eval_file.write(str(int(float(i[0]))))
            for j in range(len(i[1:])):
                eval_file.write(' {}:{}'.format(j+1, i[j+1]))
            eval_file.write('\n')