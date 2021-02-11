package domain.compiler;

import domain.enums.CommandType;
import domain.enums.ExpressionType;
import domain.tree.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class Optimizer {
    private static int changes = 0;

    public static synchronized void optimize(Program program) {
        Commands commands = program.getCommands();
        int temp = 0;
        simplifyAssignments(commands);
        removeDeadGlobalAssignments(commands);
        do {
            changes = 0;
            replaceEffectivelyEqual(commands);
            simplifyAssignments(commands);
            removeInLoopConstantAssignments(commands);
            removeDeadGlobalAssignments(commands);
            temp++;
        } while (changes > 0 && temp < 20);
//        removeInLoopConstantAssignments(commands);
    }

    //TODO: deep unnecessary assignments removal
    //TODO:
    private static void removeInLoopConstantAssignments(Commands commands) {
        List<Command> commandList = commands.getCommandList();
        int i = 0;
        while (i < commandList.size()) {
            Command c = commandList.get(i);
            if (c.getCommandType() == CommandType.WHILE) {
                WhileBlock whileBlock = (WhileBlock) c;
                List<Assign> assigns = findAndRemoveNotUsedConstantAssignments(whileBlock.getCommands());
                for (Assign a : assigns) {
                    log.debug("LEVEL UP " + a);
                    changes++;
                }
                if (!assigns.isEmpty()) {
                    if (whileBlock.isDoWhile()) {
                        for (Assign a : assigns) {
                            commandList.add(i, a);
                            i++;
                        }
                    } else {
                        Condition copy = whileBlock.getCondition().copy();
                        Commands ifCommands = new Commands();
                        assigns.forEach(ifCommands::add);
                        IfBlock ifBlock = new IfBlock(copy, ifCommands);
                        commandList.add(i, ifBlock);
                        i++;
                    }
                }
            }
            i++;
        }
    }

    private static List<Assign> findAndRemoveNotUsedConstantAssignments(Commands commands) {
        List<Command> commandList = commands.getCommandList();
        List<Assign> result = new ArrayList<>();
        int i = 0;
        while (i < commandList.size()) {
            Command c = commandList.get(i);
            if (c.getCommandType() == CommandType.ASSIGN) {
                Assign assign = (Assign) c;
                Expression expression = assign.getExpression();
                Variable assignVar = assign.getVar();
                if (expression.isVarWrap() && expression.getVar1().isConstant()) {
                    Commands newCommands = new Commands();
                    List<Command> subList = commandList.subList(i + 1, commandList.size());
                    newCommands.setCommandList(subList);
                    if (!isUsedIn(assignVar, newCommands.getAll())) {
                        result.add(assign);
                        commandList.remove(i);
                        i--;
                    }
                }
            }
            i++;
        }
        return result;
    }

    /*
    DEAD GLOBAL ASSIGNMENTS REMOVAL
     */
    private static void removeDeadGlobalAssignments(Commands commands) {
        List<Command> commandList = commands.getCommandList();
        int i = 0;
        while (i < commandList.size()) {
            Command c = commandList.get(i);
            if (c.getCommandType() == CommandType.ASSIGN) {
                Assign assign = (Assign) c;
                Variable assignVar = assign.getVar();
                Commands newCommands = new Commands();
                List<Command> subList = commandList.subList(i + 1, commandList.size());
                newCommands.setCommandList(subList);
                if (!isUsedBeforeAssignment(assignVar, newCommands)) {
                    log.debug("REMOVE " + assign);
                    changes++;
                    commandList.remove(i);
                    i--;
                }
            }
            i++;
        }
    }

    private static boolean isUsedBeforeAssignment(Variable var, Commands commands) {
        for (Command c : commands.getCommandList()) {
            if (c.getCommandType() == CommandType.ASSIGN) {
                Assign assign = (Assign) c;
                Expression expression = assign.getExpression();
                Variable v = assign.getVar();

                if (isUsedIn(var, expression.getVar1())) return true;
                if (!expression.isVarWrap())
                    if (isUsedIn(var, expression.getVar2())) return true;

                if (var.equals(v)) return false;
            }
            if (c.getCommandType() == CommandType.READ) {
                IOCommand ioCommand = (IOCommand) c;
                Variable v = ioCommand.getVar();
                //czy jest przypisywana
                if (var.equals(v)) return false;
                if (isUsedIn(var, v)) return true;
            }
            if (isUsedIn(var, c.getAll())) return true;
        }
        return false;
    }

    private static boolean isUsedIn(Variable var, Set<Variable> all) {
        return all.stream().anyMatch(v -> isUsedIn(var, v));
    }

    private static boolean isUsedIn(Variable var, Variable in) {
        if (isAssignedIn(var, in)) return true;
        return !var.isArrayVar() && in.getIndex().equals(var.getName());
    }


    /*
    SIMPLIFICATION OF EXPRESSIONS
     */
    private static void simplifyAssignments(Commands commands) {
        List<Command> commandList = commands.getCommandList();
        for (Command c : commandList) {
            switch (c.getCommandType()) {
                case ASSIGN -> {
                    Assign assign = (Assign) c;
                    Expression expression = assign.getExpression();
                    if (!expression.isVarWrap()) {
                        if (expression.getVar1().isConstant() && expression.getVar2().isConstant()) {
                            BigInteger b1 = new BigInteger(expression.getVar1().getName());
                            BigInteger b2 = new BigInteger(expression.getVar2().getName());
                            b1 = switch (expression.getType()) {
                                case TIMES -> b1.multiply(b2);
                                case PLUS -> b1.add(b2);
                                case MINUS -> b1.subtract(b2);
                                case DIVIDE -> b2.equals(BigInteger.ZERO) ? BigInteger.ZERO : b1.divide(b2);
                                case MOD -> b2.equals(BigInteger.ZERO) ? BigInteger.ZERO : b1.mod(b2);
                            };
                            if (b1.compareTo(BigInteger.ZERO) < 0) b1 = BigInteger.ZERO;
                            copyExpression(expression, new Expression(new Variable(b1.longValue())));
                        } else if (expression.getVar1().equals(expression.getVar2())) {
                            switch (expression.getType()) {
                                case MOD, MINUS -> copyExpression(expression, new Expression(new Variable(0)));
//                                case DIVIDE -> copyExpression(expression, new Expression(new Variable(1)));
                                case PLUS -> copyExpression(expression, new Expression(new Variable(2), expression.getVar1(), ExpressionType.TIMES));
                            }
                        }
                    }
                    if (!expression.isVarWrap() && (expression.getVar1().isConstant() || expression.getVar2().isConstant())) {
                        Variable cons = expression.getVar1().isConstant() ? expression.getVar1() : expression.getVar2();
                        Variable notCons = expression.getVar1().isConstant() ? expression.getVar2() : expression.getVar1();
                        long consVal = Long.parseLong(cons.getName());
                        if (consVal == 1) {
                            if (cons.equals(expression.getVar1())) {
                                if (expression.getType() == ExpressionType.TIMES)
                                    copyExpression(expression, new Expression(notCons));
                            } else {
                                switch (expression.getType()) {
                                    case TIMES, DIVIDE -> copyExpression(expression, new Expression(notCons));
                                    case MOD -> copyExpression(expression, new Expression(new Variable(0)));
                                }
                            }
                        } else if (consVal == 0) {
                            switch (expression.getType()) {
                                case PLUS, MINUS -> copyExpression(expression, new Expression(notCons));
                                case MOD, DIVIDE, TIMES -> copyExpression(expression, new Expression(new Variable(0)));
                            }
                        }
                    }
                }
                case WHILE -> simplifyAssignments(((WhileBlock) c).getCommands());
                case IF -> {
                    IfBlock ifBlock = (IfBlock) c;
                    simplifyAssignments(ifBlock.getIfCommands());
                    if (ifBlock.isElse()) simplifyAssignments(ifBlock.getElseCommands());
                }
                case FOR -> simplifyAssignments(((ForBlock) c).getCommands());
            }
        }
    }

    private static void copyExpression(Expression to, Expression from) {
        log.debug("FROM " + from + " TO " + to);
        changes++;
        to.setVar1(from.getVar1());
        to.setVar2(from.getVar2());
        to.setVarWrap(from.isVarWrap());
        to.setType(from.getType());
    }


    /*
    REPLACING EFFECTIVELY EQUAL VARIABLES
     */
    private static void replaceEffectivelyEqual(Commands commands) {
        List<Command> commandList = commands.getCommandList();
        int i = 0;
        while (i < commandList.size()) {
            Command c = commandList.get(i);
            if (c.getCommandType() == CommandType.ASSIGN) {
                Assign assign = (Assign) c;
                Expression expression = assign.getExpression();
                if (expression.isVarWrap()) {
//                    if (expression.getVar1().isIndexConstant() && assign.getVar().isIndexConstant()) {
                    if (!(expression.getVar1().isArrayVar() && !assign.getVar().isArrayVar())) {
                        Commands newCommands = new Commands();
                        List<Command> subList = commandList.subList(i + 1, commandList.size());
                        newCommands.setCommandList(subList);
                        replaceWithEqual(assign.getVar(), expression.getVar1(), newCommands);
                    }
//                    }
                }
            }
            i++;
        }
    }

    /**
     * @return true if new assignment found
     */
    private static boolean replaceWithEqual(Variable var, Variable repl, Commands commands) {
        List<Command> commandList = commands.getCommandList();

        for (Command command : commandList) {
            switch (command.getCommandType()) {
                case WRITE -> replaceEqualVar(((IOCommand) command).getVar(), var, repl, false);
                case READ -> {
                    if (isAssignedIn(var, ((IOCommand) command).getVar())
                            || isAssignedIn(repl, ((IOCommand) command).getVar())) return true;
                }
                case ASSIGN -> {
                    Assign assign = (Assign) command;
                    Expression expression = assign.getExpression();
                    Variable assignVar = assign.getVar();

                    replaceEqualVar(expression.getVar1(), var, repl, false);
                    if (!expression.isVarWrap()) replaceEqualVar(expression.getVar2(), var, repl, false);
                    replaceEqualVar(assignVar, var, repl, true);
                    if (isAssignedIn(var, assignVar) || isAssignedIn(repl, assignVar)) return true;
                }
                case IF -> {
                    IfBlock ifBlock = (IfBlock) command;
                    Condition condition = ifBlock.getCondition();
                    replaceEffectivelyEqual(ifBlock.getIfCommands());
                    if (ifBlock.isElse()) replaceEffectivelyEqual(ifBlock.getElseCommands());

                    replaceEqualVar(condition.getVar1(), var, repl, false);
                    replaceEqualVar(condition.getVar2(), var, repl, false);

                    boolean result = false;
                    if (replaceWithEqual(var, repl, ifBlock.getIfCommands())) result = true;
                    if (ifBlock.isElse() && replaceWithEqual(var, repl, ifBlock.getElseCommands())) result = true;
                    if (result) return true;
                }
                case WHILE -> {
                    WhileBlock whileBlock = (WhileBlock) command;
                    replaceEffectivelyEqual(whileBlock.getCommands());
                    if (!whileBlock.getCommands().isAssigned(var) && !whileBlock.getCommands().isAssigned(repl)) {
                        replaceWithEqual(var, repl, whileBlock.getCommands());
                        Condition condition = whileBlock.getCondition();
                        replaceEqualVar(condition.getVar1(), var, repl, false);
                        replaceEqualVar(condition.getVar2(), var, repl, false);
                    } else return true;
                }
                case FOR -> {
                    ForBlock forBlock = (ForBlock) command;
                    replaceEffectivelyEqual(forBlock.getCommands());
                    replaceEqualVar(forBlock.getMin(), var, repl, false);
                    replaceEqualVar(forBlock.getMax(), var, repl, false);
                    if (!forBlock.getCommands().isAssigned(var) && !forBlock.getCommands().isAssigned(repl))
                        replaceWithEqual(var, repl, forBlock.getCommands());
                    else return true;
                }
            }
        }
        return false;
    }


    private static void replaceEqualVar(Variable toReplace, Variable var, Variable repl, boolean onlyIndex) {
        boolean isVarArray = var.isArrayVar();
        boolean isReplArray = repl.isArrayVar();
        if (!onlyIndex && toReplace.equals(var)) {
            log.debug("FROM " + repl + " TO " + toReplace);
            changes++;
            toReplace.setName(repl.getName());
            toReplace.setIndex(repl.getIndex());
            toReplace.setConstant(repl.isConstant());
        } else if (toReplace.isArrayVar()) {
            if (!isVarArray && !isReplArray && toReplace.getIndex().equals(var.getName())) {
                log.debug("FROM " + repl.getName() + " TO INDEX OF " + toReplace);
                changes++;
                toReplace.setIndex(repl.getName());
            }
        }
    }

    /**
     * a    a    true <br>
     * t(a) t(b) true <br>
     * t(1) t(b) true <br>
     * t(a) t(1) true <br>
     * t(1) t(1) true <br>
     * t(a) a    true <br>
     * <p>
     * a    t(a) false <br>
     * a    b    false <br>
     * t(1) t(2) false <br>
     * t(1) x(1) false
     */
    public static boolean isAssignedIn(Variable var, Variable in) {
        if (in.equals(var)) return true;
        if (!in.isArrayVar()) return var.isArrayVar() && var.getIndex().equals(in.getName());
        else {
            if (var.isArrayVar() && in.getName().equals(var.getName()))
                return !(in.isIndexConstant() && var.isIndexConstant());
        }
        return false;
    }
}
