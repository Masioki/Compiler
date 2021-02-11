package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Assign implements Command {
    private final Variable var;
    private final Expression expression;

    public Assign(Variable var, Expression expression) {
        this.var = var;
        this.expression = expression;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.ASSIGN;
    }

    @Override
    public Set<Variable> getConst() {
        Set<Variable> result = new HashSet<>();
        if (expression.getVar1().isConstant()) result.add(expression.getVar1());
        if (!expression.isVarWrap() && expression.getVar2().isConstant()) result.add(expression.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getLocal() {
        return new HashSet<>();
    }

    @Override
    public Set<Variable> getAll() {
        Set<Variable> result = new HashSet<>();
        result.add(var);
        result.add(expression.getVar1());
        if (!expression.isVarWrap()) result.add(expression.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getGlobal() {
        return getAll();
    }

    @Override
    public void initializationTest(List<Variable> initialized) throws VariableException {
        if (!expression.getVar1().isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + expression.getVar1());
        if (!expression.isVarWrap() && !expression.getVar2().isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + expression.getVar2());
        initialized.add(var);
    }

    @Override
    public String toString() {
        return "Assign{" +
                "var=" + var +
                ", expression=" + expression +
                '}';
    }
}
