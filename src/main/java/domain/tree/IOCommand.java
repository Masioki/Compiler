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
public class IOCommand implements Command {
    private final CommandType type;
    private Variable var;

    public IOCommand(Variable var, CommandType type) {
        this.var = var;
        this.type = type;
    }

    @Override
    public CommandType getCommandType() {
        return type;
    }

    @Override
    public Set<Variable> getConst() {
        Set<Variable> res = new HashSet<>();
        if (var.isConstant()) res.add(var);
        return res;
    }

    @Override
    public Set<Variable> getLocal() {
        return new HashSet<>();
    }

    @Override
    public Set<Variable> getAll() {
        Set<Variable> res = new HashSet<>();
        res.add(var);
        return res;
    }

    @Override
    public Set<Variable> getGlobal() {
        return getAll();
    }

    @Override
    public void initializationTest(List<Variable> initialized) throws VariableException {
        if (getCommandType() == CommandType.WRITE) {
            if (!var.isInitialized(initialized))
                throw new VariableException("Variable not initialized: " + var);
        } else {
            initialized.add(var);
        }
    }
}
