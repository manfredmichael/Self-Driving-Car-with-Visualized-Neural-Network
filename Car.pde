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
  float maxSpeed=5;
  float traveled;
  int point=0;
  int checkPoint;

  boolean dead=false;
  Car() {
    brain=new  NeuralNetwork(layer);
    reset();
  }
  //----------------------------------------------------------------------------------------------------
  void reset() {
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

  Car copy() {
    Car clone=new Car();
    clone.p = p.copy();
    clone.brain=this.brain.copy();
    return clone;
  }
  //---------------------------------------------------------------------------------------
  void move() {
    if (!dead) {
      float rotationSpeed = (heading.heading() - v.heading());
      if (rotationSpeed>=PI)
        rotationSpeed=-PI*2+rotationSpeed;
      if (rotationSpeed<=-PI)
        rotationSpeed=PI*2+rotationSpeed;
      v.rotate(rotationSpeed/40);

      v.limit(maxSpeed);
      p.add(v);
      traveled+=v.mag();
      v.div(1.005);
    }
  }

  void show() {
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

  void showAsBest() {
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
  void throttle() {
    PVector direction=new PVector(0.12, 0);
    direction.rotate(heading.heading());
    v.add(direction);
  }

  void brake() {
    v.sub(v.copy().mult(0.1).limit(0.05));
  }
  //----------------------------------------------------------------------------------
  void checkCrash() {
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

  void sense() {
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
  }

  void showSensor(){
    PVector offset=heading.copy().rotate(PI).setMag(10);
    PVector center=new PVector(p.x, p.y).add(offset);
    
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
  void think() {
    if (!dead) {
      float [] input=new float[layer[0]];
      // for (int i=0; i<sensor.length; i++) {
      //   input[i]=sensor[i]/sensorDistance;
      // }
      // input[input.length-1] = v.mag()/5;

      input[0] = degreeFloor(heading.heading() - v.heading()) / 45;
      input[1] = sensor[1]/sensorDistance;
      input[2] = sensor[7]/sensorDistance;
      input[3] = v.mag()/5;

      float [] output=brain.feedForward(input);

      if (output [0]>=output[1]&&(output[0]>output[2]))
        throttle();
      else if (output[2]>=output[1]&&(output[2]>=output[0]))
        brake();

      if ((output[3]>output[4])&&(output[3]>output[5]))
        heading.rotate(radians(-3));
      else if ((output[4]>output[3])&&(output[4]>output[5]))
        heading.rotate(radians(3));
    }
  }
  //----------------------------------------------------------------------------------------------------------------
  float getFitness() {
    float lastPointDistance=PVector.sub(p, lastPoint).mag();
    float score = point;
    //  + (millis() - population.time)/1000;
    if (checkPoint != pointGates.size() - 1)
      score += lastPointDistance / 40;
    return score * score;
  }

  void pick() {
    float distance=PVector.sub(p, new PVector(mouseX, mouseY)).mag();
    if (distance<50)
      point*=1000;
  }
}

//-----------------------------------------------------------------------------------------------------------------------------
boolean linesTouching(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
  float denominator = ((x2 - x1) * (y4 - y3)) - ((y2 - y1) * (x4 - x3));
  float numerator1 = ((y1 - y3) * (x4 - x3)) - ((x1 - x3) * (y4 - y3));
  float numerator2 = ((y1 - y3) * (x2 - x1)) - ((x1 - x3) * (y2 - y1));

  // Detect coincident lines (has a problem, read below)
  if (denominator == 0) return numerator1 == 0 && numerator2 == 0;

  float r = numerator1 / denominator;
  float s = numerator2 / denominator;

  return (r >= 0 && r <= 1) && (s >= 0 && s <= 1);
}

PVector lineIntersection(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

  // calculate the distance to intersection point
  float uA = ((x4-x3)*(y1-y3) - (y4-y3)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
  float uB = ((x2-x1)*(y1-y3) - (y2-y1)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));

  // if uA and uB are between 0-1, lines are colliding
  if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {
    return new PVector(x1 + (uA * (x2-x1)), y1 + (uA * (y2-y1)));
  }
  return null;
}

//My solution to find angle between Vectors
float degreeFloor(float rad){
  float degree = degrees(rad);
  while(degree > 360)
    degree -= 360;
  while(degree < -360)
    degree += 360;
  if(degree < -180)
    degree = 360 + degree;
  if(degree > 180)
    degree = -360 + degree;

  return degree;
}