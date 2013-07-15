import processing.core.PApplet;

public class Food {
    float energy;
    float defaultEnergy = 1f;
    float[] position;

    public Food (float energy, float[] position) {
        this.energy = energy;
        this.position = position;
    }

    public Food (float[] position) {
        this.energy = defaultEnergy;
        this.position = position;
    }

    public float getEnergy() {
        return energy;
    }

    public void eat() {
        this.energy = 0f;
    }

    public void eat(float amount) {
        this.energy = this.energy - amount;
    }

    public float[] getPosition() {
        return position;
    }
}