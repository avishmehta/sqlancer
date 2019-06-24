package lama.mysql.ast;

import lama.IgnoreMeException;
import lama.Randomly;
import lama.mysql.ast.MySQLCastOperation.CastType;
import lama.mysql.ast.MySQLConstant.MySQLTextConstant;

public class MySQLBinaryLogicalOperation extends MySQLExpression {

	private final MySQLExpression left;
	private final MySQLExpression right;
	private final MySQLBinaryLogicalOperator op;
	private final String textRepresentation;

	public enum MySQLBinaryLogicalOperator {
		AND("AND", "&&") {
			@Override
			public MySQLConstant apply(MySQLConstant left, MySQLConstant right) {
				if (left.isNull() && right.isNull()) {
					return MySQLConstant.createNullConstant();
				} else if (left.isNull()) {
					if (right.asBooleanNotNull()) {
						return MySQLConstant.createNullConstant();
					} else {
						return MySQLConstant.createFalse();
					}
				} else if (right.isNull()) {
					if (left.asBooleanNotNull()) {
						return MySQLConstant.createNullConstant();
					} else {
						return MySQLConstant.createFalse();
					}
				} else {
					return MySQLConstant.createBoolean(left.asBooleanNotNull() && right.asBooleanNotNull());
				}
			}
		},
		OR("OR", "||") {
			@Override
			public MySQLConstant apply(MySQLConstant left, MySQLConstant right) {
				if (!left.isNull() && left.asBooleanNotNull()) {
					return MySQLConstant.createTrue();
				} else if (!right.isNull() && right.asBooleanNotNull()) {
					return MySQLConstant.createTrue();
				} else if (left.isNull() || right.isNull()) {
					return MySQLConstant.createNullConstant();
				} else {
					return MySQLConstant.createFalse();
				}
			}
		},
		XOR("XOR") {
			@Override
			public MySQLConstant apply(MySQLConstant left, MySQLConstant right) {
				if (left.isNull() || right.isNull()) {
					return MySQLConstant.createNullConstant();
				}
				/* workaround for https://bugs.mysql.com/bug.php?id=95927 */
				if (left.isString() || right.isString()) {
					throw new IgnoreMeException();
				}
				boolean xorVal = left.asBooleanNotNull() ^ right.asBooleanNotNull();
				return MySQLConstant.createBoolean(xorVal);
			}
		};

		private final String[] textRepresentations;

		private MySQLBinaryLogicalOperator(String... textRepresentations) {
			this.textRepresentations = textRepresentations;
		}

		String getTextRepresentation() {
			return Randomly.fromOptions(textRepresentations);
		}

		public abstract MySQLConstant apply(MySQLConstant left, MySQLConstant right);

		public static MySQLBinaryLogicalOperator getRandom() {
			return Randomly.fromOptions(values());
		}
	}

	public MySQLBinaryLogicalOperation(MySQLExpression left, MySQLExpression right, MySQLBinaryLogicalOperator op) {
		this.left = left;
		this.right = right;
		this.op = op;
		this.textRepresentation = op.getTextRepresentation();
	}

	public MySQLExpression getLeft() {
		return left;
	}

	public MySQLBinaryLogicalOperator getOp() {
		return op;
	}

	public MySQLExpression getRight() {
		return right;
	}

	public String getTextRepresentation() {
		return textRepresentation;
	}

	@Override
	public MySQLConstant getExpectedValue() {
		MySQLConstant leftExpected = left.getExpectedValue();
		MySQLConstant rightExpected = right.getExpectedValue();

		/**
		 * workaround for https://bugs.mysql.com/bug.php?id=95958
		 */
		boolean leftIsSmallFloatingPointText = leftExpected.isString()
				&& ((MySQLTextConstant) leftExpected).asBooleanNotNull()
				&& leftExpected.castAs(CastType.SIGNED).getInt() == 0;
		boolean rightIsSmallFloatingPointText = rightExpected.isString()
				&& ((MySQLTextConstant) rightExpected).asBooleanNotNull()
				&& rightExpected.castAs(CastType.SIGNED).getInt() == 0;
		if (leftIsSmallFloatingPointText || rightIsSmallFloatingPointText) {
			throw new IgnoreMeException();
		}

		return op.apply(leftExpected, rightExpected);
	}

}
