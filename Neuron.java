import java.util.List;

class Neuron {

    final float scale = 10f;

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
        //if (fired) System.out.print("*");
        return fired;
    }

    public float getV() {
        return v;
    }

}