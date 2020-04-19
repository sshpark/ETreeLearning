from cyaron import *
import random
from sys import argv
import numpy as np

n = int(argv[1])
m = int(argv[2])
delayMean = int(argv[3])
delayVar = int(argv[4])

test_data = IO(file_prefix='../db/data_'+str(n)+'_'+str(m), data_id="")

graph = Graph.UDAG(n, m, self_loop=False, repeated_edges=False)

for edge in graph.iterate_edges():
    weight = delayMean + np.random.randn()*delayVar
    edge.weight = max(1, weight)
    test_data.input_writeln(edge) # 输出这条边，以u v w的形式
