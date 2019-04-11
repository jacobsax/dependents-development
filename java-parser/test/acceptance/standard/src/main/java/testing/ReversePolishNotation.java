package testing;


import java.util.Stack;
import java.util.stream.Stream;


/**
 * A Simple Reverse Polish Notation calculator with memory function. From the JavaParser example project
 */
public class ReversePolishNotation {

    private double memory = 0;

    public static Double fetchZero() {
        return 0.0;
    }

    public static Double convertValue(Double input) {
        return 0.0;
    }

    /**
     * Takes reverse polish notation style string and returns the resulting calculation.
     *
     * @param input mathematical expression in the reverse Polish notation format
     * @return the calculation result
     */
    public Double calc(String input) {

        String[] tokens = input.split(" ");
        Stack<Double> numbers = new Stack<>();

        Stream.of(tokens).forEach(t -> {
            double a;
            double b;
            switch(t){
                case "+":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a + b);
                    break;
                case "/":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a / b);
                    break;
                case "-":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a - b);
                    break;
                case "*":
                    b = numbers.pop();
                    a = numbers.pop();
                    numbers.push(a * b);
                    break;
                default:
                    numbers.push(Double.valueOf(t));
            }
        });
        return numbers.pop();
    }

    public Double launchCalc(String input) {
        return this.calc(input);
    }

    /**
     * Memory Recall uses the number in stored memory, defaulting to 0.
     *
     * @return the double
     */
    public double memoryRecall(){
        return memory;
    }

    /**
     * Memory Clear sets the memory to 0.
     */
    public void memoryClear(){
        memory = 0;
    }
}