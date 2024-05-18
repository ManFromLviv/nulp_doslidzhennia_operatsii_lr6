package org.example;

public class Main {
	public static void main(String[] args) {
		System.out.println("Pavlo Valchevskyi, IO-11sp for Doslidzhennya operatsiy, LR 6 variant 91");
		LinearFractionalProblem min = new LinearFractionalProblem(
				new Double[]{3.0, 4.0}, new Double[]{2.0, 1.0});
		min.addFunction(new Function(new Double[]{8.0, -5.0}, 40, Function.Sign.LESS));
		min.addFunction(new Function(new Double[]{2.0, 5.0}, 10, Function.Sign.GREATER));
		min.addFunction(new Function(new Double[]{-6.0, 5.0}, 60, Function.Sign.LESS));
		min.addFunction(new Function(new Double[]{2.0, 1.0}, 14, Function.Sign.LESS));

		FunctionSystem max = new LinearFractionalProblem(min);
		System.out.println(min);

		max.getOptimal(true);
		min.getOptimal(false);
	}
}