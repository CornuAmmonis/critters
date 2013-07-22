import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

class Neuron {

    final int queueLength = 8;

    final float scale = 1f;
    final float fThresh = 30.0f;

    NeuronParams neuronParams;
    List<Connection> inputs;
    List<Queue> inputHistory;
    Queue<Boolean> selfHistory;
    float[] weights;

    float u, v, a, b, c, d, k;

    boolean fired = false;
    public Neuron(NeuronParams params){
        a = params.a;
        b = params.b;
        c = params.c;
        d = params.d;
        k = params.k;
        v = -65f;
        u = b * v;
        weights = params.weights;
        neuronParams = params;
        selfHistory = new ArrayBlockingQueue<Boolean>(queueLength);
    }

    public NeuronParams getNeuronParams() {
        return neuronParams;
    }

    public List<Connection> getInputs() {
        return inputs;
    }

    public void setInputs(List<Connection> inputs) {
        this.inputs = inputs;
        inputHistory = new ArrayList<Queue>();
        for (Connection input : inputs) {
            inputHistory.add(new ArrayBlockingQueue(queueLength));
        }
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
        if (Float.isNaN(v) || Float.isNaN(u)) {
            v = c;
            u = b * v;
            fired = false;
        }
        updateHistory(getFired(), selfHistory);
        updateInputHistory();
        updateSTDP();
    }

    public float getI() {
        float input = 0f;
        for (int j = 0; j <  inputs.size(); j++) {
            input += activation(inputs.get(j).getValue() * weights[j]);
        }
        if (Float.isNaN(input)) {
            return 0f;
        } else {
            return k * input;
        }
    }

    public void updateSTDP() {
        //must be the same as queueLength
        float[] stdpArray = new float[]{-0.125f, -0.25f, -0.5f, -1f, 1f, 0.5f, 0.25f, 0.125f};
        float selfStdp = 0f;
        float learningRate = 0.001f;
        int i = 0;
        for (Boolean fired : selfHistory) {
            selfStdp += fired ? stdpArray[i] : 0f;
            i++;
        }
        if (selfStdp == 0f) {
            return;
        }
        int inputPosition = 0;
        for (Queue<Boolean> q : inputHistory) {
            weights[inputPosition] = weights[inputPosition] < 0f ? 0f : weights[inputPosition];
            weights[inputPosition] = weights[inputPosition] > 1f ? 1f : weights[inputPosition];
            int queuePosition = 0;
            float inputStdp = 0f;
            for (Boolean fired : q) {
                inputStdp += fired ? stdpArray[queuePosition] : 0f;
                queuePosition++;
            }
            if (inputStdp == 0f) {
                continue;
            }
            float totalStdp = selfStdp - inputStdp;
            if (totalStdp > 0.5f || totalStdp < -0.5f) {
                float delta = learningRate * (totalStdp);
                weights[inputPosition] += delta;
            }
            weights[inputPosition] = weights[inputPosition] < 0f ? 0f : weights[inputPosition];
            weights[inputPosition] = weights[inputPosition] > 1f ? 1f : weights[inputPosition];
            inputPosition++;
        }

        /*
            TODO: neuron giving scaling value for STDP, neuron triggering weight changes
         */
    }

    public void updateInputHistory() {
        for (int i = 0; i < inputs.size(); i++) {
            updateHistory(inputs.get(i).getFired(),inputHistory.get(i));
        }
    }

    public void updateHistory(Boolean f, Queue q) {
        if (q.size() >= queueLength) {
            q.poll();
        }
        q.add(f);
    }

    public void updateWeights() {
        final float delta = 0.001f;
        for (int j = 0; j <  inputs.size(); j++) {
            if (inputs.get(j).getFired() && this.getFired()) {
                weights[j] += delta;
                weights[j] = weights[j] > 1f ? 1f : weights[j];
            } else if (inputs.get(j).getFired() && !this.getFired()) {
                weights[j] -= delta;
                weights[j] = weights[j] < 0f ? 0f : weights[j];
            }
        }
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