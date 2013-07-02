import java.util.List;
import java.util.Random;

public class NeuronParams {
    public float a, b, c, d;
    public float[] weights;
    public NeuronParams() {}

    public void mutate(float mutation) {
        a = mutate(a, mutation);
        b = mutate(b, mutation);
        c = mutate(c, mutation);
        d = mutate(d, mutation);
        for (int i = 0; i < weights.length; i++) {
            weights[i] = mutate(weights[i], mutation);
        }
    }

    public float mutate(float x, float mutation) {
        Random random = new Random();
        return x * ((float)random.nextGaussian() * mutation + 1f);
    }

    public NeuronParams clone() {
        NeuronParams neuronParams = new NeuronParams();
        neuronParams.a = this.a;
        neuronParams.b = this.b;
        neuronParams.c = this.c;
        neuronParams.d = this.d;
        neuronParams.weights = this.weights.clone();
        return neuronParams;
    }
}