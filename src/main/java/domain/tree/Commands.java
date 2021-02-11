package domain.tree;

import domain.enums.CommandType;
import domain.exceptions.VariableException;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class Commands implements Command {
    private List<Command> commandList;

    public Commands() {
        commandList = new ArrayList<>();
    }

    public void add(Command command) {
        commandList.add(command);
    }

    public boolean isAssigned(Variable var) {
        if (var.isConstant()) return false;

        for (Command c : commandList) {
            switch (c.getCommandType()) {
                case ASSIGN -> {
                    Assign assign = (Assign) c;
                    Variable assignVar = assign.getVar();
                    if (assignVar.equals(var)) return true;
                    if (!assignVar.isArrayVar() && var.isArrayVar())
                        if (var.getIndex().equals(assignVar.getName())) return true;

                    if (assignVar.isArrayVar() && var.isArrayVar()) {
                        if (assignVar.getName().equals(var.getName())) {
                            if (!assignVar.isIndexConstant() || !var.isIndexConstant()) return true;
                        }
                    }
                }
                case READ -> {
                    IOCommand io = (IOCommand) c;
                    Variable assignVar = io.getVar();
                    if (assignVar.equals(var)) return true;
                    if (!assignVar.isArrayVar() && var.isArrayVar())
                        if (var.getIndex().equals(assignVar.getName())) return true;
                    if (assignVar.isArrayVar() && var.isArrayVar()) {
                        if (assignVar.getName().equals(var.getName())) {
                            if (!assignVar.isIndexConstant() || !var.isIndexConstant()) return true;
                        }
                    }
                }
                case FOR -> {
                    ForBlock forBlock = (ForBlock) c;
                    if (forBlock.getCommands().isAssigned(var)) return true;
                }
                case IF -> {
                    IfBlock ifBlock = (IfBlock) c;
                    if (ifBlock.getIfCommands().isAssigned(var)) return true;
                    if (ifBlock.isElse() && ifBlock.getElseCommands().isAssigned(var)) return true;
                }
                case WHILE -> {
                    WhileBlock whileBlock = (WhileBlock) c;
                    if (whileBlock.getCommands().isAssigned(var)) return true;
                }
            }
        }
        return false;
    }


    @Override
    public CommandType getCommandType() {
        return null;
    }

    @Override
    public Set<Variable> getConst() {
        return commandList.stream()
                .map(Command::getConst)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Variable> getLocal() {
        return commandList.stream()
                .map(Command::getLocal)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Variable> getAll() {
        return commandList.stream()
                .map(Command::getAll)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Variable> getGlobal() {
        try {
            return commandList.stream()
                    .map(Command::getGlobal)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        } catch (NullPointerException e) {
            Set<Set<Variable>> i = commandList.stream()
                    .map(Command::getGlobal).collect(Collectors.toSet());
            if (i.isEmpty()) System.out.println("DUUPA");
            System.out.println(i);
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    @Override
    public void initializationTest(List<Variable> initialized) throws VariableException {
        for (Command c : commandList) c.initializationTest(initialized);
    }
}
