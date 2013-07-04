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

float vScale = 0.1f; //velocity scale

float initialE = 2f;
public final int inputN = 11;

public void setup() {
  size(1024, 768, OPENGL);
  noFill();
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
    newSp = new ArrayList(sp);
    background(255);
    float totalEnergy = 0;
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
        if (spinner.getId() != a.getId() && ((same && (spinner.getType() == a.getType())) || (!same && (spinner.getType() != a.getType())))) {
            float newdist = getDistance(spinner.getPosition(), a.getPosition());
            if (min == null || newdist < min) {
                min = newdist;
                closest = spinner;
            }
        }
    }
    return closest;
}

public float getDistance(float[] a, float[] b) {
  return sqrt(sq(a[0] - b[0]) + sq(a[1] - b[1]));    
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

    public long getAge() {
        return System.currentTimeMillis() - this.birthday;
    }

    public float[] getPosition() {
        return position;
    }

    public void setPosition(float[] position) {
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
      setNetInput();
      net.update();
      float rdm = random(1f);

      Spinner closestSame = getClosestOfSameType(this, true);
      Spinner closestDifferent = getClosestOfSameType(this, true);
      if (closestSame == null || closestDifferent == null) return;
      float distanceSame = getDistance(closestSame.getPosition(), this.getPosition());
      float distanceDifferent = getDistance(closestDifferent.getPosition(), this.getPosition());
      float[] netOutput = net.getVOutput();
      float[] rawVelocityOutput = new float[2];
      rawVelocityOutput[0] = vScale * (netOutput[0] - netOutput[1]);
      rawVelocityOutput[1] = vScale * (netOutput[2] - netOutput[3]);
      velocity = averager(rawVelocityOutput, velocity, 0.4f);
      if (interactDistance(distanceSame, getRadius())) {
          if (agents < maxAgents) {
            if (mate(this, closestSame)) {
                agents++;
                maxId++;
            }
          }
      }
      if (interactDistance(distanceDifferent, getRadius())) {
          if (attack(this, closestDifferent)) {
              agents--;
          }
      }
      position[0] = pm(position[0] + velocity[0], (float) width);
      position[1] = pm(position[1] + velocity[1], (float) height);
  }
  
  public void setNetInput() {
    float[] input = new float[inputN];
    Spinner closestSame = getClosestOfSameType(this, true);
    Spinner closestDifferent = getClosestOfSameType(this, false);
    float[] distanceVectorSame = getDistanceVector(closestSame.getPosition(), this.getPosition());
    float[] distanceVectorDifferent = getDistanceVector(closestDifferent.getPosition(), this.getPosition());
    float[] velocity = getVelocity();
    
    input[0] = distanceVectorSame[0];
    input[1] = distanceVectorSame[1];
    input[2] = distanceVectorDifferent[0];
    input[3] = distanceVectorDifferent[1];
    input[4] = velocity[0];
    input[5] = velocity[1];
    input[6] = closestSame.getRadius();
    input[6] = closestDifferent.getRadius();
    input[7] = activation(this.getAge(), 1f, 1E-5f, 3f);
    input[8] = activation(closestSame.getAge(), 1f, 1E-5f, 3f);
    input[9] = activation(closestDifferent.getAge(), 1f, 1E-5f, 3f);
    input[10] = this.getEnergy();

    net.setInputs(input);
  }

    public float activation(float x, float scaleRange, float scaleDomain, float shift) {
        return scaleRange * ((float) Math.tanh((x + shift) * scaleDomain) + 1f);
    }

    //returns true if a new agent was created
    public boolean mate(Spinner self, Spinner target) throws Exception {
        final float childEnergy = 1f;
        final float minEnergy = 0.1f;
        float minE = Math.min(self.getEnergy(), target.getEnergy());
        float maxE = Math.max(self.getEnergy(), target.getEnergy());

        if (!newSp.contains(target) || !newSp.contains(self) || minE < minEnergy) {
            return false;    //crappy way of handling concurrency issues
        }

        float maxContribution = (maxE / (minE + maxE)) * childEnergy;
        float minContribution = (minE / (minE + maxE)) * childEnergy;

        int newId = maxId;
        if (minE + maxE >= childEnergy) {
            Network newNet = new Network(self.getNetwork(), target.getNetwork(), 0.05f);
            Spinner newSpinner = new Spinner(newId, self.getType(), childEnergy, newNet);
            if (self.getEnergy() > target.getEnergy()) {
                self.setEnergy(self.getEnergy() - maxContribution);
                target.setEnergy(target.getEnergy() - minContribution);
            } else {
                self.setEnergy(self.getEnergy() - minContribution);
                target.setEnergy(target.getEnergy() - maxContribution);
            }
            double angle = random(2f * (float)Math.PI);
            float[] newPosition = new float[2];
            newPosition[0] = self.getPosition()[0] + self.getRadius() * (float)Math.cos(angle);
            newPosition[1] = self.getPosition()[1] + self.getRadius() * (float)Math.sin(angle);
            newSpinner.setPosition(newPosition);
            newSp.add(newSpinner);
            return true;
            //print("+");
        }
        return false;
      }

  //returns true if someone died
  public boolean attack(Spinner self, Spinner target) {
    if (!newSp.contains(target) || !newSp.contains(self)) {
        return false;    //crappy way of handling concurrency issues
    }

    float rdm = random(1f);
    float ratio = self.getEnergy() / (target.getEnergy() + self.getEnergy());
    if (rdm < ratio) {
      self.setEnergy(self.getEnergy() + target.getEnergy());
      newSp.remove(target);
      //print("<");
    } else {
      target.setEnergy(target.getEnergy() + self.getEnergy());
      newSp.remove(self);
      //print(">");
    }
    return true;
  }

  public float getRadius() {
    return sqrt(100*energy)/2f;
  }
  
  public void draw() {
    
    if (this.getType() == 0) {
      stroke(100, 100, 200);
    } else {
      stroke(200, 100, 100);  
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
