import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

class Neuron {

    final int queueLength = 8;

    final float scale = 1f;
    final float fThresh = 30.0f;

    NeuronParams neuronParams;
    List<Connection> inputs;
    List<Queue<FiringState>> inputHistory;
    Queue<FiringState> selfHistory;
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
        selfHistory = new ArrayBlockingQueue<FiringState>(queueLength);
    }

    public NeuronParams getNeuronParams() {
        return neuronParams;
    }

    public List<Connection> getInputs() {
        return inputs;
    }

    public void setInputs(List<Connection> inputs) {
        this.inputs = inputs;
        inputHistory = new ArrayList<Queue<FiringState>>();
        for (Connection input : inputs) {
            inputHistory.add(new ArrayBlockingQueue<FiringState>(queueLength));
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
            FiringState fired = inputs.get(j).getFired();
            //fired == null indicates an External Connection which do not fire)
            if (fired == FiringState.DISABLED) {
                input += activation(inputs.get(j).getValue() * weights[j]);
            } else {
                input += inputs.get(j).getFired() == FiringState.FIRED ? weights[j] : 0f;
            }
        }
        if (Float.isNaN(input)) {
            return 0f;
        } else {
            return k * input;
        }
    }

    public void updateSTDP() {
        //size of stdpArray must be equal to queueLength (TODO: parameterize, make evolvable)
        float[] stdpArray = new float[]{-0.125f, -0.25f, -0.5f, -1f, 1f, 0.5f, 0.25f, 0.125f};
        float selfStdp = 0f;
        float learningRate = 0.001f;
        int i = 0;
        for (FiringState fired : selfHistory) {
            if (fired == FiringState.DISABLED) {
                throw new RuntimeException("Neuron firing state should never be FiringState.DISABLED");
            }
            selfStdp += fired == FiringState.FIRED ? stdpArray[i] : 0f;
            i++;
        }
        int inputPosition = 0;
        for (Queue<FiringState> q : inputHistory) {
            weights[inputPosition] = weights[inputPosition] < -1f ? -1f : weights[inputPosition];
            weights[inputPosition] = weights[inputPosition] > 1f ? 1f : weights[inputPosition];
            if (selfStdp == 0f) {
                inputPosition++;
                continue;
            }
            int queuePosition = 0;
            float inputStdp = 0f;
            boolean isExternalConnection = false;
            for (FiringState fired : q) {
                if (fired == FiringState.DISABLED) {
                    isExternalConnection = true;
                } else {
                    inputStdp += fired == FiringState.FIRED ? stdpArray[queuePosition] : 0f;
                    queuePosition++;
                }
            }
            if (inputStdp == 0f || isExternalConnection) {
                inputPosition++;
                continue;
            }
            float totalStdp = selfStdp - inputStdp;
            if (totalStdp > 0.5f || totalStdp < -0.5f) {
                float delta = learningRate * (totalStdp);
                weights[inputPosition] += delta;
            }
            weights[inputPosition] = weights[inputPosition] < -1f ? -1f : weights[inputPosition];
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

    public void updateHistory(FiringState f, Queue q) {
        if (q.size() >= queueLength) {
            q.poll();
        }
        q.add(f);
    }

    public float activation(float x) {
        return scale * ((float) Math.tanh(x) + 1f);
    }

    public FiringState getFired() {
        return fired ? FiringState.FIRED : FiringState.NOTFIRED;
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