from cyaron import *
import random
from sys import argv
import numpy as np

n = int(argv[1])
delayMean = int(argv[2])
delayVar = int(argv[3])

test_data = IO(file_prefix='./data'+str(n), data_id="")

m = 2*n

graph = Graph.UDAG(n, m, self_loop=False, repeated_edges=False)

for edge in graph.iterate_edges():
#     weight = delayMean + np.random.randn()*delayVar
    edge.weight = 1
    test_data.input_writeln(edge) # 输出这条边，以u v w的形式
