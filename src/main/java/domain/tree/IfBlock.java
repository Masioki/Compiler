package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class IfBlock implements Command {
    private final Condition condition;
    private final Commands ifCommands, elseCommands;
    private final boolean isElse;

    public IfBlock(Condition condition, Commands ifCommands, Commands elseCommands, boolean isElse) {
        this.condition = condition;
        this.ifCommands = ifCommands == null ? new Commands() : ifCommands;
        this.elseCommands = elseCommands == null ? new Commands() : elseCommands;
        this.isElse = isElse;
    }

    public IfBlock(Condition condition, Commands ifCommands) {
        this(condition, ifCommands, new Commands(), false);
    }

    public IfBlock(Condition condition, Commands ifCommands, Commands elseCommands) {
        this(condition, ifCommands, elseCommands, true);
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.IF;
    }

    @Override
    public Set<Variable> getConst() {
        Set<Variable> result = ifCommands.getConst();
        if (isElse) result.addAll(elseCommands.getConst());
        if (condition.getVar1().isConstant()) result.add(condition.getVar1());
        if (condition.getVar2().isConstant()) result.add(condition.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getLocal() {
        Set<Variable> result = ifCommands.getLocal();
        if (isElse) result.addAll(elseCommands.getLocal());
        return result;
    }

    @Override
    public Set<Variable> getAll() {
        Set<Variable> result = ifCommands.getAll();
        if (isElse) result.addAll(elseCommands.getAll());
        result.add(condition.getVar1());
        result.add(condition.getVar2());
        return result;
    }

    @Override
    public Set<Variable> getGlobal() {
        Set<Variable> result = ifCommands.getGlobal();
        if (isElse) result.addAll(elseCommands.getGlobal());
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

        ifCommands.initializationTest(new ArrayList<>(initialized));
        if (isElse()) elseCommands.initializationTest(new ArrayList<>(initialized));
    }
}
