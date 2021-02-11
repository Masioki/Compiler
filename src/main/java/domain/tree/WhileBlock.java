package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Getter
public class WhileBlock implements Command {
    private final Condition condition;
    private final Commands commands;
    private final boolean doWhile;

    public WhileBlock(Condition condition, Commands commands, boolean doWhile) {
        this.condition = condition;
        this.commands = commands == null ? new Commands() : commands;
        this.doWhile = doWhile;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.WHILE;
    }

    @Override
    public Set<Variable> getConst() {
        Set<Variable> result = commands.getConst();
        if (condition.getVar1().isConstant()) result.add(condition.getVar1());
        if (condition.getVar2().isConstant()) result.add(condition.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getLocal() {
        return commands.getLocal();
    }

    @Override
    public Set<Variable> getAll() {
        Set<Variable> result = commands.getAll();
        result.add(condition.getVar1());
        result.add(condition.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getGlobal() {
        Set<Variable> result = commands.getGlobal();
        result.add(condition.getVar1());
        result.add(condition.getVar2());
        return result;
    }

    @Override
    public void initializationTest(List<Variable> initialized) throws VariableException {
        Variable var1 = condition.getVar1();
        Variable var2 = condition.getVar2();
        if (!var1.isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + var1);

        if (!var2.isInitialized(initialized))
            throw new VariableException("Variable not initialized: " + var2);
        commands.initializationTest(initialized);
    }
}
