import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FuncFormatter

filepath = '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/delay_etree.txt'
x = []
num = 0
with open(filepath) as fp:
    for line in fp:
        c = list(map(int, line.split()))
        x = c
    print(np.mean(x), np.std(x))
    fig,(ax0,ax1) = plt.subplots(nrows=2,figsize=(9,6))
    # pdf概率分布图
    ax0.hist(x, 100, normed=1, histtype='bar', facecolor='blue', edgecolor="black", alpha=0.9)
    ax0.set_title('')
    ax0.set_xlabel('Delay / ms')
    ax0.set_ylabel('Percent')

    #cdf累计概率函数
    ax1.hist(x,100,normed=1,histtype='bar',facecolor='red', edgecolor="black", alpha=0.9,cumulative=True,rwidth=0.8)
    ax1.set_title("cdf")
    ax1.set_xlabel('Delay / ms')
    ax1.set_ylabel('Percent')

    fig.subplots_adjust(hspace=0.4)
    plt.savefig('./reports/20200301/delay_etree_100_nodes', dpi=600)