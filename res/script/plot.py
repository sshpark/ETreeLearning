import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

def to_percent(val, position):
    return str(val*100.0) + '%'

def plot_save(title, xlabel, cnt, flag):
    plt.subplot(1,2,1)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.grid(True)
    if flag:
        plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.ylabel('Loss')
    if flag:
        plt.plot(x, loss_y, lossStyle[cnt] , label=losslabels[cnt])
    else:
        plt.plot(loss_y, lossStyle[cnt] , label=losslabels[cnt])
    plt.legend()

    plt.subplot(1,2,2)
    plt.title(title)
    plt.xlabel(xlabel)
    plt.grid(True)
    if flag:
        plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
    plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))
    plt.ylabel('Accuracy')
    if flag:
        plt.plot(x, acc_y, lossStyle[cnt] , label=losslabels[cnt])
    else:
        plt.plot(acc_y, lossStyle[cnt] , label=losslabels[cnt])
    plt.legend()

filenames = [
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/fed_100.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_100.txt'
]
losslabels = ['Federated Learning', 'ETree Learning']
acclabels = ['Federated Learning accuracy', 'ETree Learning accuracy']
lossStyle = ['b-.', 'r-.', 'm-.']
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
        plot_save('1000 nodes, HAR', 'Simulation time', cnt, True)
    cnt += 1

plt.savefig('reports/20200301/test.png', dpi=600)

# Simulation time