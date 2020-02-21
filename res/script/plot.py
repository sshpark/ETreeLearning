import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

filenames = [
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_2000_layer_1.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_2000_layer_2.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_2000_layer_3.txt',
]
labels = ['3 layers', '4 layers', '5 layers']
lineStyle = ['m-.', 'b-.', 'r-.']
cnt = 0

for filepath in filenames:
    x = [10]
    y = [0.4229934924078091]
    with open(filepath) as file:
        for line in file:
            a, b = line.split()
            x.append(int(a))
            y.append(float(b))
        plt.plot(x, y, lineStyle[cnt], label=labels[cnt])
    cnt += 1

plt.title("2000 nodes")
plt.xlabel("Simulation time")
plt.ylabel("0-1 Error")

plt.legend()
plt.grid(True)
plt.xscale('log')
plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
plt.savefig('./etree_2000_layer.png', dpi=600)


