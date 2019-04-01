package org.javaparser.samples;

import java.util.Stack;
import java.util.stream.Stream;

interface Age
{
    int x = 21;
    void getAge();
}

public class NewAge implements  Age {
    NewAge() {}

    void getAge() {
        return;
    }
}

/**
 * A Simple Reverse Polish Notation calculator with memory function.
 */
public class ReversePolishNotation {

    class ReversePolishNotationSub {
        Age oj1;
        NewAge oj2;

        ReversePolishNotationSub() {

            this.oj1 = new Age() {
                @Override
                public void getAge() {
                    return;
                }
            };

        }

        private void doSomethingElse() {
            this.oj1.getAge();
            this.oj2.getAge();
        }

        public void doSomething() {
            this.doSomethingElse();
        }
    }

    // What does this do?
    public static int ONE_BILLION = 1000000000;

    private double memory = 0;

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


    public void memoryStore(double value){

        ReversePolishNotationSub sub = new ReversePolishNotationSub();
        sub.doSomething();

        memory = value;
    }

}
/* EOF */
