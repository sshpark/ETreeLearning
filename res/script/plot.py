import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

filenames = [
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/fed_100.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_100.txt'
]
labels = ['Federated Learning', 'ETree Learning', '5 layers']
lineStyle = ['b->', 'r->', 'm-.']
cnt = 0

for filepath in filenames:
    x = [10]
    y = [0.6931471805599446]
    with open(filepath) as file:
        for line in file:
            a, b = line.split()
            x.append(int(a))
            y.append(float(b))
        plt.plot(x, y, lineStyle[cnt], label=labels[cnt])
    cnt += 1

plt.title("100 nodes")
plt.xlabel("Simulation time")
plt.ylabel("loss")
plt.legend()
plt.grid(True)
plt.xscale('log')
plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
plt.savefig('./test_fed_100.png', dpi=600)