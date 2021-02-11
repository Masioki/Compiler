package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.NonNull;

import java.util.List;
import java.util.Set;

public interface Command {

    CommandType getCommandType();

    Set<Variable> getConst();

    Set<Variable> getLocal();

    Set<Variable> getAll();

    @NonNull
    Set<Variable> getGlobal();

    void initializationTest(List<Variable> initialized) throws VariableException;
}
