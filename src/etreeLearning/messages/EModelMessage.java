package etreeLearning.messages;

import gossipLearning.interfaces.Model;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.messages.Message;
import gossipLearning.messages.ModelMessage;
import peersim.core.Node;

public class EModelMessage extends ModelMessage {

    public double value = 0.0;
    public Node src;

    public EModelMessage(Node src, double value) {
        super(src, new ModelHolder() {
            @Override
            public void init(String prefix) {
                
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public Model getModel(int index) {
                return null;
            }

            @Override
            public void setModel(int index, Model model) {

            }

            @Override
            public boolean add(Model model) {
                return false;
            }

            @Override
            public Model remove(int index) {
                return null;
            }

            @Override
            public void clear() {

            }

            @Override
            public Object clone() {
                return null;
            }
        });
        this.src = src;
        this.value = value;
    }
}
