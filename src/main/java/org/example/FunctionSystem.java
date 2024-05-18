package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/*
У цьому класі реалізовано систему лінійних функцій, яка використовується для оптимізації методом двоїстого симплекс-методу. Основні функції та методи класу включають:

Ініціалізація системи функцій:

Конструктори для створення нових систем функцій або копіювання існуючих.
Метод getDual, який повертає дуальну систему функцій.
Метод getOptimal, який знаходить оптимальне значення функції в системі.
Методи getOptimalDual та getOptimalInt, які знаходять оптимальне значення для двоїстого симплекс-методу.
Додавання функцій до системи:

Метод addFunction, який додає нову функцію до системи.
Метод addFunctionComplete, який додає повну функцію до системи.
Виконання оптимізації:

Методи findOptimalDualSimplex та getOptimal, які виконують оптимізацію за допомогою симплекс-методу.
Методи setBasis, minRow, toOne, які допомагають в процесі оптимізації.
Додаткові допоміжні методи:

Методи для роздруківки результатів (print, printHeader, printFunctions, printZCDiff, printResult).
Методи для обчислення значень та множення векторів (getObjectiveValue, getVectorMultiplication).
Цей клас дозволяє моделювати системи лінійних функцій та знаходити їхні оптимальні значення за допомогою симплекс-методу.
 */

public class FunctionSystem {
	protected String delimiter;
	protected final Function objective;
	protected final List<Function> functions = new ArrayList<>();
	protected int prevCoefNum;
	protected int coefNum;
	protected final int orgSize;
	protected boolean inverse;
	protected List<Double> res;
	protected final List<Integer> basis;
	protected final List<Double> basisCoef;
	protected final List<Double> zcDiff;

	public FunctionSystem(Function objective) {
		this.objective = objective;
		coefNum = orgSize = objective.coefficients.size();
		prevCoefNum = -1;
		res = new ArrayList<>();
		basis = new ArrayList<>();
		basisCoef = new ArrayList<>();
		zcDiff = new ArrayList<>();
		inverse = false;
	}

	public FunctionSystem(FunctionSystem system) {
		objective = new Function(system.objective);
		for (Function f : system.functions) {
			functions.add(new Function(f));
		}
		prevCoefNum = system.prevCoefNum;
		coefNum = system.coefNum;
		orgSize = system.orgSize;
		inverse = system.inverse;
		delimiter = system.delimiter;
		res = new ArrayList<>(system.res);
		basis = new ArrayList<>(system.basis);
		basisCoef = new ArrayList<>(system.basisCoef);
		zcDiff = new ArrayList<>(system.zcDiff);
	}

	public FunctionSystem getDual(boolean max) {
		FunctionSystem system = new FunctionSystem(this);
		Double[][] dualFunctions = new Double[system.orgSize][system.functions.size()];
		Double[] dualValues = new Double[system.orgSize];
		Double[] dualObjective = new Double[system.functions.size()];

		if (max) {
			system.objective.multiply(-1);
		}

		for (int i = 0; i < system.functions.size(); i++) {
			if (system.functions.get(i).sign == Function.Sign.LESS) {
				system.functions.get(i).multiply(-1);
			}

			for (int j = 0; j < system.orgSize; j++) {
				dualFunctions[j][i] = system.functions.get(i).coefficients.get(j);
			}
			dualObjective[i] = system.functions.get(i).value;
		}
		for (int i = 0; i < system.orgSize; i++) {
			dualValues[i] = system.objective.coefficients.get(i);
		}

		system = new FunctionSystem(new Function(dualObjective));
		for (int i = 0; i < dualFunctions.length; i++) {
			system.addFunction(new Function(dualFunctions[i], dualValues[i], Function.Sign.LESS));
		}
		system.inverse = false;
		return system;
	}

	public List<Double> getOptimalDual() {
		return getOptimal(true);
	}

	public List<Double> getOptimalInt(boolean max) {
		prevCoefNum = -1;
		getOptimal(max);
		if (res.isEmpty()) {
			return null;
		}

		List<Double> valuesFractional = new ArrayList<>();
		for (Function function : functions) {
			valuesFractional.add(getFractional(function.value));
		}

		int indexMax;
		try {
			indexMax = optimalIndFractional(valuesFractional, true);
		} catch (Exception e) {
			res.clear();
			printResult();
			return null;
		}

		if (indexMax != -1) {
			System.out.println("Result contains non-integer values. We need to use Gomori's method");
		}
		while (indexMax != -1) {
			int ind = indexMax;
			Double[] newRow = new Double[coefNum];
			Arrays.setAll(newRow, (i) -> {
				double fractional = functions.get(ind).coefficients.get(i);
				fractional = getFractional(fractional);
				if (fractional < 0.00001) {
					return 0.0;
				} else {
					return -fractional;
				}
			});

			addFunction(new Function(newRow, -valuesFractional.get(indexMax), Function.Sign.LESS));
			basis.add(coefNum - 1);
			basisCoef.add(0.0);
			if (findOptimalDualSimplex() == null) {
				res.clear();
				printResult();
				return null;
			}

			for (int i = 0; i < functions.size(); i++) {
				double fractional = getFractional(functions.get(i).value);
				fractional = fractional < 0.00001 || fractional > 0.99999 ? 0 : fractional;
				if (valuesFractional.size() < i + 1) {
					valuesFractional.add(fractional);
				} else {
					valuesFractional.set(i, fractional);
				}
			}

			try {
				indexMax = optimalIndFractional(valuesFractional, true);
			} catch (Exception e) {
				res.clear();
				printResult();
				return null;
			}
		}

		printResult();
		return res;
	}

	private double getFractional(double x) {
		return x - Math.floor(x);
	}

	private int optimalIndFractional(List<Double> list, boolean max)
			throws Exception {
		int ind = -1;
		boolean found = false;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) != 0 && (ind == -1 ||
					max && list.get(i) > list.get(ind) ||
					!max && list.get(i) < list.get(ind))) {
				found = true;
				boolean hasFractional = false;
				for (int j = 0; j < functions.get(i).coefficients.size(); j++) {
					double fractional = getFractional(functions.get(i).coefficients.get(j));
					hasFractional = hasFractional || fractional > 0.00001 && fractional < 0.99999;
				}
				if (hasFractional) {
					ind = i;
				}
			}
		}

		if (found && ind == -1) {
			throw new Exception();
		}
		return ind;
	}

	public List<Double> findOptimalDualSimplex() {
		setZcDiff();
		print();

		int minRow = minRow();
		while (minRow != -1) {
			int minCol = -1;
			double minValue = 0;
			for (int i = 0; i < coefNum; i++) {
				if (functions.get(minRow).coefficients.get(i) < -0.00001) {
					double value = -zcDiff.get(i) /
							functions.get(minRow).coefficients.get(i);
					if (functions.get(minRow).coefficients.get(i) < -0.00001 &&
							(minCol == -1 || value < minValue)) {
						minCol = i;
						minValue = value;
					}
				}
			}

			if (minCol == -1) {
				res.clear();
				return null;
			}
			toOne(minRow, minCol);
			basis.set(minRow, minCol);
			basisCoef.set(minRow, objective.coefficients.get(minCol));
			setZcDiff();

			print();
			minRow = minRow();
		}

		return setRes();
	}

	private int minRow() {
		int minRow = -1;
		for (int i = 0; i < functions.size(); i++) {
			if (functions.get(i).value < -0.00001 && (minRow == -1 ||
					functions.get(i).value < functions.get(minRow).value)) {
				minRow = i;
			}
		}
		return minRow;
	}

	public List<Double> getOptimal(boolean max) {
		getOptimal(max, false);
		return res;
	}

	public List<Double> getOptimal(boolean max, boolean additionalVariable) {
		if (max == inverse) {
			objective.multiply(-1);
			inverse = !inverse;
		}
		setDelimiter();

		print();
		if (!setBasis(additionalVariable)) {
			print();
			res.clear();
			printResult();
			return null;
		}

		for (Integer ind : basis) {
			basisCoef.add(objective.coefficients.get(ind));
		}
		if (additionalVariable) {
			print();
		}

		do {
			setZcDiff();
			print();

			int maxNotOpt = -1;
			for (int i = 0; i < zcDiff.size(); i++) {
				if (zcDiff.get(i) < 0) {
					if (maxNotOpt == -1 ||
							Math.abs(zcDiff.get(i)) > Math.abs(zcDiff.get(maxNotOpt))) {
						maxNotOpt = i;
					}
				}
			}

			if (maxNotOpt == -1) {
				setRes();
				printResult();
				return res;
			}

			int minPropInd = -1;
			double minProportion = 0;
			for (int i = 0; i < functions.size(); i++) {
				double coefficient = functions.get(i).coefficients.get(maxNotOpt);
				if (coefficient > 0) {
					double proportion = functions.get(i).value / coefficient;
					if (minPropInd == -1 || minProportion > proportion) {
						minPropInd = i;
						minProportion = proportion;
					}
				}
			}

			if (minPropInd != -1) {
				toOne(minPropInd, maxNotOpt);
				basis.set(minPropInd, maxNotOpt);
				basisCoef.set(minPropInd,
						objective.coefficients.get(basis.get(minPropInd)));
			} else {
				res.clear();
				printResult();
				return null;
			}
		} while (true);
	}

	private boolean setBasis(boolean additionalVariable) {
		basis.clear();
		basisCoef.clear();
		for (Function f : functions) {
			if (f.value < -0.001) {
				f.multiply(-1);
			}
		}

		for (int i = 0; i < functions.size(); i++) {
			for (int j = 0; j < coefNum; j++) {
				if (isBasis(i, j)) {
					basis.add(j);
					break;
				}
			}
			if (basis.size() == i + 1) {
				continue;
			}

			if (additionalVariable) {
				functions.get(i).coefficients.add(1.0);
				for (int j = 0; j < functions.size(); j++) {
					if (j != i) {
						functions.get(j).coefficients.add(0.0);
					}
				}
				objective.coefficients.add(-100.0);
				basis.add(++coefNum - 1);
			} else {
				int min = -1;
				for (int j = 0; j < coefNum; j++) {
					double multiply = functions.get(i).value == 0 ? functions.get(i).coefficients.get(j)
							: functions.get(i).coefficients.get(j) * functions.get(i).value;
					if (Math.abs(functions.get(i).coefficients.get(j)) > 0.0001 &&
							multiply > 0) {
						if (min == -1 || Math.abs(functions.get(i).coefficients.get(j)) < Math.abs(functions.get(i).coefficients.get(min))) {
							min = j;
						}
					}
				}
				if (min != -1) {
					toOne(i, min);
				}
				basis.add(min);
			}
		}

		print();
		boolean loop;
		int num = 0;
		do {
			loop = false;
			for (int i = 0; i < functions.size(); i++) {
				if (functions.get(i).value < -0.0001) {
					int min = -1;
					for (int j = 0; j < coefNum; j++) {
						if (functions.get(i).coefficients.get(j) < -0.0001) {
							if (min == -1 ||
									functions.get(i).coefficients.get(j) < functions.get(i).coefficients.get(min)) {
								min = j;
							}
						}
					}
					if (min != -1) {
						toOne(i, min);
						basis.set(i, min);
					}
					loop = true;
				}
			}
			num++;
			if (num > 100) {
				return false;
			}
		} while (loop);

		for (int b : basis) {
			if (b == -1) {
				return false;
			}
		}
		return true;
	}

	private boolean isBasis(int x, int y) {
		if (functions.get(x).coefficients.get(y) != 1) {
			return false;
		}
		for (int i = 0; i < functions.size(); i++) {
			if (i != x && Math.abs(functions.get(i).coefficients.get(y)) > 0.00001) {
				return false;
			}
		}
		return true;
	}

	private void setZcDiff() {
		for (int i = 0; i < coefNum; i++) {
			if (zcDiff.size() < (i + 1)) {
				zcDiff.add(getVectorMultiplication(basisCoef, i));
			} else {
				zcDiff.set(i, getVectorMultiplication(basisCoef, i));
			}
		}
	}

	protected List<Double> setRes() {
		res = new ArrayList<>(coefNum);
		for (int i = 0; i < coefNum; i++) {
			res.add(0.0);
		}
		for (int i = 0; i < basis.size(); i++) {
			res.set(basis.get(i), functions.get(i).value);
		}
		return res;
	}

	public void setDelimiter() {
		delimiter = "+" + ("-".repeat(10) + "+").repeat(coefNum + 2);
	}

	public double getObjectiveValue(List<Double> x) {
		double res = 0;
		for (int i = 0; i < x.size(); i++) {
			res += objective.coefficients.get(i) * x.get(i);
		}
		return res;
	}

	public void addFunction(Function function) {
		functions.add(function);
		for (int i = function.coefficients.size(); i < coefNum; i++) {
			function.coefficients.add(0.0);
		}
		if (function.sign == Function.Sign.GREATER) {
			function.coefficients.add(-1.0);
		} else if (function.sign == Function.Sign.LESS) {
			function.coefficients.add(1.0);
		}
		if (function.sign != Function.Sign.EQUAL) {
			for (Function f : functions) {
				if (!function.equals(f)) {
					f.coefficients.add(0.0);
				}
			}
			objective.coefficients.add(0.0);
			coefNum++;
		}
		function.sign = Function.Sign.EQUAL;
	}

	public void addFunctionComplete(Function function) {
		functions.add(function);
	}

	public void print() {
		setDelimiter();
		if (prevCoefNum != coefNum) {
			printHeader();
			prevCoefNum = coefNum;
		}
		printFunctions();
		if (!zcDiff.isEmpty()) {
			printZCDiff();
		}
	}

	public void printHeader() {
		System.out.println(delimiter);
		System.out.println("|%10s".formatted("") +
				objective.coefToString() + "%10s|".formatted("b"));
		System.out.println(delimiter);
	}

	public void printFunctions() {
		for (int i = 0; i < functions.size(); i++) {
			System.out.printf("|%6.1f p%-2d%s%n",
					basisCoef.isEmpty() ? 0 : basisCoef.get(i),
					basis.isEmpty() ? 0 : basis.get(i) + 1,
					functions.get(i).toString());
		}
		System.out.println(delimiter);
	}

	private void printZCDiff() {
		StringBuilder builder = new StringBuilder("|%10s|".formatted(""));
		for (double v : zcDiff) {
			builder.append("%10.5f|".formatted(v));
		}
		builder.append("%10.5f|".formatted(getVectorMultiplication(basisCoef, coefNum)));

		System.out.println(builder);
		System.out.println(delimiter);
	}

	protected void printResult() {
		if (res.isEmpty()) {
			System.out.println("There is no " + (!inverse ? "max" : "min") + " value");
		} else {
			StringJoiner joiner = new StringJoiner(", ", "[", "]");
			for (double x : res) {
				joiner.add("%.3f".formatted(x));
			}
			System.out.printf((!inverse ? "Max" : "Min") + " point - %s, F" +
							(!inverse ? "max" : "min") + " = %.3f%n", joiner,
					inverse ? -getObjectiveValue(res) : getObjectiveValue(res));
		}
		System.out.println();
	}

	public double getVectorMultiplication(List<Double> vector, int y) {
		if (vector.size() != functions.size()) {
			return Double.MIN_VALUE;
		}
		double res;
		if (y < coefNum) {
			res = -objective.coefficients.get(y);
			for (int i = 0; i < vector.size(); i++) {
				res += functions.get(i).coefficients.get(y) * vector.get(i);
			}
		} else {
			res = 0;
			for (int i = 0; i < vector.size(); i++) {
				res += functions.get(i).value * vector.get(i);
			}
		}
		return res;
	}

	public void toOne(int x, int y) {
		functions.get(x).divide(functions.get(x).coefficients.get(y));
		for (int i = 0; i < functions.size(); i++) {
			if (i != x) {
				functions.get(i).add(functions.get(x), -functions.get(i).coefficients.get(y));
			}
		}
	}

	protected void buildFunction(Function function, StringBuilder builder) {
		builder.append(function.coefficients.get(0)).append("x1");
		for (int i = 1; i < function.coefficients.size(); i++) {
			double x = function.coefficients.get(i);
			builder.append(x < 0 ? " - " : " + ")
					.append(Math.abs(x)).append('x').append(i + 1);
		}
		if (function != objective) {
			builder.append(" ").append(function.sign)
					.append(" ").append(function.value);
		}
		builder.append("\n");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("F = ");
		buildFunction(objective, builder);
		functions.forEach((f) -> buildFunction(f, builder));
		return builder.toString();
	}
}