//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package software.bernie.geckolib3.core.molang.math;

import java.util.HashSet;
import java.util.Set;

public enum Operation {
    ADD("+", 1) {
        public double calculate(double a, double b) {
            return a + b;
        }
    },
    SUB("-", 1) {
        public double calculate(double a, double b) {
            return a - b;
        }
    },
    MUL("*", 2) {
        public double calculate(double a, double b) {
            return a * b;
        }
    },
    DIV("/", 2) {
        public double calculate(double a, double b) {
            return a / (b == 0.0 ? 1.0 : b);
        }
    },
    MOD("%", 2) {
        public double calculate(double a, double b) {
            return a % b;
        }
    },
    POW("^", 3) {
        public double calculate(double a, double b) {
            return Math.pow(a, b);
        }
    },
    AND("&&", 5) {
        public double calculate(double a, double b) {
            return a != 0.0 && b != 0.0 ? 1.0 : 0.0;
        }
    },
    OR("||", 5) {
        public double calculate(double a, double b) {
            return a == 0.0 && b == 0.0 ? 0.0 : 1.0;
        }
    },
    LESS("<", 5) {
        public double calculate(double a, double b) {
            return a < b ? 1.0 : 0.0;
        }
    },
    LESS_THAN("<=", 5) {
        public double calculate(double a, double b) {
            return a <= b ? 1.0 : 0.0;
        }
    },
    GREATER_THAN(">=", 5) {
        public double calculate(double a, double b) {
            return a >= b ? 1.0 : 0.0;
        }
    },
    GREATER(">", 5) {
        public double calculate(double a, double b) {
            return a > b ? 1.0 : 0.0;
        }
    },
    EQUALS("==", 5) {
        public double calculate(double a, double b) {
            return equals(a, b) ? 1.0 : 0.0;
        }
    },
    NOT_EQUALS("!=", 5) {
        public double calculate(double a, double b) {
            return !equals(a, b) ? 1.0 : 0.0;
        }
    };

    private static final Set<String> OPERATORS = new HashSet<>();

    static {
        for (Operation op : values()) {
            OPERATORS.add(op.sign);
        }
    }

    public final String sign;
    public final int value;

    public static boolean contains(String operation) {
        return OPERATORS.contains(operation);
    }

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < 1.0E-5;
    }

    Operation(String sign, int value) {
        this.sign = sign;
        this.value = value;
    }

    public abstract double calculate(double var1, double var3);
}
