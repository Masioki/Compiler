package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ForBlock implements Command {
    private final boolean down;
    private final Commands commands;
    private Variable iterVar, min, max;

    public ForBlock(String iter, Variable min, Variable max, boolean down, Commands commands) {
        this.iterVar = new Variable(iter);
        this.min = min;
        this.max = max;
        this.down = down;
        this.commands = commands == null ? new Commands() : commands;
    }


    @Override
    public CommandType getCommandType() {
        return CommandType.FOR;
    }

    @Override
    public Set<Variable> getConst() {
        Set<Variable> result = commands.getConst();
        if (min.isConstant()) result.add(min);
        if (max.isConstant()) result.add(max);
        return result;
    }

    @Override
    public Set<Variable> getLocal() {
        Set<Variable> result = commands.getLocal();
        result.removeIf(v -> v.getName().equals(iterVar.getName()));
        result.add(iterVar);
        return result;
    }

    @Override
    public Set<Variable> getAll() {
        Set<Variable> result = commands.getAll();
        result.add(min);
        result.add(max);
        result.add(iterVar);
        return result;
    }

    @Override
    public Set<Variable> getGlobal() {
        Set<Variable> result = new HashSet<>(commands.getGlobal());
        result.remove(iterVar);
        result.remove(max);
        result.remove(min);
        return result;
    }

    @Override
    public void initializationTest(List<Variable> initialized) throws VariableException {
        if (!min.isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + min);
        if (!max.isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + max);
        initialized.add(iterVar);
        commands.initializationTest(initialized);

        if (initialized.stream().filter(i -> i.equals(iterVar)).count() > 1)
            throw new VariableException("Assignment to loop variable: " + iterVar);

        initialized.removeIf(i -> i.equals(iterVar));
    }
}
