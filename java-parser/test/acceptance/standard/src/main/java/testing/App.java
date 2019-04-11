package testing;

public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        String polishNotation = "15 7 +";

        System.out.println(polishNotation);

        ReversePolishNotation reversePolishNotation = new ReversePolishNotation();

        Double result = reversePolishNotation.launchCalc(polishNotation);

        System.out.println(result);

        System.out.println(ReversePolishNotation.fetchZero());
    }
}
