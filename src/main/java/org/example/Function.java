package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
У цьому класі визначається об'єкт, що представляє функцію з коефіцієнтами, значенням та знаком. Клас містить конструктори для ініціалізації об'єктів, методи для додавання, ділення та множення функцій на число, а також методи для перетворення коефіцієнтів та значення у рядок. Клас також містить вкладений перерахування для представлення можливих знаків функцій.

Основні елементи класу:

coefficients: Список коефіцієнтів функції.
value: Значення функції.
sign: Знак функції (GREATER, EQUAL, LESS).
Методи виконують операції з об'єктами типу Function:

add: Додає іншу функцію, помножену на певне число, до поточної функції.
divide: Ділить кожен коефіцієнт та значення функції на задане число.
multiply: Перемножує кожен коефіцієнт та значення функції на задане число.
Також є методи для перетворення коефіцієнтів та значення у рядок.

Клас містить вкладений перерахування для представлення можливих знаків функцій (більше або дорівнює, дорівнює, менше або дорівнює).
*/

public class Function {
	List<Double> coefficients;
	double value;
	Sign sign;

	public Function(Double[] coefficients, double value, Sign sign) {
		this.coefficients = new ArrayList<>(Arrays.asList(coefficients));
		this.value = value;
		this.sign = sign;
	}

	public Function(Double[] coefficients) {
		this.coefficients = new ArrayList<>(Arrays.asList(coefficients));
		sign = Sign.EQUAL;
	}

	public Function(Function function) {
		coefficients = new ArrayList<>(function.coefficients);
		value = function.value;
		sign = function.sign;
	}

	public void add(Function function, double x) {
		for (int i = 0; i < coefficients.size(); i++) {
			coefficients.set(i, coefficients.get(i) + function.coefficients.get(i) * x);
		}
		value += function.value * x;
	}

	public void divide(double x) {
		coefficients.replaceAll(aDouble -> aDouble / x);
		value /= x;
	}

	public void multiply(double x) {
		coefficients.replaceAll(aDouble -> aDouble * x);
		value *= x;
		if (x < 0) {
			sign = sign.opposite();
		}
	}

	public String coefToString() {
		StringBuilder builder = new StringBuilder("|");
		coefficients.forEach((a) -> builder.append("%10.5f|".formatted(a)));
		return builder.toString();
	}

	@Override
	public String toString() {
		return coefToString() + "%10.5f|".formatted(value);
	}

	public enum Sign {
		GREATER(">="),
		EQUAL("="),
		LESS("<=");

		private final String symbol;

		Sign(String symbol) {
			this.symbol = symbol;
		}

		Sign opposite() {
			return switch (this) {
				case GREATER -> LESS;
				case EQUAL -> EQUAL;
				case LESS -> GREATER;
			};
		}

		@Override
		public String toString() {
			return symbol;
		}
	}
}