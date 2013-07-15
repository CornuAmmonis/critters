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

    public float compare(NeuronParams target) {
        float avgPctDiff;
        float dA = percentDifference(this.a, target.a);
        float dB = percentDifference(this.b, target.b);
        float dC = percentDifference(this.c, target.c);
        float dD = percentDifference(this.d, target.d);
        avgPctDiff = dA + dB + dC + dD;
        for (int i = 0; i < weights.length; i++) {
            avgPctDiff += percentDifference(this.weights[i], target.weights[i]);
        }
        return avgPctDiff / (4 + weights.length);
    }

    private float percentDifference(float x, float y) {
        return Math.abs(x - y) / (Math.abs(x) + Math.abs(y));
    }
}