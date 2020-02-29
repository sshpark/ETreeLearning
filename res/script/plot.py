import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

def to_seconds(ms, position):
    return str(ms/1000) + 's'

filenames = [
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/fed_1000.txt',
    '/Users/huangjiaming/Documents/developer/ETreeLearning/res/losses/etree_1000.txt'
]
losslabels = ['Federated Learning', 'ETree Learning']
acclabels = ['Federated Learning accuracy', 'ETree Learning accuracy']
lossStyle = ['b-.', 'r-.', 'm-.']
accStyle = ['b-', 'r-', 'm-']
cnt = 0

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
        plt.plot([i+1 for i in range(len(loss_y))], acc_y, lossStyle[cnt] , label=losslabels[cnt])
#         _, ax1 = plt.subplots()
#         ax2 = ax1.twinx()
#         ax1.plot(x, loss_y, lossStyle[cnt], label=losslabels[cnt])
#         ax2.plot(x, acc_y, accStyle[cnt], label=acclabels[cnt])
#         ax1.set_xlabel('Simulation time')
#         ax1.set_ylabel('eval loss')
#         ax2.set_ylabel('eval accuracy')
    cnt += 1

plt.title("100 nodes")
plt.legend()
plt.grid(True)
# plt.xscale('log')
plt.gca().xaxis.set_major_formatter(FuncFormatter(to_seconds))
plt.savefig('./test_fed_100.png', dpi=300)