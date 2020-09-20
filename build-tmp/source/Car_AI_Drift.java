import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Car_AI_Drift extends PApplet {

ArrayList<Car > cars=new ArrayList<Car>();
ArrayList<Wall> walls=new ArrayList<Wall>();
ArrayList<Wall> pointGates=new ArrayList<Wall>();

PImage carImage;
PImage raceTrack;

PVector camera;
PVector cameraHeading;

int [] layer={3, 5, 6};

int clones= 40;
int roundTime=15;
int generation=1;

boolean throttle=false;
boolean brake=false;
boolean left=false;
boolean right=false;
boolean makingWall=false;
boolean showBoard = false;

float prevMouseX=0;
float prevMouseY=0;
float zoom=1.2f;

JSONArray lines;
JSONArray pointLines;
JSONObject brain;

PFont font;

Population population;
NetBoard netboard;


public void setup() {
  
  netboard = new NetBoard();
  font=createFont("Calibri", 32);
  textAlign(CENTER);
  frameRate(40);

  camera=new PVector(width/2, height/2);
  

  carImage=loadImage("car.png");
  carImage.resize(45, 24);
  raceTrack=loadImage("raceTrack.png");
  raceTrack.resize(1366, 768);

  lines=new JSONArray();
  loadLine();

  for (int i=0; i<clones; i++) {
    cars.add(new Car());
  }

  population=new Population();

  loadBrain();
  //roundTime=100;
  cameraHeading=PVector.fromAngle(PI / 2 - cars.get(0).v.heading());
}

public void draw() {
  background(96, 160, 96);
  scale(zoom); 
  camera.add(PVector.sub(cars.get(0).p, camera).mult(0.06f));
  float rotationSpeed=(-cars.get(0).v.heading()-cameraHeading.heading());

  if (rotationSpeed>=PI)
    rotationSpeed=-PI*2+rotationSpeed;
  if (rotationSpeed<=-PI)
    rotationSpeed=PI*2+rotationSpeed;

  cameraHeading.rotate(rotationSpeed*0);
  if (!showBoard)
    translate(width / (2 * zoom), height / (2 * zoom));
  else
    translate(width * 3 / (4 * zoom), height/(2 * zoom));
  rotate(-PI/2+cameraHeading.heading());
  translate(-camera.x, -camera.y);
  image(raceTrack, 0, 0);
  stroke(255, 100);
  //line(cars.get(0).p.x, cars.get(0).p.y, cars.get(0).p.x+cars.get(0).heading.copy().setMag(200).x, cars.get(0).p.y+cars.get(0).heading.copy().setMag(200).y);
  line(cars.get(0).p.x, cars.get(0).p.y, cars.get(0).p.x+cars.get(0).v.copy().setMag(100).x, cars.get(0).p.y+cars.get(0).v.copy().setMag(100).y);

  population.update();

  // for (Wall pointGate : pointGates) {
  //   pointGate.show();
  // }

  //for (Wall wall : walls) {
  //  wall.show();
  //}

  translate(camera.x, camera.y);
  rotate(PI/2-cameraHeading.heading());
  if (!showBoard)
    translate(-width/(2*zoom), -height/(2*zoom));
  else if (showBoard) {
    translate(-width * 3/(4*zoom), -height/(2*zoom));
    netboard.visualizeNN();
  }
  fill(255, 200);
  text("Generation "+str(generation), width/2, 20);
}

public void mousePressed() {

  //pointGates.add(new Wall(prevMouseX, prevMouseY, mouseX, mouseY));
  //saveLine();
  ////makingWall=true;
  //prevMouseX=mouseX;
  //prevMouseY=mouseY;
  //stroke(0);
}

public void mouseReleased() {
  ////makingWall=false;
}

public void keyPressed() {
  //if ((key=='c')||(key=='C')) {
  //  if (pointGates.size()>0)
  //    pointGates.remove(0);
  //}
  //if ((key=='z')||(key=='Z')) {
  //  if (pointGates.size()>0)
  //    pointGates.remove(pointGates.size()-1);
  //}
  if ((key == 's')||(key == 'S'))
    showBoard = !showBoard;
  if (keyCode==LEFT)
    left=true;
  if (keyCode==RIGHT)
    right=true;
  if (keyCode==UP)
    throttle=true;
  if (keyCode==DOWN)
    brake=true;
}

public void keyReleased() {
  if (keyCode==LEFT)
    left=false;
  if (keyCode==RIGHT)
    right=false;
  if (keyCode==UP)
    throttle=false;
  if (keyCode==DOWN)
    brake=false;
}


//--------------------------------------------------------------------------------------

public void saveLine() {
  pointLines=new JSONArray();
  for (int i=0; i<pointGates.size(); i++) {
    Wall pointGate=pointGates.get(i);
    JSONObject line=new JSONObject();
    line.setFloat("x1", pointGate.x1);
    line.setFloat("y1", pointGate.y1);
    line.setFloat("x2", pointGate.x2);
    line.setFloat("y2", pointGate.y2);
    pointLines.setJSONObject(i, line);
  }

  saveJSONArray(pointLines, "data/pointLines.json");

  lines=new JSONArray();
  for (int i=0; i<walls.size(); i++) {
    Wall wall=walls.get(i);
    JSONObject line=new JSONObject();
    line.setFloat("x1", wall.x1);
    line.setFloat("y1", wall.y1);
    line.setFloat("x2", wall.x2);
    line.setFloat("y2", wall.y2);
    lines.setJSONObject(i, line);
  }

  saveJSONArray(lines, "data/lines.json");
}

public void loadLine() {
  pointLines=loadJSONArray("pointLines.json");
  for (int i=0; i<pointLines.size(); i++) {
    JSONObject line=pointLines.getJSONObject(i);
    float x1=line.getInt("x1");
    float y1=line.getInt("y1");
    float x2=line.getInt("x2");
    float y2=line.getInt("y2");
    Wall wall=new Wall(x1, y1, x2, y2);
    pointGates.add(wall);
  }

  lines=loadJSONArray("lines.json");
  for (int i=0; i<lines.size(); i++) {
    JSONObject line=lines.getJSONObject(i);
    float x1=line.getInt("x1");
    float y1=line.getInt("y1");
    float x2=line.getInt("x2");
    float y2=line.getInt("y2");
    Wall wall=new Wall(x1, y1, x2, y2);
    walls.add(wall);
  }
}
//---------------------------------------------------------------------------------------------------------------------------------
public void loadBrain() {
  brain=loadJSONObject("brain.json");
  generation=brain.getInt("generation");
  roundTime=brain.getInt("time");

  JSONArray brains=brain.getJSONArray("brains");

  int iterator=brains.size();
  if (clones<brains.size())
    iterator=clones;

  for (int i=0; i<iterator; i++) {

    JSONObject brain=brains.getJSONObject(i);

    JSONArray weights=brain.getJSONArray("weights");
    JSONArray biases=brain.getJSONArray("biases");

    for (int j=0; j<weights.size(); j++) {
      JSONObject weight=weights.getJSONObject(j);

      int weightRow=weight.getInt("row");
      int weightColumn=weight.getInt("column");
      JSONArray weightArrayRow=weight.getJSONArray("array");
      for (int k=0; k<weightArrayRow.size(); k++) {
        JSONArray weightArrayColumn=weightArrayRow.getJSONArray(k);
        float [] arrayColumn=weightArrayColumn.getFloatArray();
        cars.get(i).brain.weights.get(j).array[k]=arrayColumn;
      }
      cars.get(i).brain.weights.get(j).row=weightRow;
      cars.get(i).brain.weights.get(j).column=weightColumn;


      JSONObject bias=biases.getJSONObject(j);
      int biasRow=bias.getInt("row");
      int biasColumn=bias.getInt("column");
      JSONArray biasArrayRow=bias.getJSONArray("array");
      for (int k=0; k<biasArrayRow.size(); k++) {
        JSONArray biasArrayColumn=biasArrayRow.getJSONArray(k);
        float [] arrayColumn=biasArrayColumn.getFloatArray();
        cars.get(i).brain.biases.get(j).array[k]=arrayColumn;
      }
      cars.get(i).brain.biases.get(j).row=biasRow;
      cars.get(i).brain.biases.get(j).column=biasColumn;
    }
  }
}
//----------------------------------------------------------------------------------------
public void saveBrain() {
  int layer=cars.get(0).brain.weights.size();
  brain=new JSONObject();
  JSONArray brains=new JSONArray();

  for (int i=0; i<clones; i++) {

    JSONObject brain=new JSONObject();

    JSONArray weights=new JSONArray();
    JSONArray biases=new JSONArray();

    for (int j=0; j<layer; j++) {
      JSONObject weight=new JSONObject();

      int weightRow=cars.get(i).brain.weights.get(j).row; 
      int weightColumn=cars.get(i).brain.weights.get(j).column; 

      JSONArray weightArrayRow=new JSONArray();
      for (int k=0; k<weightRow; k++) {
        JSONArray weightArrayColumn=new JSONArray();
        float [] arrayColumn;
        arrayColumn=cars.get(i).brain.weights.get(j).array[k];
        for (int l=0; l<weightColumn; l++ ) {
          weightArrayColumn.setFloat(l, arrayColumn[l]);
        }
        weightArrayRow.setJSONArray(k, weightArrayColumn);
      }

      weight.setInt("row", weightRow); 
      weight.setInt("column", weightColumn); 
      weight.setJSONArray("array", weightArrayRow); 


      JSONObject bias=new JSONObject(); 

      int biasRow=cars.get(i).brain.biases.get(j).row; 
      int biasColumn=cars.get(i).brain.biases.get(j).column; 

      JSONArray biasArrayRow=new JSONArray(); 
      for (int k=0; k<biasRow; k++) {
        JSONArray biasArrayColumn=new JSONArray();
        float [] arrayColumn;
        arrayColumn=cars.get(i).brain.biases.get(j).array[k];
        for (int l=0; l<biasColumn; l++ ) {
          biasArrayColumn.setFloat(l, arrayColumn[l]);
        }
        biasArrayRow.setJSONArray(k, biasArrayColumn);
      }

      bias.setInt("row", biasRow); 
      bias.setInt("column", biasColumn); 
      bias.setJSONArray("array", biasArrayRow); 

      biases.setJSONObject(j, bias); 

      weights.setJSONObject(j, weight);
    }
    brain.setJSONArray("weights", weights); 
    brain.setJSONArray("biases", biases); 

    brains.setJSONObject(i, brain);
  }
  brain.setJSONArray("brains", brains); 

  brain.setInt("generation", generation);
  brain.setInt("time", roundTime);
  saveJSONObject(brain, "data/brain.json");
}
class Population {
  float mutationRate=0.02f;

  int time;
  int alive;

  PVector bestPos=new PVector(0, 0);

  Population() {
    time=millis();
    alive=cars.size();
  }

  public void update() {
    fill(255, 50);
    ellipse(bestPos.x, bestPos.y, 40, 40);

    alive=0;
    for (Car car : cars) {
      car.move();
      car.show();
      car.checkCrash();
      car.sense();
      car.think();
      //if (mousePressed)
      //  car.pick();
      if (!car.dead && car.checkPoint < pointGates.size() - 1)
        alive++;
    }

    //displayBestStat();


    if ((millis()>time+roundTime*1000)||(alive<=0 && millis()>time+2000 && millis() > 10000)) {
      time=millis();
      alive=cars.size();
      regenerate();
      generation++;
    }
  }

  public void displayBestStat() {
    text("S "+str(generation), cars.get(0).p.x+width/3-50, cars.get(0).p.y+height/3-100);
    text("Generation "+str(generation), cars.get(0).p.x+width/3-50, cars.get(0).p.y+height/3-80);
  }

  public void regenerate() {
    Car best=new Car();
    ArrayList<Car> parentPool=new ArrayList<Car>();
    float highest=0.01f;
    float sum = 0;
    for (Car car : cars) {
      float fitness=car.getFitness();
      sum += fitness;
      if (fitness>highest) {
        highest=fitness;
        best=car.copy();
        bestPos=car.p.copy();
      }
    }

    if (highest>60)
      roundTime=PApplet.parseInt(sqrt(highest)/10);
    else
      roundTime=6;
    if (roundTime>180)
      roundTime=180;
    println(sqrt(highest));

    for (Car car : cars) {
      float chance=sqrt(car.getFitness())*20/sqrt(highest);
      for (int i=1; i<chance; i++) {
        parentPool.add(car.copy());
      }
    }
    //for (int i = 0; i < clones; i++) {
    //  parentPool.add(pickOne(cars, sum));
    //  fill(255, 100);
    //  ellipse(parentPool.get(i).p.x, parentPool.get(i).p.y, 100, 100);
    //  //println(i,cars.indexOf(parentPool.get(i)),parentPool.get(i).getFitness());
    //}
    println("parentpool:",parentPool.size());

    Collections.shuffle(parentPool);

    if (parentPool.size()>0) {
    for (int i = 0; i < cars.size(); i++) {
      // if(i < cars.size() * 3 / 4){
        int indexA=floor(random(parentPool.size()));
        cars.get(i).brain=parentPool.get(indexA).brain.copy();
        // }
      // else{
      //   int indexA = floor(random(parentPool.size()));
      //   int indexB = floor(random(parentPool.size()));
      //   cars.get(i).brain = parentPool.get(indexA).brain.crossover(parentPool.get(indexB).brain);
      // }
      }
    }

    Collections.shuffle(cars);

    for (int i = 0; i < clones * 7 / 8; i++) {
      cars.get(i).brain.mutate(mutationRate);
      cars.get(i).reset();
    } 
    
    for (int i = clones * 7 / 8; i < clones; i++) {
      cars.get(i).brain.mutate(0.1f);
      cars.get(i).reset();
    }
    best.reset();
    best.think();
    cars.set(0, best.copy());
    saveBrain();
  }
}

public float acceptReject(float fitness, float maxFitness) {
  float r=random(maxFitness);
  if (r<=fitness)
    return fitness;
  else
    return 0;
}

public Car pickOne(ArrayList<Car> candidate, float sum) {
  int index= 0;
  float r = random(1);

  while (r > 0) {
    float prob = candidate.get(index).getFitness() / sum;
    r = r - prob;
    index++;
  }
  index--;
  return candidate.get(index).copy();
}
class Car {
  NeuralNetwork brain;

  PVector p;
  PVector v;
  PVector heading;
  PVector lastPoint;

  float [] sensor=new float[8];

  float sensorDistance=180;
  float wheelBase=15;
  float steerAngle=0;
  float maxSteer=25;
  float maxSpeed=10;
  float traveled;
  int point=0;
  int checkPoint;

  boolean dead=false;
  Car() {
    brain=new  NeuralNetwork(layer);
    reset();
  }
  //----------------------------------------------------------------------------------------------------
  public void reset() {
    p=new PVector(width / 2 - 210, height / 2 + 46);
    v=new PVector(0, 0);
    heading=new PVector(-1, 0);
    v.rotate(heading.heading());
    lastPoint=p.copy();
    point=0;
    dead=false;
    checkPoint = pointGates.size() - 1;
    traveled=0;
  }

  public Car copy() {
    Car clone=new Car();
    clone.p = p.copy();
    clone.brain=this.brain.copy();
    return clone;
  }
  //---------------------------------------------------------------------------------------
  public void move() {
    if (!dead) {
      float rotationSpeed = (heading.heading() - v.heading());
      if (rotationSpeed>=PI)
        rotationSpeed=-PI*2+rotationSpeed;
      if (rotationSpeed<=-PI)
        rotationSpeed=PI*2+rotationSpeed;
      v.rotate(rotationSpeed/40);
      p.add(v);
      traveled+=v.mag();
      v.div(1.005f);
    }
  }

  public void show() {
    pushMatrix();
    translate(p.x, p.y);
    rotate(heading.heading());
    stroke(255);    
    image(carImage, -32, -12);
    //line(0, -100, 0, 100);
    //line(-100, 0, 100, 0);
    //rectMode(CENTER);
    //fill(255, 100);
    //rect(-10, 0, 45, 20);
    popMatrix();
  }
  //----------------------------------------------------------------------------------
  public void throttle() {
    PVector direction=new PVector(0.12f, 0);
    direction.rotate(heading.heading());
    v.add(direction);
    //v.limit(maxSpeed);
  }

  public void brake() {
    v.sub(v.copy().mult(0.1f).limit(0.05f));
  }
  //----------------------------------------------------------------------------------
  public void checkCrash() {
    PVector offset=heading.copy().rotate(PI).setMag(10);
    PVector center=new PVector(p.x, p.y).add(offset);
    PVector w=heading.copy().setMag(45/2);
    PVector h=heading.copy().rotate(PI/2).setMag(20/2);
    PVector topLeft=PVector.sub(center, w).sub(h);
    PVector topRight=PVector.add(center, w).sub(h);
    PVector bottomRight=PVector.add(center, w).add(h);
    PVector bottomLeft=PVector.sub(center, w).add(h);

    for (Wall wall : walls) {
      if (linesTouching(topLeft.x, topLeft.y, topRight.x, topRight.y, wall.x1, wall.y1, wall.x2, wall.y2))
        dead=true;
      if (linesTouching(topRight.x, topRight.y, bottomRight.x, bottomRight.y, wall.x1, wall.y1, wall.x2, wall.y2))
        dead=true;
      if (linesTouching(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y, wall.x1, wall.y1, wall.x2, wall.y2))
        dead=true;
      if (linesTouching(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y, wall.x1, wall.y1, wall.x2, wall.y2))
        dead=true;
    }
    Wall wall=pointGates.get(checkPoint);
    if ((linesTouching(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y, wall.x1, wall.y1, wall.x2, wall.y2))||(linesTouching(topLeft.x, topLeft.y, topRight.x, topRight.y, wall.x1, wall.y1, wall.x2, wall.y2))) {
      point+=20;
      lastPoint=p.copy();
      checkPoint--;
      if (checkPoint < 0) {
        checkPoint=pointGates.size() - 1;
        point+=200;
      }
    }
  }

  public void sense() {
    PVector offset=heading.copy().rotate(PI).setMag(10);
    PVector center=new PVector(p.x, p.y).add(offset);

    for (int i=0; i<sensor.length; i++) {
      sensor[i]=sensorDistance;
      PVector sensorLine=new PVector(sensorDistance, 0).rotate(heading.heading()+(PI/4)*i); 
      PVector sensorPoint=PVector.add(center, sensorLine);

      for (Wall wall : walls) {
        if (linesTouching(center.x, center.y, sensorPoint.x, sensorPoint.y, wall.x1, wall.y1, wall.x2, wall.y2)) {
          PVector intersectPoint=lineIntersection(center.x, center.y, sensorPoint.x, sensorPoint.y, wall.x1, wall.y1, wall.x2, wall.y2);
          float intersectionDistance=PVector.sub(center, intersectPoint).mag();
          if (intersectionDistance<sensor[i]) {
            sensor[i]=intersectionDistance;
          }
        }
      }
    }

    for (int i: new int[]{1, 7}) {
      PVector sensorLine=new PVector(sensor[i], 0).rotate(heading.heading()+(PI/4)*i); 
      PVector sensorPoint=PVector.add(center, sensorLine);
      //line(center.x, center.y, sensorPoint.x, sensorPoint.y);
      stroke(255);
      strokeWeight(1);
      float len = 8;
      line(sensorPoint.x-len, sensorPoint.y, sensorPoint.x+len, sensorPoint.y);
      line(sensorPoint.x, sensorPoint.y-len, sensorPoint.x, sensorPoint.y+len);
    }
  }
  //----------------------------------------------------------------------------------------------------------------------
  public void think() {
    if (!dead) {
      float [] input=new float[layer[0]];
      // for (int i=0; i<sensor.length; i++) {
      //   input[i]=sensor[i]/sensorDistance;
      // }

      input[0] = sensor[1]/sensorDistance;
      input[1] = sensor[7]/sensorDistance;
      input[2] = v.mag()/5;

      float [] output=brain.feedForward(input);

      if (output [0]>=output[1]&&(output[0]>output[2]))
        throttle();
      else if (output[2]>=output[1]&&(output[2]>=output[0]))
        brake();

      if ((output[3]>output[4])&&(output[3]>output[5]))
        heading.rotate(radians(-2.5f));
      else if ((output[4]>output[3])&&(output[4]>output[5]))
        heading.rotate(radians(2.5f));
    }
  }
  //----------------------------------------------------------------------------------------------------------------
  public float getFitness() {
    float lastPointDistance=PVector.sub(p, lastPoint).mag();
    float score = point;
    //  + (millis() - population.time)/1000;
    if (checkPoint != pointGates.size() - 1)
      score += lastPointDistance / 40;
    return score * score;
  }

  public void pick() {
    float distance=PVector.sub(p, new PVector(mouseX, mouseY)).mag();
    if (distance<50)
      point*=1000;
  }
}

//-----------------------------------------------------------------------------------------------------------------------------
public boolean linesTouching(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
  float denominator = ((x2 - x1) * (y4 - y3)) - ((y2 - y1) * (x4 - x3));
  float numerator1 = ((y1 - y3) * (x4 - x3)) - ((x1 - x3) * (y4 - y3));
  float numerator2 = ((y1 - y3) * (x2 - x1)) - ((x1 - x3) * (y2 - y1));

  // Detect coincident lines (has a problem, read below)
  if (denominator == 0) return numerator1 == 0 && numerator2 == 0;

  float r = numerator1 / denominator;
  float s = numerator2 / denominator;

  return (r >= 0 && r <= 1) && (s >= 0 && s <= 1);
}

public PVector lineIntersection(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

  // calculate the distance to intersection point
  float uA = ((x4-x3)*(y1-y3) - (y4-y3)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
  float uB = ((x2-x1)*(y1-y3) - (y2-y1)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));

  // if uA and uB are between 0-1, lines are colliding
  if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {
    return new PVector(x1 + (uA * (x2-x1)), y1 + (uA * (y2-y1)));
  }
  return null;
}
class Matrix {
  float [][] array;
  int row;
  int column;

  Matrix(int row, int column) {
    array=new float[row][column];
    this.row=row;
    this.column=column;
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=random(-1, 1);
      }
    }
  }

  Matrix(Matrix other) {
    row=other.row;
    column=other.column;
    array=new float[row][column];
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=other.array[i][j];
      }
    }
  }

  Matrix(float [] input) {
    row=1;
    column=input.length;
    array=new float[row][column];
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=input[j];
      }
    }
  }

  public float get(int i, int j) {
    return array[i][j];
  }

  public float [][] getArray() {
    return array;
  }

  public void set(float n) {
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=n;
      }
    }
  }

  public Matrix copy() {
    Matrix result=new Matrix(row, column);
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        result.array[i][j]=array[i][j];
      }
    }
    return result;
  }

  public void printMatrix() {
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        print(array[i][j]+" ");
      }
      println();
    }
    println();
  }

  public void T() {
    Matrix result=new Matrix(column, row);
    for (int i=0; i<column; i++) {
      for (int j=0; j<row; j++) {
        result.array[i][j]=array[j][i];
      }
    }
    row=result.row;
    column=result.column;
    array=result.array;
  }

  public void add(float n) {
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=array[i][j]+n;
      }
    }
  } 

  public void mult(float n) {
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        array[i][j]=array[i][j]*n;
      }
    }
  }

  public void mutate(float mutationRate) {
    for (int i=0; i<row; i++) {
      for (int j=0; j<column; j++) {
        float random=random(1);
        if (random<=mutationRate) {
          float x=array[i][j]+=random(-7, 7);
          array[i][j]=1/(1+exp(-1*x));
        }
      }
    }
  }
}

class MatrixMath {
  public Matrix mult(Matrix a, Matrix b) {
    Matrix result=new Matrix(a.row, b.column);

    if (a.column==b.row) {
      for (int i=0; i<a.row; i++) {
        for (int j=0; j<b.column; j++) {
          result.array[i][j]=0;
          for (int k=0; k<b.row; k++) {
            result.array[i][j]+=a.array[i][k]*b.array[k][j];
          }
        }
      }
    } else {
      println("=========================================");
      println("this matrix column doesnt match other row");
      println("=========================================");
    }

    return result;
  }

  public Matrix add(Matrix a, Matrix b) {
    Matrix result=new Matrix(a.row, a.column); //not done error mismatch row cathcer
    if ((a.row==b.row)&&(a.column==b.column)) {
      for (int i=0; i<result.row; i++) {
        for (int j=0; j<result.column; j++) {
          result.array[i][j]=a.array[i][j]+b.array[i][j];
        }
      }
    } else {
      println("=========================================");
      println("this matrix column/row doesnt match other column/row");
      println("=========================================");
    }
    return result;
  }

  public Matrix sub(Matrix a, Matrix b) {
    Matrix result=new Matrix(a.row, a.column);
    if ((a.row==b.row)&&(a.column==b.column)) {
      for (int i=0; i<result.row; i++) {
        for (int j=0; j<result.column; j++) {
          result.array[i][j]=a.array[i][j]-b.array[i][j];
        }
      }
    } else {
      println("=========================================");
      println("this matrix column/row doesnt match other column/row");
      println("=========================================");
    }
    return result;
  }

  public Matrix getT(Matrix a) {
    Matrix result=new Matrix(a.column, a.row);
    for (int i=0; i<result.row; i++) {
      for (int j=0; j<result.column; j++) {
        result.array[i][j]=a.array[j][i];
      }
    }
    return result;
  }

  public Matrix hadamartProduct(Matrix a, Matrix b) {
    Matrix result=new Matrix(a.row, a.column); //not done error mismatch row cathcer
    if ((a.row==b.row)&&(a.column==b.column)) {
      for (int i=0; i<result.row; i++) {
        for (int j=0; j<result.column; j++) {
          result.array[i][j]=a.array[i][j]*b.array[i][j];
        }
      }
    } else {
      println("=========================================");
      println("this matrix column/row doesnt match other column/row");
      println("=========================================");
    }
    return result;
  }

  public Matrix sigmoid(Matrix a) {
    Matrix result=new Matrix(a);
    for (int i=0; i<a.row; i++) {
      for (int j=0; j<a.column; j++) {
        float x=result.array[i][j];
        result.array[i][j]=1/(1+exp(-1*x));
      }
    }
    return result;
  }
}

MatrixMath Matrix=new MatrixMath();
class NeuralNetwork {
  ArrayList<Matrix> weights=new ArrayList<Matrix>();
  ArrayList<Matrix> biases=new ArrayList<Matrix>();
  ArrayList<Matrix> perceptrons=new ArrayList<Matrix>();
  NeuralNetwork(int [] layers) {
    for (int i=0; i<layers.length-1; i++) {
      int row=layers[i+1];
      int column=layers[i];
      weights.add(new Matrix(row, column));

      row=layers[i+1];
      column=1;
      biases.add(new Matrix(row, column));
    }
  }

  public float [] feedForward(float [] input) {
    perceptrons.clear();
    Matrix output=new Matrix(input);
    output.T();
    perceptrons.add(output.copy());
    for (int i=0; i<weights.size(); i++) {
      output=Matrix.mult(weights.get(i), output);

      output=Matrix.add(output, biases.get(i));

      output=Matrix.sigmoid(output);
      perceptrons.add(output.copy());
    }
    output.T();
    return output.array[0];
  }

  public void mutate(float mutationRate) {
    for (Matrix weight : weights)
      weight.mutate(mutationRate);
    for (Matrix bias : biases)
      bias.mutate(mutationRate);
  }

  public NeuralNetwork crossover(NeuralNetwork other) {
    NeuralNetwork child=this.copy();
    child.weights.clear();
    child.biases.clear();
    // int mid=round(random(weights.size()));
    for (int i=0; i<weights.size(); i++) {
      if (round(random(1)) == 0) {
        child.weights.add(weights.get(i).copy());
      } else {
        child.weights.add(other.weights.get(i).copy());
      }

      if (round(random(1)) == 0) {
        child.biases.add(biases.get(i).copy());
      } else {
        child.biases.add(other.biases.get(i).copy());
      }
    }
    return child;
  }

  public NeuralNetwork copy() {
    int [] parameter={0};
    NeuralNetwork clone=new NeuralNetwork(parameter);
    clone.weights.clear();
    clone.biases.clear();
    for (int i=0; i<weights.size(); i++) {
      clone.weights.add(weights.get(i).copy());
      clone.biases.add(biases.get(i).copy());
    }
    return clone;
  }

  public void train(float [] inputArray, float [] targetArray) {
    float learningRate=0.5f;
    ArrayList<Matrix> neurons=new ArrayList<Matrix>();
    ArrayList<Matrix> errors=new ArrayList<Matrix>();

    Matrix target=new Matrix(targetArray);
    target.T();
    Matrix output=new Matrix(inputArray);
    output.T();
    neurons.add(output.copy());

    output.printMatrix();
    for (int i=0; i<weights.size(); i++) {
      println("WEIGHT :");
      weights.get(i).printMatrix();

      println("WEIGHT X INPUT :");
      output=Matrix.mult(weights.get(i), output);
      output.printMatrix();

      println("BIAS :");
      output=Matrix.add(output, biases.get(i));
      biases.get(i).printMatrix();

      println("INPUT + BIAS :");
      output.printMatrix();

      println("SIGMOID :");
      output=Matrix.sigmoid(output);
      output.printMatrix();
      println();
      println();
      neurons.add(output.copy());
    }
    errors.add(Matrix.sub(target, output));
    println("ERROR :");
    errors.get(0).printMatrix();

    for (int i=weights.size()-1; i>0; i--) {
      Matrix transposedWeight=Matrix.getT(weights.get(i));
      println("TRANSPOSED WEIGHT :");
      transposedWeight.printMatrix();
      for (int j=0; j<transposedWeight.row; j++) {
        float sumOfRow=0;
        for (int k=0; k<transposedWeight.column; k++) {
          sumOfRow+=transposedWeight.array[j][k];
        }
        println("SUM OF ROW "+ str(j)+" :");
        println(sumOfRow);
        for (int k=0; k<transposedWeight.column; k++) {
          if (sumOfRow!=0)
            transposedWeight.array[j][k]*=(1/sumOfRow);
        }
        println("TRANSPOSED WEIGHT ROW "+ str(j)+" / "+"SUM OF ROW :");
        printArray(transposedWeight.array[j]);
      }

      Matrix error=Matrix.mult(transposedWeight, errors.get(0));
      println("ERROR LAYER "+str(i)+" :");
      error.printMatrix();
      errors.add(0, error);
    }

    for (int i=weights.size()-1; i>=0; i--) {

      Matrix derivatedSigmoid=neurons.get(i+1).copy();
      Matrix inverseMatrix=derivatedSigmoid.copy();
      inverseMatrix.set(1);
      inverseMatrix=Matrix.sub(inverseMatrix, derivatedSigmoid);
      derivatedSigmoid=Matrix.hadamartProduct(derivatedSigmoid, inverseMatrix);
      Matrix gradient=Matrix.hadamartProduct(errors.get(i), derivatedSigmoid);

      Matrix slope=Matrix.mult(gradient, Matrix.getT(neurons.get(i)));
      slope.mult(learningRate);

      Matrix weight=weights.get(i).copy();
      weights.remove(i);
      weights.add(i, Matrix.add(weight, slope));

      Matrix bias=biases.get(i).copy();
      biases.remove(i);
      biases.add(i, Matrix.add(bias, gradient));
      gradient.printMatrix();

      slope.printMatrix();
    }
  }
}

float scroll = 0;
class NetBoard {
  PGraphics board;
  PGraphics decision;
  int margin = PApplet.parseInt(32 / zoom);
  float boardX       = 0;
  int boardSize      = PApplet.parseInt((width / 4 + margin) / zoom );
  float inputX       = margin / 2;
  float outputX      = boardSize - margin /2;
  float size         = outputX - inputX;
  float resolution   = size / (layer.length - 1);
  float scale        = 40 / zoom;
  NetBoard() {
    board = createGraphics(boardSize, height);
    decision = createGraphics(100, height);
    decision.beginDraw();
    decision.textFont(createFont("Calibri", 34));
    decision.endDraw();
  }
  public void visualizeNN() {
    image(board, boardX, 0);
    image(decision, boardX + size + margin, 0);
    board.beginDraw();
    board.background(255);
    NeuralNetwork nn = cars.get(0).brain;
    for ( int i = 0; i < nn.perceptrons.size(); i++) {
      for ( int j = 0; j < layer[i]; j++) {
        float x      = inputX + resolution * i;
        float y      = 50 + 40 * (j * 2  + 1 - layer[i]) / 2 + scroll;
        float value  = nn.perceptrons.get(i).get(j, 0) ;
        if (i < nn.weights.size()) {
          for (int k = 0; k < layer[i + 1]; k++) {
            float xo = inputX + resolution * (i + 1);
            float yo = 50 + 40 * (k * 2  + 1 - layer[i + 1]) / 2 + scroll;
            float w  = nn.weights.get(i).get(k, j);
            if (w>0)
              board.stroke(0, 255, 0, 128 * abs(w));
            else
              board.stroke(255, 0, 0, 128 * abs(w));
            board.line(x, y, xo, yo);
          }
        }
        board.stroke(0);
        board.fill(100 + 155 * value);
        board.ellipse(x, y, 30, 30);
        board.textAlign(CENTER);
        if (value > 0.5f)
          board.fill(0);
        else
          board.fill(255);
        board.text(nf(value, 0, 2), x + 1, y + 5);
      }
    }
    board.endDraw();
    if (nn.perceptrons.size() > 0) {
      Matrix output = nn.perceptrons.get(nn.perceptrons.size() - 1);
      decision.beginDraw();
      decision.background(255);
      decision.textAlign(CORNER, CENTER);
      float x              = 0;
      float y              = 50 + 40 * (-0.5f  + 1 - layer[layer.length - 1]) / 2 + scroll;
      float valueActive    = output.get(0, 0) ;
      float valueDeactive  = output.get(1, 0) ;
      fill(0, 255);
      /*
      if (output [0]>=output[1]&&(output[0]>output[2]))
        throttle();
      else if (output[2]>=output[1]&&(output[2]>=output[0]))
        brake();

      if ((output[3]>output[4])&&(output[3]>output[5]))
        heading.rotate(radians(-2.5));
      else if ((output[4]>output[3])&&(output[4]>output[5]))
        heading.rotate(radians(2.5));
      */
      if (output.get(0, 0)>=output.get(1, 0) && output.get(0, 0)>output.get(2, 0))
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("GAS", x + 1, y + 5);


      y = 50 + 40 * (3.5f  + 1 - layer[layer.length - 1]) / 2 + scroll;
      if (output.get(2, 0)>=output.get(1, 0) && output.get(2, 0)>=output.get(0, 0))
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("REM", x + 1, y + 5);


      y = 50 + 40 * (5.5f  + 1 - layer[layer.length - 1]) / 2 + scroll;
      if (output.get(3, 0)>output.get(4, 0) && output.get(3, 0)>output.get(5, 0))
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("KIRI", x + 1, y + 5);

      y = 50 + 40 * (7.5f  + 1 - layer[layer.length - 1]) / 2 + scroll;
      if (output.get(4, 0)>output.get(3, 0) && output.get(4, 0)>output.get(5, 0))
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("KANAN", x + 1, y + 5);
      decision.endDraw();
    }
    if (mousePressed) {
      scroll += mouseY- pmouseY;
    }
  }
}
class Wall {
  float x1;
  float y1;
  float x2;
  float y2;
  Wall(float x1, float y1, float x2, float y2 ) {
    this.x1=x1;
    this.y1=y1;
    this.x2=x2;
    this.y2=y2;
  }

  public void show() {
    strokeWeight(2);
    stroke(0);
    line(x1, y1, x2, y2);
  }
}
  public void settings() {  size(displayWidth,displayHeight,P2D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Car_AI_Drift" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
