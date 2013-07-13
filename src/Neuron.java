import java.util.ArrayList;
import java.util.List;

class Neuron {

    final float scale = 1f;

    final float fThresh = 30.0f;

    NeuronParams neuronParams;
    List<Connection> inputs;
    float[] weights;

    float u, v, a, b, c, d;

    boolean fired = false;
    public Neuron(NeuronParams params){
        a = params.a;
        b = params.b;
        c = params.c;
        d = params.d;
        v = -65f;
        u = b * v;
        weights = params.weights;
        neuronParams = params;
    }

    public NeuronParams getNeuronParams() {
        return neuronParams;
    }

    public List<Connection> getInputs() {
        return inputs;
    }

    public void setInputs(List<Connection> inputs) {
        this.inputs = inputs;
    }

    public float[] getWeights() {
        return weights;
    }

    public void setWeights(float[] weights) {
        this.weights = weights;
    }

    public void update() {
        if (fired) {
            v = c;
            u = u + d;
            fired = false;
        } else {
            //Simulate 1 ms frame step (can be repeated for larger frame step size)
            v += 0.5f*((0.04f * v + 5.0f) * v + 140.0f - u + getI());
            u += 0.5f*a * (b * v - u);
            v += 0.5f*((0.04f * v + 5.0f) * v + 140.0f - u + getI());
            u += 0.5f*a * (b * v - u);

            if(v >= fThresh){
                v = fThresh;
                fired = true;
            }
        }
    }

    public float getI() {
        float input = 0f;
        for (int j = 0; j <  inputs.size(); j++) {
            input += activation(inputs.get(j).getValue() * weights[j]);
        }
        return input;
    }

    public float activation(float x) {
        return scale * ((float) Math.tanh(x) + 1f);
    }

    public boolean getFired() {
        return fired;
    }

    public float getV() {
        return v;
    }

    public Neuron clone(Network net) {
        Neuron clone = new Neuron(getNeuronParams().clone());
        List<Connection> inputs = getInputs();
        List<Connection> newConnections = new ArrayList<Connection>();
        for (Connection connection : inputs) {
            newConnections.add(connection.clone(net));
        }
        clone.setInputs(newConnections);
        return clone;
    }

    public Neuron clone(Network net, float mutation) {
        Neuron clone = clone(net);
        NeuronParams newParams = getNeuronParams().clone();
        newParams.mutate(mutation);
        Neuron mutatedClone = new Neuron(newParams);
        mutatedClone.setInputs(clone.getInputs());
        return mutatedClone;
    }

    public float compare(Neuron target) {
        return neuronParams.compare(target.getNeuronParams());
    }

}