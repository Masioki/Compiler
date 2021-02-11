package domain.tree;

import domain.enums.ExpressionType;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class Expression {
    private Variable var1, var2;
    private ExpressionType type;
    private boolean varWrap;

    private Expression(@NonNull Variable var1, Variable var2, ExpressionType type, boolean varWrap) {
        this.var1 = var1;
        this.var2 = var2;
        this.type = type;
        this.varWrap = varWrap;
    }

    public Expression(Variable variable) {
        this(variable, null, null, true);
    }

    public Expression(Variable var1, Variable var2, ExpressionType type) {
        this(var1, var2, type, false);
    }

    @Override
    public String toString() {
        return "Expression{" +
                "var1=" + var1 +
                ", var2=" + var2 +
                ", type=" + type +
                '}';
    }
}
