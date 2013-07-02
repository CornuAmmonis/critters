import java.util.ArrayList;
import java.util.List;

class Network {
    float[] inputs;
    int inputNs = 5;
    int hiddenNs = 10;
    int outputNs = 4;

    final float scale = 100f;

    float ratioInhib = 0.2f;
    List<Neuron> neurons = new ArrayList<Neuron>();

    public Network(List<Neuron> neurons) {
        this.neurons = neurons;
    }

    public Network() {
        for (int i = 0; i < inputNs; i++) {
            Neuron n = createRandomNeuron(1);

            List<Connection> connections = new ArrayList<Connection>();
            connections.add(new ExternalConnection(i, this, scale));
            n.setInputs(connections);

            neurons.add(n);
        }
        for (int i = 0; i < hiddenNs; i++) {
            Neuron n = createRandomNeuron(inputNs);

            List<Connection> connections = new ArrayList<Connection>();
            for (int j = 0; j < inputNs; j++) {
                connections.add(new InternalConnection(neurons.get(j), scale / inputNs));
            }
            n.setInputs(connections);

            neurons.add(n);
        }
        for (int i = 0; i < outputNs; i++) {
            Neuron n = createRandomNeuron(hiddenNs);

            List<Connection> connections = new ArrayList<Connection>();
            for (int j = inputNs; j < inputNs + hiddenNs; j++) {
                connections.add(new InternalConnection(neurons.get(j), scale / hiddenNs));
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
    }

    private class InternalConnection implements Connection {
        Neuron n;
        float scale;
        public InternalConnection(Neuron n, float scale) {
            this.n = n;
            this.scale = scale;
        }

        public float getValue() {
            return scale * (n.getFired() ? 1f : 0f);
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

    private float random(float r) {
        return (float)Math.random() * r;
    }

    private float pow(float b, float e) {
        return (float)Math.pow(b, e);
    }
}