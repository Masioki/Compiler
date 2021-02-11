package domain.tree;

import lombok.Getter;

import java.rmi.AlreadyBoundException;
import java.util.HashSet;
import java.util.Set;

@Getter
public class Declaration {
    private final Set<Variable> vars;
    private final Set<Array> arrays;

    public Declaration() {
        vars = new HashSet<>();
        arrays = new HashSet<>();
    }

    public boolean contains(Variable var) {
        return (var.isArrayVar() && arrays.stream().anyMatch(a -> a.getName().equals(var.getName()) && a.inRange(var.getIndex())))
                || (!var.isConstant() && !var.isArrayVar() && vars.contains(var));
    }

    public void addVar(Variable variable) throws Exception {
        if (contains(variable))
            throw new AlreadyBoundException("Variable already declared: " + variable.getName());
        vars.add(variable);
    }

    public void addArray(Array array) throws Exception {
        if (arrays.contains(array))
            throw new AlreadyBoundException("Variable already declared: " + array.getName());
        arrays.add(array);
    }
}
