ArrayList<Car > cars=new ArrayList<Car>();
ArrayList<Wall> walls=new ArrayList<Wall>();
ArrayList<Wall> pointGates=new ArrayList<Wall>();

PImage carImage;
PImage raceTrack;

PVector camera;
PVector cameraHeading;

int [] layer={4, 6, 6};

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
float zoom=1.2;

JSONArray lines;
JSONArray pointLines;
JSONObject brain;

PFont font;

Population population;
NetBoard netboard;
import java.util.*;

void setup() {
  size(displayWidth,displayHeight,P2D);
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

  // loadBrain();
  //roundTime=100;
  cameraHeading=PVector.fromAngle(PI / 2 - cars.get(0).v.heading());
}

void draw() {
  background(96, 160, 96);
  scale(zoom); 
  camera.add(PVector.sub(cars.get(0).p, camera).mult(0.06));
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

void mousePressed() {

  //pointGates.add(new Wall(prevMouseX, prevMouseY, mouseX, mouseY));
  //saveLine();
  ////makingWall=true;
  //prevMouseX=mouseX;
  //prevMouseY=mouseY;
  //stroke(0);
}

void mouseReleased() {
  ////makingWall=false;
}

void keyPressed() {
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

void keyReleased() {
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

void saveLine() {
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

void loadLine() {
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
void loadBrain() {
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
void saveBrain() {
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
