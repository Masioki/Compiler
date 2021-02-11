package domain.tree;

import domain.compiler.MemoryManager;
import domain.compiler.Optimizer;
import domain.exceptions.VariableException;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class Program {
    private final Commands commands;
    private final Declaration declaration;

    public Program(@NonNull Commands commands) {
        this.commands = commands;
        declaration = new Declaration();
        try {
            checkVariables();
            commands.initializationTest(new ArrayList<>());
            Optimizer.optimize(this);
            new MemoryManager().toAssembly(this);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public Program(@NonNull Declaration declaration, @NonNull Commands commands) {
        this.commands = commands;
        this.declaration = declaration;
        try {
            checkVariables();
            commands.initializationTest(new ArrayList<>());
            Optimizer.optimize(this);
            new MemoryManager().toAssembly(this);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void checkVariables() throws VariableException {
        Set<Variable> all = commands.getAll();
        Set<Variable> local = commands.getLocal();
        Set<Variable> global = commands.getGlobal();

        Set<Variable> tempGlob = new HashSet<>(global);
        tempGlob.retainAll(local);
        if (!tempGlob.isEmpty())
            throw new VariableException("Local variables used outside their scope: " + tempGlob.stream().map(Variable::getName).collect(Collectors.joining(", ")));

        if (local.stream().anyMatch(declaration::contains))
            throw new VariableException("Global and local variable with same name");

        all.removeAll(local);
        all.removeIf(Variable::isConstant);
        Optional<Variable> opt = all.stream().filter(v -> !declaration.contains(v)).findFirst();
        if (opt.isPresent())
            throw new VariableException("Incorrect variable usage " + opt.get().getName());
    }
}
