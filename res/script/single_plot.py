import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

def to_percent(val, position):
    return str(val*100.0) + '%'

def plot_with(title, time):
    plt.title(title)
    plt.title(title)
    if time:
        plt.xlabel('Simulation time')
    else:
        plt.xlabel('Number of rounds')
    plt.grid(True)
    if time: plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))
    plt.ylabel('Accuracy')
    if not time:
        plt.plot([i+1 for i in range(len(acc_y))], acc_y, lossStyle[cnt] , label=losslabels[cnt])
    else:
        temp_x = [i for i in x if i < 90000]
        temp_y = acc_y[:len(temp_x)]
        plt.plot(temp_x, temp_y, lossStyle[cnt] , label=losslabels[cnt])
    plt.legend()

def plot_save(title, xlabel, cnt, flag):
    plt.subplot(1,2,1)
    plt.title(title)
    plt.xlabel('Simulation time')
    plt.grid(True)
    plt.ylabel('Accuracy')
    temp_x = [i for i in x if i < 90000]
    temp_y = acc_y[:len(temp_x)]

    plt.plot(temp_x, temp_y, lossStyle[cnt] , label=losslabels[cnt])
    plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))
    plt.legend()

    plt.subplot(1,2,2)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.grid(True)
    plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))
    plt.ylabel('Accuracy')
#     if flag:
#         plt.plot([i+1 for i in range(len(acc_y))], acc_y, lossStyle[cnt] , label=losslabels[cnt]) # E-Tree
#     else:
    plt.plot([i+1 for i in range(len(acc_y))], acc_y, lossStyle[cnt] , label=losslabels[cnt])
    plt.legend()

filenames = [
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/20200309/gossip_100_noniid.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/20200309/fed_100_noniid.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/20200309/etree_100_5_20_noniid.txt'
]
losslabels = ['Gossip Learning', 'Federated Learning', 'E-Tree Learning', 'Only mid-layer Non-IID (4)', 'ETree Learning with 30']
acclabels = ['Federated Learning accuracy', 'ETree Learning accuracy']
lossStyle = ['b-', 'r-', 'm-', 'g-']
accStyle = ['b-', 'r-', 'm-']
cnt = 0

plt.figure(figsize=(6, 4))

for filepath in filenames:
    x = []
    single_y = []
    loss_y = []
    acc_y = []
    flag = 'etree' in filepath
    if 'gossip' not in filepath:
        x.append(10)
        acc_y.append(0.1672887682388875)

    with open(filepath) as file:
        for line in file:
            a, b, c = line.split()
            x.append(int(a))
            loss_y.append(float(b))
            acc_y.append(float(c))
        print(np.mean(acc_y[-10:]), np.max(acc_y[-10:]), np.min(acc_y[-10:]), np.max(acc_y[-10:])-np.min(acc_y[-10:]), np.std(acc_y[-10:]))
        plot_with('100 nodes, Non-IID(4) Setting', False)
    cnt += 1

plt.savefig('reports/20200310/E5.png', format='png', dpi=600)

# Simulation time