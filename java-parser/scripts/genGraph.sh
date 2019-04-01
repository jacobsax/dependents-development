# graphml2gv -o convgraph.dot graph.graphml

dot -Tpng graph.dot > output.png

open -a "Xee³" ./output.png