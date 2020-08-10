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

  float [] feedForward(float [] input) {
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

  void mutate(float mutationRate) {
    for (Matrix weight : weights)
      weight.mutate(mutationRate);
    for (Matrix bias : biases)
      bias.mutate(mutationRate);
  }

  NeuralNetwork crossover(NeuralNetwork other) {
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

  NeuralNetwork copy() {
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

  void train(float [] inputArray, float [] targetArray) {
    float learningRate=0.5;
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
  int margin = int(32 / zoom);
  float boardX       = 0;
  int boardSize      = int((width / 4 + margin) / zoom );
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
  void visualizeNN() {
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
        if (value > 0.5)
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
      float y              = 50 + 40 * (-0.5  + 1 - layer[layer.length - 1]) / 2 + scroll;
      float valueActive    = output.get(0, 0) ;
      float valueDeactive  = output.get(1, 0) ;
      fill(0, 255);
      if (valueActive > valueDeactive)
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("GAS", x + 1, y + 5);

      y              = 50 + 40 * (3.5  + 1 - layer[layer.length - 1]) / 2 + scroll;
      valueActive    = output.get(2, 0) ;
      valueDeactive  = output.get(3, 0) ;
      if (valueActive > valueDeactive)
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("REM", x + 1, y + 5);

      y              = 50 + 40 * (7.5  + 1 - layer[layer.length - 1]) / 2 + scroll;
      valueActive    = output.get(4, 0) ;
      if ((valueActive > output.get(5, 0))&&(valueActive > output.get(6, 0)))
        decision.fill(0, 255, 0);
      else
        decision.fill(255, 0, 0);
      decision.text("KIRI", x + 1, y + 5);

      y              = 50 + 40 * (9.5  + 1 - layer[layer.length - 1]) / 2 + scroll;
      valueActive    = output.get(5, 0) ;
      if ((valueActive > output.get(4, 0))&&(valueActive > output.get(6, 0)))
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
