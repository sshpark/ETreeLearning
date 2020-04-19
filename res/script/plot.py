import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

def to_percent(val, position):
    return str(val*100.0) + '%'

def find_closed_index(x, val):
    for i in range(len(x)):
        if x[i] == val:
            return i
        if x[i] > val:
            return i-1
    return len(x)-1

def plot_save(title, xlabel, cnt, flag):
    plt.subplot(1,2,1)
    if flag:
        plt.plot(x, loss_y, lossStyle[cnt], label=losslabels[cnt])
        # temp_x, temp_y = [], []
        # for dot_i in range(10, 101, 10):
        #     idx = find_closed_index(x, dot_i*1000)
        #     temp_x.append(x[idx])
        #     temp_y.append(loss_y[idx])
        # plt.plot(temp_x, temp_y, lossStyle[cnt], mfc="none", label=losslabels[cnt])
    else:
        plt.plot(loss_y, lossStyle[cnt] , label=losslabels[cnt])
    
    plt.grid(True)
    if flag:
        plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.ylabel('Loss')
    plt.title(title)
    plt.xlabel(xlabel)
    plt.legend()

    plt.subplot(1,2,2)
    if flag:
        plt.plot(x, acc_y, lossStyle[cnt] , label=losslabels[cnt])
        # temp_x, temp_y = [], []
        # for dot_i in range(10, 101, 10):
        #     idx = find_closed_index(x, dot_i*1000)
        #     print(x[idx])
        #     temp_x.append(x[idx])
        #     temp_y.append(acc_y[idx])
        # plt.plot(temp_x, temp_y, lossStyle[cnt], mfc="none", label=losslabels[cnt])
    else:
        plt.plot(acc_y, lossStyle[cnt] , label=losslabels[cnt])
    
    plt.grid(True)
    if flag:
        plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.title(title)
    plt.xlabel(xlabel)
    plt.ylabel('Accuracy')
    plt.legend()

filenames = [
    '../losses/temp/fed_100_high.txt',
    '../losses/temp/etree_100_high.txt'
]
losslabels = ['FL', 'E-Tree', 'E-Tree Learning 30']
acclabels = ['Federated Learning accuracy', 'ETree Learning accuracy']
lossStyle = ['b-', 'r-', 'm-.', 'g-']
accStyle = ['b-', 'r-', 'm-']
cnt = 0

plt.figure(figsize=(12, 4))
for filepath in filenames:
    x = []
    loss_y = []
    acc_y = []
    with open(filepath) as file:
        for line in file:
            a, b, c = line.split()
            x.append(int(a))
            loss_y.append(float(b))
            acc_y.append(float(c))
        plot_save('100 nodes', 'Simulation time(s)', cnt, True)
    cnt += 1

plt.savefig('./E5.png', dpi=300)

# Simulation time