import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FuncFormatter

filepath = '/Users/huangjiaming/Documents/developer/ETreeLearning/res/delay/etree.txt'
x = []
num = 0
with open(filepath) as fp:
    for line in fp:
        c = list(map(int, line.split()))
        x = c
    print(np.mean(x), np.std(x))
    #cdf累计概率函数
    plt.hist(x,40,alpha=0.9,cumulative=True,rwidth=0.8)
    plt.xlabel('delay(ms)')
    plt.ylabel('percent')
    plt.savefig('../../reports/20200401/delay_etree_high.png', dpi=600)