package domain.tree;

import domain.enums.ConditionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Condition {
    private final ConditionType type;
    private Variable var1, var2;

    public Condition(Variable var1, Variable var2, ConditionType type) {
        this.var1 = var1;
        this.var2 = var2;
        this.type = type;
    }

    public Condition copy() {
        return new Condition(var1.copy(), var2.copy(), type);
    }
}
