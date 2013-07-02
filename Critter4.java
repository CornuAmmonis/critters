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
int agents = 500;

float vScale = 0.2f; //velocity scale

public void setup() {
  size(1024, 768, OPENGL);
  noFill();
  sp = new ArrayList<Spinner>();
  for (int i = 0; i < agents; i++) {
    if (i < agents/2) {
      sp.add(i, new Spinner(i,0,1f));
    } else {
      sp.add(i, new Spinner(i,1,1f));
    }  
  }
}

public void draw() {
    newSp = new ArrayList(sp);
    background(255);
    for (Spinner spinner : sp) {
        try{
            spinner.update();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        spinner.draw();
    }
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

    }
  
    public int getId() {
        return id;
    }

    public int getType() {
        return type;
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

      Spinner closest = getClosest(this);
      if (closest == null) return;
      float distance = getDistance(closest.getPosition(), this.getPosition());
      float[] netOutput = net.getVOutput();
      float[] rawVelocityOutput = new float[2];
      rawVelocityOutput[0] = vScale * (netOutput[0] - netOutput[1]);
      rawVelocityOutput[1] = vScale * (netOutput[2] - netOutput[3]);
      velocity = averager(rawVelocityOutput, velocity, 0.4f);
      if (interactDistance(distance, getRadius())) {
          if (closest.getType() == this.getType()) {
              mate(this, closest);
          } else {
              attack(this, closest);
          }
      }
      position[0] = pm(position[0] + velocity[0], (float) width);
      position[1] = pm(position[1] + velocity[1], (float) height);
  }
  
  public void setNetInput() {
    float[] input = new float[5];
    Spinner closest = getClosest(this);
    float[] distanceVector = getDistanceVector(closest.getPosition(), this.getPosition());
    float[] velocity = getVelocity();
    int type = closest.getType();
    
    input[0] = distanceVector[0];
    input[1] = distanceVector[1];
    input[2] = velocity[0];
    input[3] = velocity[1];
    input[4] = type + 0f;

    net.setInputs(input);
  }
  
    public void mate(Spinner self, Spinner target) throws Exception {
        float contribution = self.getEnergy() / (self.getEnergy() + target.getEnergy());
        int newId = newSp.size();
        if (self.getEnergy() > contribution && target.getEnergy() > contribution) {
            Network newNet = new Network(self.getNetwork(), target.getNetwork(), 0.05f);
            Spinner newSpinner = new Spinner(newId, self.getType(), 1f);
            self.setEnergy(self.getEnergy() - contribution);
            target.setEnergy(target.getEnergy() + contribution - 1);
            newSpinner.setPosition(self.getPosition());
            newSp.add(newSpinner);
            //print("+");
        }
      }
  
  public void attack(Spinner self, Spinner target) {
    if (!newSp.contains(target)) {
        return;    //crappy way of handling concurrency issues
    }

    float rdm = random(1f);
    float ratio = self.getEnergy() / (target.getEnergy() + self.getEnergy());
    if (rdm < ratio) {
      self.setEnergy(self.getEnergy() + target.getEnergy());
      newSp.remove(target);
      print("<");
    } else {
      target.setEnergy(target.getEnergy() + self.getEnergy());
      newSp.remove(self);
      print(">");
    }   
  }

  public float getRadius() {
    return sqrt(100*energy);
  }
  
  public void draw() {
    
    if (this.getType() == 0) {
      stroke(100, 100, 200);
    } else {
      stroke(200, 100, 100);  
    }
    strokeWeight(2);
    
    ellipse(position[0], position[1], getRadius(), getRadius());
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
