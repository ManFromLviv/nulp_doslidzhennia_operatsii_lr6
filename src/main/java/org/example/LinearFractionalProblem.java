package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/*

У цьому класі реалізована система лінійних дробових задач (Linear Fractional Problems), яка є підкласом класу FunctionSystem. Основні аспекти цього класу включають:

Ініціалізація:

Конструктор, який приймає чисельник та знаменник дробової функції.
У конструкторі створюються об'єкти функцій з чисельника та знаменника, а також додаткові коефіцієнти для функцій, що додаються.
Додавання функцій:

Перевизначений метод addFunction, який додає нову функцію до системи.
Додавання додаткових коефіцієнтів до функцій, які враховують чисельник та знаменник.
Обчислення результатів:

Перевизначений метод setRes, який обчислює результати для дробової задачі.
Перевизначений метод printResult, який виводить результати у вигляді дробових значень.
Цей клас розширює функціональність системи функцій для обробки дробових задач, де функції виражені як дроби, а не просто лінійні функції.
*/

public class LinearFractionalProblem extends FunctionSystem {
	private List<Double> y;

	public LinearFractionalProblem(Double[] numerator, Double[] denominator) {
		super(new Function(numerator));
		objective.coefficients.add(0, 0.0);

		functions.add(new Function(denominator, 1, Function.Sign.EQUAL));
		functions.get(0).coefficients.add(0, 0.0);
		coefNum = denominator.length + 1;
	}

	public LinearFractionalProblem(LinearFractionalProblem system) {
		super(system);
		this.y = system.y;
	}

	@Override
	public void addFunction(Function function) {
		Double[] coefs = new Double[function.coefficients.size() + 1];
		coefs[0] = function.sign == Function.Sign.LESS ? -function.value : function.value;
		for (int i = 0; i < function.coefficients.size(); i++) {
			coefs[i + 1] = function.sign == Function.Sign.LESS ?
					function.coefficients.get(i) : -function.coefficients.get(i);
		}
		Function func = new Function(coefs, 0, Function.Sign.LESS);
		super.addFunction(func);
	}

	@Override
	protected List<Double> setRes() {
		super.setRes();
		y = res;
		res = new ArrayList<>(coefNum - 1);
		for (int i = 1; i < coefNum; i++) {
			res.add(y.get(i) / y.get(0));
		}
		return res;
	}

	@Override
	protected void printResult() {
		if (res.isEmpty()) {
			System.out.println("There is no " + (!inverse ? "max" : "min") + " value");
		} else {
			StringJoiner joiner = new StringJoiner(", ", "[", "]");
			for (double x : y) {
				joiner.add("%.3f".formatted(x));
			}
			System.out.println("Y: " + joiner);

			joiner = new StringJoiner(", ", "[", "]");
			for (double x : res) {
				joiner.add("%.3f".formatted(x));
			}
			System.out.printf((!inverse ? "Max" : "Min") + " point - %s, F" +
							(!inverse ? "max" : "min") + " = %.3f%n", joiner,
					inverse ? -getObjectiveValue(y) : getObjectiveValue(y));
		}
		System.out.println();
	}

	@Override
	protected void buildFunction(Function function, StringBuilder builder) {
		builder.append(function.coefficients.get(0)).append("y0");
		for (int i = 1; i < function.coefficients.size(); i++) {
			double x = function.coefficients.get(i);
			builder.append(x < 0 ? " - " : " + ")
					.append(Math.abs(x)).append('y').append(i);
		}
		if (function != objective) {
			builder.append(" ").append(function.sign)
					.append(" ").append(function.value);
		}
		builder.append("\n");
	}
}