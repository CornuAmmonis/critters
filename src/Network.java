import java.util.ArrayList;
import java.util.List;

class Network {
    float[] inputs;
    int inputNs = 14;
    int hiddenNs = 10;
    int outputNs = 5;

    final float scale = 10f;

    float ratioInhib = 0.2f;
    List<Neuron> neurons = new ArrayList<Neuron>();

    public Network(List<Neuron> neurons) {
        this.neurons = neurons;
    }

    public Network(Network network1, Network network2, float mutation) throws Exception {
        List<Neuron> neurons1 = network1.getNeurons();
        List<Neuron> neurons2 = network2.getNeurons();
        List<Neuron> newNeurons = new ArrayList<Neuron>();
        if (neurons1.size() != neurons2.size()) {
            throw new Exception("Neuron count must be the same between networks.");
        }
        for (int i = 0; i < neurons1.size(); i++) {
            if (Math.random() < 0.5f) {
                newNeurons.add(neurons1.get(i).clone(this, mutation));
            } else {
                newNeurons.add(neurons2.get(i).clone(this, mutation));
            }
        }
        neurons = newNeurons;
    }

    public Network() {
        for (int i = 0; i < inputNs; i++) {
            Neuron n = createRandomNeuron(inputNs + hiddenNs);

            List<Connection> connections = new ArrayList<Connection>();
            for (int j = 0; j < inputNs; j++) {
                connections.add(new ExternalConnection(j, this, scale / inputNs));
            }

            //feedback loop to hidden layer
            for (int j = inputNs; j < inputNs + hiddenNs; j++) {
                connections.add(new InternalConnection(j, this, scale / hiddenNs));
            }
            n.setInputs(connections);

            neurons.add(n);
        }
        for (int i = 0; i < hiddenNs; i++) {
            Neuron n = createRandomNeuron(inputNs);

            List<Connection> connections = new ArrayList<Connection>();
            for (int j = 0; j < inputNs; j++) {
                connections.add(new InternalConnection(j, this, scale / inputNs));
            }
            n.setInputs(connections);

            neurons.add(n);
        }
        for (int i = 0; i < outputNs; i++) {
            Neuron n = createRandomNeuron(hiddenNs);

            List<Connection> connections = new ArrayList<Connection>();
            for (int j = inputNs; j < inputNs + hiddenNs; j++) {
                connections.add(new InternalConnection(j, this, scale / hiddenNs));
            }
            n.setInputs(connections);

            neurons.add(n);
        }
    }

    public Neuron createRandomNeuron(int synapses) {
        NeuronParams neuronParams;
        if (random(1f) > ratioInhib) {
            neuronParams = getExcitatoryParams();
            neuronParams.weights = getRandomWeights(true, synapses);
        } else {
            neuronParams = getInhibitoryParams();
            neuronParams.weights = getRandomWeights(false, synapses);
        }
        return new Neuron(neuronParams);
    }

    public float[] getRandomWeights(boolean excitatory, int size) {
        float[] weights = new float[size];
        for (int i = 0; i < size; i++) {
            if (excitatory) {
                weights[i] = random(0.5f);
            } else {
                weights[i] = -random(1f);
            }
        }
        return weights;
    }

    public NeuronParams getExcitatoryParams() {
        NeuronParams neuronParams = new NeuronParams();
        float rand = random(1f);
        neuronParams.a = 0.02f;
        neuronParams.b = 0.2f;
        neuronParams.c = -65 + 15 * pow(rand, 2f);
        neuronParams.d = 8 - 6 * rand;
        return neuronParams;
    }

    public NeuronParams getInhibitoryParams() {
        NeuronParams neuronParams = new NeuronParams();
        float rand = random(1f);
        neuronParams.a = 0.02f + 0.08f * rand;
        neuronParams.b = 0.25f - 0.05f * rand;
        neuronParams.c = -65;
        neuronParams.d = 2;
        return neuronParams;
    }

    public float[] getInputs() {
        return inputs;
    }

    public float getInput(int i) {
        return inputs[i];
    }

    public void setInputs(float[] inputs) {
        this.inputs = inputs;
    }

    private class ExternalConnection implements Connection {
        int inputN;
        Network net;
        float scale;
        public ExternalConnection(int inputN, Network net, float scale) {
            this.inputN = inputN;
            this.net = net;
            this.scale = scale;
        }

        public float getValue() {
            return scale * net.getInput(inputN);
        }

        public void setNetwork(Network net) {
            this.net = net;
        }

        public ExternalConnection clone(Network net) {
            return new ExternalConnection(this.inputN, net, this.scale);
        }
    }

    private class InternalConnection implements Connection {
        int neuronN;
        Network net;
        float scale;
        public InternalConnection(int neuronN, Network net, float scale) {
            this.neuronN = neuronN;
            this.net = net;
            this.scale = scale;
        }

        public float getValue() {
            return scale *  (net.getNeuron(neuronN).getFired() ? 1f : 0f);
        }

        public void setNetwork(Network net) {
            this.net = net;
        }

        public InternalConnection clone(Network net) {
            return new InternalConnection(this.neuronN, net, this.scale);
        }
    }

    public void update() {
        for (Neuron n : neurons) {
            n.update();
        }
    }

    public float[] getOutput() {
        float[] output = new float[outputNs];
        for (int i = 0; i < outputNs; i++) {
            output[i] = getOutputNeuron(i).getFired() ? 1f : 0f;
        }
        return output;
    }

    public boolean[] getFired() {
        boolean[] output = new boolean[outputNs];
        for (int i = 0; i < outputNs; i++) {
            output[i] = getOutputNeuron(i).getFired();
        }
        return output;
    }

    public float[] getVOutput() {
        float[] output = new float[outputNs];
        for (int i = 0; i < outputNs; i++) {
            output[i] = getOutputNeuron(i).getV();
        }
        return output;
    }

    public Neuron getOutputNeuron(int i) {
        return neurons.get(inputNs + hiddenNs + i);
    }

    public Neuron getNeuron(int i) {
        return neurons.get(i);
    }

    public List<Neuron> getNeurons() {
        return neurons;
    }

    private float random(float r) {
        return (float)Math.random() * r;
    }

    private float pow(float b, float e) {
        return (float)Math.pow(b, e);
    }

    public float compare(Network target) {
        float avgPctDiff = 0f;
        for (int i = 0; i < this.getNeurons().size(); i++) {
            avgPctDiff += this.getNeuron(i).compare(target.getNeuron(i));
        }
        return avgPctDiff / this.getNeurons().size();
    }
}