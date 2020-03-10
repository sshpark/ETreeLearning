import sys
f = open('C:\\Users\JmHuang\\Documents\\developer\\Gossip-Learning-Framework\\res\\db\\spambase_train.dat')
fw = open('C:\\Users\\JmHuang\\Documents\\developer\\Gossip-Learning-Framework\\res\\db\\spambase_train_1.dat', 'a+')
for line in f:
    lst = line.split(',')
    x = float(lst[-1][:-1])
    fw.write(str(int(x)))
    for num in range(len(lst)-1):
        fw.write(' {}:{}'.format(num+1, lst[num]))
    fw.write('\n')
f.close()
fw.close()