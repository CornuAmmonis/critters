import com.sun.jmx.remote.internal.ArrayQueue;
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.opengl.*;
import java.util.*;
import java.util.*;
import java.util.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Critter4 extends PApplet {



    ArrayList<Spinner> sp;
    ArrayList<Spinner> newSp;

    int agents = 50;
    int maxAgents = 400;
    int maxId = 0;
    int maxType = 1;
    float childEnergy = 1f;

    float vScale = 0.1f; //velocity scale

    float initialE = 10f;
    public final int inputN = 16;

    GlobalProperties globalProperties;

    public void setup() {
        size(640, 480, OPENGL);
        noFill();
        colorMode(HSB, 100);

        sp = new ArrayList<Spinner>();
        for (int i = 0; i < agents; i++) {
            if (i < agents/2) {
                sp.add(i, new Spinner(maxId,0,initialE));
            } else {
                sp.add(i, new Spinner(maxId,1,initialE));
            }
            maxId++;
        }

        globalProperties = new GlobalProperties(width, height);
    }

    public void draw() {
        background(0, 0, 100);
        newSp = new ArrayList(sp);
        float totalEnergy = 0f;
        for (Spinner spinner : sp) {
            try{
                spinner.update();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
            totalEnergy += spinner.getEnergy();
            spinner.draw();
        }
        sp = newSp;
        globalProperties.makeFood();

        totalEnergy += globalProperties.getGlobalEnergy();
        float foodEnergy = globalProperties.draw();
        totalEnergy += foodEnergy;
        System.out.println(totalEnergy  + ", " + foodEnergy + ", " + agents + ", " + maxId);
    }

    public float getTotalEnergy() {
        float totalEnergy = 0f;
        for (Spinner spinner : newSp) {
            totalEnergy += spinner.getEnergy();
        }
        return totalEnergy;
    }

    public Spinner getClosest(Spinner a) {
        Float min = null;
        Spinner closest = null;
        for (Spinner spinner : newSp) {
            if (spinner.getId() != a.getId()) {
                float newDistance = getDistance(spinner.getPosition(), a.getPosition());
                if (min == null || newDistance < min) {
                    min = newDistance;
                    closest = spinner;
                }
            }
        }
        return closest;
    }

    public Spinner getClosestOfSameType(Spinner a, boolean same) {
        Float min = null;
        Spinner closest = null;
        for (Spinner spinner : newSp) {
            if (spinner.getId() != a.getId()) {
                if (same) {
                    if (spinner.getType() == a.getType()) {
                        float newDistance = getDistance(spinner.getPosition(), a.getPosition());
                        if (min == null || newDistance < min) {
                            min = newDistance;
                            closest = spinner;
                        }
                    }
                } else {
                    if (spinner.getType() != a.getType()) {
                        float newDistance = getDistance(spinner.getPosition(), a.getPosition());
                        if (min == null || newDistance < min) {
                            min = newDistance;
                            closest = spinner;
                        }
                    }
                }
            }
        }
        return closest;
    }

    public float getDistance(float[] a, float[] b) {
        return (float)Math.sqrt(Math.pow(a[0] - b[0], 2d) + Math.pow(a[1] - b[1], 2d));
    }

    public boolean interactDistance(float distance, float radius) {
        return distance < radius;
    }

    public float[] getDistanceVector(float[] a, float[] b) {
        float[] vector = new float[2];
        vector[0] = a[0] - b[0];
        vector[1] = a[1] - b[1];
        return vector;
    }

    public float[] getUnitDistanceVector(float[] a, float[] b) {
        float[] distanceVector = getDistanceVector(a, b);
        float distance = getDistance(a, b);
        float[] unitDistanceVector = new float[2];
        unitDistanceVector[0] = distanceVector[0] / distance;
        unitDistanceVector[1] = distanceVector[1] / distance;
        return unitDistanceVector;
    }


    class Spinner {
        int id;
        int type;
        float energy;
        Network net;
        long birthday;
        boolean alive = true;

        float[] position = new float[2];
        float[] velocity = new float[2];

        Spinner(int id, int type, float energy) {
            this.type = type;
            this.id = id;
            this.energy = energy;
            velocity[0] = random(1f) - 0.5f;
            velocity[1] = random(1f) - 0.5f;
            position[0] = random((float)width);
            position[1] = random((float)height);
            this.net = new Network();
            this.birthday = System.currentTimeMillis();
        }

        Spinner(int id, int type, float energy, Network net) {
            this.type = type;
            this.id = id;
            this.energy = energy;
            velocity[0] = random(1f) - 0.5f;
            velocity[1] = random(1f) - 0.5f;
            position[0] = random((float)width);
            position[1] = random((float)height);
            this.net = net;
            this.birthday = System.currentTimeMillis();
        }

        public int getId() {
            return id;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public boolean getAlive() {
            return alive && getAge() < 100000;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

        public long getAge() {
            return System.currentTimeMillis() - this.birthday;
        }

        public float[] getPosition() {
            return position;
        }

        public void setPosition(float[] position) {
            if (Float.isNaN(position[0]) || Float.isNaN(position[1])) {
                throw new RuntimeException("it's nan");
            }
            this.position = position;
        }

        public float[] getVelocity() {
            return velocity;
        }

        public float getEnergy() {
            return energy;
        }

        public void setEnergy(float energy) {
            this.energy = energy;
        }

        public Network getNetwork() {
            return net;
        }

        public void setNetwork(Network net) {
            this.net = net;
        }

        public float[] averager(float[] velocity, float[] prevVelocity, float ratio) {
            float[] newVelocity = new float[2];
            newVelocity[0] = velocity[0]*ratio + prevVelocity[0]*(1-ratio);
            newVelocity[1] = velocity[1]*ratio + prevVelocity[1]*(1-ratio);
            return newVelocity;
        }

        public void update() throws Exception {
            if (getAlive()) {
                setNetInput();
                net.update();

                Spinner closestSame = getClosestOfSameType(this, true);
                Spinner closestDifferent = getClosestOfSameType(this, false);
                float distanceSame;
                float distanceDifferent;

                if (closestSame == null) {
                    distanceSame = 0f;
                } else {
                    distanceSame = getDistance(closestSame.getPosition(), this.getPosition());
                }

                if (closestDifferent == null) {
                    distanceDifferent = 0f;
                } else {
                    distanceDifferent = getDistance(closestDifferent.getPosition(), this.getPosition());
                }

                float[] netOutput = net.getVOutput();
                float[] rawVelocityOutput = new float[2];
                rawVelocityOutput[0] = vScale * (netOutput[0] - netOutput[1]);
                rawVelocityOutput[1] = vScale * (netOutput[2] - netOutput[3]);
                velocity = averager(rawVelocityOutput, velocity, 0.4f);

                boolean mated = false;
                boolean reproduce = net.getFired()[4];
                if (reproduce && closestSame != null && interactDistance(distanceSame, getRadius() + closestSame.getRadius())) {
                    if (agents < maxAgents) {
                        if (mate(this, closestSame)) {
                            mated = true;
                            agents++;
                            maxId++;
                        }
                    }
                }
                if (reproduce && !mated) {
                    if (agents < maxAgents) {
                        if (mate(this, this)) {
                            agents++;
                            maxId++;
                        }
                    }
                }
                boolean attack = net.getFired()[5];
                if (attack && closestDifferent != null && interactDistance(distanceDifferent, getRadius() + closestDifferent.getRadius())) {
                    if (attack(this, closestDifferent)) {
                        agents--;
                    }
                }

                eat();

                float velocityEnergyScale = 0.0001f;
                float velocityMagnitude = (float)Math.sqrt(Math.pow(velocity[0], 2f) + Math.pow(velocity[1], 2f));
                float selfEnergy = this.getEnergy();
                float energyLoss = velocityEnergyScale * (selfEnergy * (float)Math.pow(velocityMagnitude, 2f));


                if (energyLoss > selfEnergy) {
                    this.setEnergy(0f);
                    float ratio = selfEnergy / energyLoss;
                    velocity[0] = ratio * (velocity[0] / velocityMagnitude);
                    velocity[1] = ratio * (velocity[1] / velocityMagnitude);
                    energyLoss = selfEnergy;
                    this.setAlive(false);
                } else {
                    this.setEnergy(selfEnergy - energyLoss);
                }

                position[0] = pm(position[0] + velocity[0], (float) width);
                position[1] = pm(position[1] + velocity[1], (float) height);

                globalProperties.addEnergy(energyLoss);

                //Cost of living
                float costOfLivingEnergyScale = 0.01f;
                float costOfLiving = this.getEnergy() * costOfLivingEnergyScale;
                globalProperties.addEnergy(costOfLiving);
                this.setEnergy(this.getEnergy() - costOfLiving);

                if (this.getEnergy() <= 0) {
                    globalProperties.addEnergy(-1f * this.getEnergy());
                    this.setAlive(false);
                    this.setEnergy(0f);
                }
            }
        }

        public void setNetInput() {
            float[] input = new float[inputN];
            Spinner closestSame = getClosestOfSameType(this, true);
            Spinner closestDifferent = getClosestOfSameType(this, false);
            Food closestFood = globalProperties.getClosest(this.getPosition());
            float[] distanceVectorSame;
            float[] distanceVectorDifferent;
            float[] distanceVectorFood;
            if (closestSame != null) {
                distanceVectorSame = getDistanceVector(closestSame.getPosition(), this.getPosition());
            } else {
                distanceVectorSame = new float[]{0f, 0f};
            }
            if (closestDifferent != null) {
                distanceVectorDifferent = getDistanceVector(closestDifferent.getPosition(), this.getPosition());
            } else {
                distanceVectorDifferent = new float[]{0f, 0f};
            }
            if (closestFood != null) {
                distanceVectorFood = getDistanceVector(closestFood.getPosition(), this.getPosition());
            } else {
                distanceVectorFood = new float[]{0f, 0f};
            }
            float[] velocity = getVelocity();

            input[0] = distanceVectorSame[0];
            input[1] = distanceVectorSame[1];
            input[2] = distanceVectorDifferent[0];
            input[3] = distanceVectorDifferent[1];
            input[4] = velocity[0];
            input[5] = velocity[1];
            if (closestSame != null) {
                input[6] = closestSame.getEnergy();
                input[7] = activation(closestSame.getAge(), 1f, 1E-5f, 3f);
                input[8] = closestSame.getAlive() ? 1f : 0f;
            } else {
                input[6] = 0f;
                input[7] = 0f;
                input[8] = 0f;
            }
            if (closestDifferent != null) {
                input[9] = closestDifferent.getEnergy();
                input[10] = activation(closestDifferent.getAge(), 1f, 1E-5f, 3f);
                input[11] = closestDifferent.getAlive() ? 1f : 0f;
            } else {
                input[9] = 0f;
                input[10] = 0f;
                input[11] = 0f;
            }
            input[12] = activation(this.getAge(), 1f, 1E-5f, 3f);
            input[13] = this.getEnergy();
            input[14] = distanceVectorFood[0];
            input[15] = distanceVectorFood[1];
            net.setInputs(input);
        }

        public float activation(float x, float scaleRange, float scaleDomain, float shift) {
            return scaleRange * ((float) Math.tanh((x + shift) * scaleDomain) + 1f);
        }

        //returns true if a new agent was created
        public boolean mate(Spinner self, Spinner target) throws Exception {
            if (!target.getAlive()) {
                return false;
            }
            float threshold = 0.1f;
            float minRemainder = 0.05f;

            //prevent massive amount of inbreeding
            if (self.getAge() < 1000 || target.getAge() < 1000) {
                return false;
            }

            final float childEnergy = 1f;
            final float minEnergy = 0.1f;
            float minE = Math.min(self.getEnergy(), target.getEnergy());
            float maxE = Math.max(self.getEnergy(), target.getEnergy());

            if (!newSp.contains(target) || !newSp.contains(self) || minE < minEnergy) {
                return false;    //crappy way of handling concurrency issues
            }

            boolean selfIsMax = self.getEnergy() > target.getEnergy();

            float maxContribution = (maxE / (minE + maxE)) * childEnergy;
            float minContribution = (minE / (minE + maxE)) * childEnergy;

            if (minE - minContribution < minRemainder || maxE - maxContribution < minRemainder || (self == target && minE - childEnergy < minRemainder)) {
                return false;
            }

            int newId = maxId;
            if (minE + maxE >= childEnergy) {
                int type = self.getType();
                if (self.getNetwork().compare(target.getNetwork()) > threshold) {
                    type = ++maxType;
                }

                float mutation = 0.1f;
                Network newNet = new Network(self.getNetwork(), target.getNetwork(), mutation);
                Spinner newSpinner = new Spinner(newId, type, childEnergy, newNet);
                if (self == target) {
                    self.setEnergy(maxE - childEnergy);
                } else if (selfIsMax) {
                    self.setEnergy(maxE - maxContribution);
                    target.setEnergy(minE - minContribution);
                } else {
                    self.setEnergy(minE - minContribution);
                    target.setEnergy(maxE - maxContribution);
                }
                double angle = random(2f * (float)Math.PI);
                float[] newPosition = new float[2];
                newPosition[0] = self.getPosition()[0] + self.getRadius() * (float)Math.cos(angle);
                newPosition[1] = self.getPosition()[1] + self.getRadius() * (float)Math.sin(angle);

                newSpinner.setPosition(newPosition);
                newSp.add(newSpinner);
                return true;
            }
            return false;
        }

        //returns true if someone died
        public boolean attack(Spinner self, Spinner target) {
            if (!newSp.contains(target) || !newSp.contains(self)) {
                return false;    //crappy way of handling concurrency issues
            }

            float rdm = random(1f);
            //float ratio = self.getEnergy() / (target.getEnergy() + self.getEnergy());
            float targetEnergy = target.getEnergy();
            float selfEnergy = self.getEnergy();
            float energyWager = Math.min(self.getEnergy(), target.getEnergy());
            boolean allSelfEnergy = targetEnergy >= selfEnergy;
            boolean allTargetEnergy = selfEnergy >= targetEnergy;
            if (rdm < 0.5 || !target.getAlive()) {
                self.setEnergy(selfEnergy + energyWager);
                target.setEnergy(targetEnergy - energyWager);
                if (allTargetEnergy) {
                    newSp.remove(target);
                    self.setEnergy(self.getEnergy());
                    return true;
                } else {
                    return false;
                }
            } else {
                target.setEnergy(targetEnergy + energyWager);
                self.setEnergy(selfEnergy - energyWager);
                if (allSelfEnergy) {
                    newSp.remove(self);
                    target.setEnergy(target.getEnergy());
                    return true;
                } else {
                    return false;
                }
            }
        }

        public boolean eat() {
            Food closestFood = globalProperties.getClosest(this.getPosition());
            if (closestFood == null) {
                return false;
            }
            float distance = getDistance(this.getPosition(), closestFood.getPosition());
            if (interactDistance(distance, this.getRadius())) {
                this.setEnergy(this.getEnergy() + closestFood.getEnergy());
                globalProperties.removeFood(closestFood);
                return true;
            }
            return false;
        }

        public float getRadius() {
            return sqrt(10f*energy)*2f;
        }

        public void draw() {
            if (getAlive()) {
                stroke((51 * this.getType()) % 100, 100, 100);
            } else {
                stroke(0, 0, 0);
            }
            strokeWeight(2);

            ellipse(position[0], position[1], getRadius() * 2f, getRadius() * 2f);
        }
    }

    public int pm(int x, int m){
        int mod = x%m;
        return (mod < 0 ? m + mod : mod);
    }

    public float pm(float x, float m){
        float mod = x%m;
        return (mod < 0f ? m + mod : mod);
    }

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "Critter4" };
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }

    public class GlobalProperties {

        public Queue<Food> foods;
        private float globalEnergy = 0f;
        public int width, height;

        public GlobalProperties(int width, int height) {
            this.width = width;
            this.height = height;
            foods = new LinkedList<Food>();
        }

        //returns total energy for convenience
        public float draw() {
            float totalEnergy = 0f;
            for (Food food : foods) {
                drawFood(food.getPosition());
                totalEnergy += food.getEnergy();
            }
            return totalEnergy;
        }

        public void makeFood() {
            float foodSize = 1f;

            /*while(globalEnergy > foodSize) {
                foods.add(new Food(foodSize, getRandomPositionVector()));
                globalEnergy -= foodSize;
            }*/

            foods.add(new Food(globalEnergy, getRandomPositionVector()));
            globalEnergy = 0f;
        };

        public float[] getRandomPositionVector() {
            float position[] = new float[2];
            position[0] = (float)(width * Math.random());
            position[1] = (float)(height * Math.random());
            return position;
        }

        public  void addEnergy(float energy) {
            globalEnergy += energy;
        }

        public float getGlobalEnergy() {
            return globalEnergy;
        }

        public Queue<Food> getFoods() {
            return foods;
        }

        public void removeFood(Food food) {
            foods.remove(food);
        }

        public float getDistance(float[] a, float[] b) {
            return (float)Math.sqrt(Math.pow(a[0] - b[0], 2d) + Math.pow(a[1] - b[1], 2d));
        }

        public float[] getDistanceVector(float[] a, float[] b) {
            float[] vector = new float[2];
            vector[0] = a[0] - b[0];
            vector[1] = a[1] - b[1];
            return vector;
        }

        public Food getClosest(float[] x) {
            Float min = null;
            Food closest = null;
            for (Food food : foods) {
                float newDistance = getDistance(food.getPosition(), x);
                if (min == null || newDistance < min) {
                    min = newDistance;
                    closest = food;
                }
            }
            return closest;
        }

        public void drawFood(float[] position) {
            stroke(0, 0, 0);
            strokeWeight(2);
            ellipse(position[0], position[1], 2f, 2f);
        }

    }
}
