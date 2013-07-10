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
public final int inputN = 14;

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
    System.out.println(totalEnergy  + ", " + newSp.size() + ", " + agents + ", " + maxId);
    sp = newSp;
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
      float newdist = getDistance(spinner.getPosition(), a.getPosition());
      if (min == null || newdist < min) {
        min = newdist;
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
                    float newdist = getDistance(spinner.getPosition(), a.getPosition());
                    if (min == null || newdist < min) {
                        min = newdist;
                        closest = spinner;
                    }
                }
            } else {
                if (spinner.getType() != a.getType()) {
                    float newdist = getDistance(spinner.getPosition(), a.getPosition());
                    if (min == null || newdist < min) {
                        min = newdist;
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

    float usedEnergy = 0f;

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
        return !alive || getAge() < 10000;
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

    public float getUsedEnergy() {
        return usedEnergy;
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
          float selfEnergy = getEnergy();
          setNetInput();
          net.update();
          float rdm = random(1f);

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
          boolean reproduce = net.getFired()[4];
          velocity = averager(rawVelocityOutput, velocity, 0.4f);

          boolean mated = false;
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
          if (closestDifferent != null && interactDistance(distanceDifferent, getRadius() + closestDifferent.getRadius())) {
              float tE = closestDifferent.getEnergy();
              float sE = this.getEnergy();
              if (attack(this, closestDifferent)) {
                  agents--;
              }
              float tE2 = closestDifferent.getEnergy();
              float sE2 = this.getEnergy();
              if ((tE2 + sE2) - (tE + sE) > 0.001f) {
                  throw new Exception("energy violation");
              }
          }

          if (agents < maxAgents) {
              float energyScale = 0.001f;
              float velocityMagnitude = (float)Math.sqrt(Math.pow(velocity[0], 2f) + Math.pow(velocity[1], 2f));
              selfEnergy = this.getEnergy();
              float energyLoss = energyScale * (selfEnergy * (float)Math.pow(velocityMagnitude, 2f));
              this.setEnergy(selfEnergy - energyLoss);

              if (selfEnergy < energyLoss) {
                  this.setEnergy(0f);
                  float ratio = selfEnergy / energyLoss;
                  velocity[0] = ratio * (velocity[0] / velocityMagnitude);
                  velocity[1] = ratio * (velocity[1] / velocityMagnitude);
                  energyLoss = selfEnergy;
                  this.setAlive(false);
              }

              position[0] = pm(position[0] + velocity[0], (float) width);
              position[1] = pm(position[1] + velocity[1], (float) height);

              if (usedEnergy  > childEnergy) {
                  usedEnergy -= childEnergy;
                  int newId = maxId;
                  int type = this.getType();
                  Spinner newSpinner = new Spinner(newId, type, energyLoss);
                  newSpinner.setAlive(false);
                  newSp.add(newSpinner);
                  agents++;
              }
          }
      }
  }
  
  public void setNetInput() {
    float[] input = new float[inputN];
    Spinner closestSame = getClosestOfSameType(this, true);
    Spinner closestDifferent = getClosestOfSameType(this, false);
    float[] distanceVectorSame = new float[2];
    float[] distanceVectorDifferent = new float[2];
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
    float[] velocity = getVelocity();
    
    input[0] = distanceVectorSame[0];
    input[1] = distanceVectorSame[1];
    input[2] = distanceVectorDifferent[0];
    input[3] = distanceVectorDifferent[1];
    input[4] = velocity[0];
    input[5] = velocity[1];
    if (closestSame != null) {
        input[6] = closestSame.getRadius();
        input[7] = activation(closestSame.getAge(), 1f, 1E-5f, 3f);
        input[8] = closestSame.getAlive() ? 1f : 0f;
    } else {
        input[6] = 0f;
        input[7] = 0f;
        input[8] = 0f;
    }
    if (closestDifferent != null) {
        input[9] = closestDifferent.getRadius();
        input[10] = activation(closestDifferent.getAge(), 1f, 1E-5f, 3f);
        input[11] = closestDifferent.getAlive() ? 1f : 0f;
    } else {
        input[9] = 0f;
        input[10] = 0f;
        input[11] = 0f;
    }
    input[12] = activation(this.getAge(), 1f, 1E-5f, 3f);
    input[13] = this.getEnergy();

    net.setInputs(input);
  }

    public float activation(float x, float scaleRange, float scaleDomain, float shift) {
        return scaleRange * ((float) Math.tanh((x + shift) * scaleDomain) + 1f);
    }

    //returns true if a new agent was created
    public boolean mate(Spinner self, Spinner target) throws Exception {
        float selfEnergy = self.getEnergy();
        float targetEnergy = target.getEnergy();
        float newEnergy = 0f;
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
          self.setEnergy(self.getEnergy() + target.getUsedEnergy());
          return true;
      } else {
          return false;
      }
    } else {
      target.setEnergy(targetEnergy + energyWager);
      self.setEnergy(selfEnergy - energyWager);
      if (allSelfEnergy) {
        newSp.remove(self);
        target.setEnergy(target.getEnergy() + self.getUsedEnergy());
        return true;
      } else {
        return false;
      }
    }
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

/*
  other conditions?
  current energy
  energy system
  breeding system
  genetic system
*/

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Critter4" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
