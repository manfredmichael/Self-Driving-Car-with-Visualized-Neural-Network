class Population {
  float mutationRate=0.02;

  int time;
  int alive;

  PVector bestPos=new PVector(0, 0);

  Population() {
    time=millis();
    alive=cars.size();
  }

  void update() {
    fill(255, 50);
    ellipse(bestPos.x, bestPos.y, 40, 40);

    alive=0;
    for (Car car : cars) {
      car.move();
      car.checkCrash();
      car.sense();
      car.think();
      //if (mousePressed)
      //  car.pick();
      if (!car.dead && car.checkPoint < pointGates.size() - 1)
        alive++;
    }

    if(!showBest){
      tint(255, 100);
      for (Car car : cars)
        car.show();
      tint(255, 255);
    }
    cars.get(0).show();

    //displayBestStat();


    if ((millis()>time+roundTime*1000)||(alive<=0 && millis()>time+2000 && millis() > 10000)) {
      time=millis();
      alive=cars.size();
      regenerate();
      generation++;
    }
  }

  void displayBestStat() {
    text("S "+str(generation), cars.get(0).p.x+width/3-50, cars.get(0).p.y+height/3-100);
    text("Generation "+str(generation), cars.get(0).p.x+width/3-50, cars.get(0).p.y+height/3-80);
  }

  void regenerate() {
    Car best=new Car();
    ArrayList<Car> parentPool=new ArrayList<Car>();
    float highest=0.01;
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

    if (sqrt(highest)>60)
      roundTime=int(sqrt(highest)/10);
    else
      roundTime=6;
    if (roundTime>180)
      roundTime=180;
    println(sqrt(highest));

    for (Car car : cars) {
      float chance=car.getFitness()*20/highest;
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
      if(i < cars.size() * 3 / 4){
        int indexA=floor(random(parentPool.size()));
        cars.get(i).brain=parentPool.get(indexA).brain.copy();
        }
      else{
        int indexA = floor(random(parentPool.size()));
        int indexB = floor(random(parentPool.size()));
        cars.get(i).brain = parentPool.get(indexA).brain.crossover(parentPool.get(indexB).brain);
      }
      }
    }

    Collections.shuffle(cars);

    for (int i = 0; i < clones * 7 / 8; i++) {
      cars.get(i).brain.mutate(mutationRate);
      cars.get(i).reset();
    } 
    
    for (int i = clones * 7 / 8; i < clones; i++) {
      cars.get(i).brain.mutate(0.1);
      cars.get(i).reset();
    }
    best.reset();
    best.think();
    cars.set(0, best.copy());
    // saveBrain();
  }
}

float acceptReject(float fitness, float maxFitness) {
  float r=random(maxFitness);
  if (r<=fitness)
    return fitness;
  else
    return 0;
}

Car pickOne(ArrayList<Car> candidate, float sum) {
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
