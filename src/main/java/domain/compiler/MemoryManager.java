package domain.compiler;

import domain.enums.ConditionType;
import domain.enums.ExpressionType;
import domain.tree.*;
import jflex.base.Pair;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static domain.enums.ConditionType.*;

/**
 * @author Tomasz Pach
 * @version Alpha
 */
@Getter
public class MemoryManager {
    private static final int LOAD_CONSTANT_AVG_COST = 21;
    public static String OUT_FILE = "";
    private final Map<Variable, Long> variables = new HashMap<>();
    private final Map<Array, Pair<Long, Long>> arrays = new HashMap<>();
    private long memCounter = 0;

    /*
    MAIN
     */
    public void toAssembly(Program program) throws IOException {
//        log.debug("---------- " + OUT_FILE + " -----------------");
        Register register = new Register();
        List<String> result = allocMemory(program, register);
        result.addAll(commands(program.getCommands(), register));
        result.add("HALT");
        String resultString = String.join("\n", result);
        Files.writeString(Paths.get(OUT_FILE), resultString);
    }

    private List<String> allocMemory(Program program, Register register) {
        variables.clear();
        memCounter = 0;
        List<String> result = new ArrayList<>();
        if (!program.getCommands().getConst().isEmpty()) {
            boolean temp = true;
            for (Variable v : program.getCommands().getConst()) {
                if (temp) {
                    result.add("RESET a");
                    temp = false;
                } else result.add("INC a");

                result.add("RESET b");
                List<Integer> factor = factorBinary(Long.parseLong(v.getName()));
                for (int i = 0; i < factor.size(); i++) {
                    if (factor.get(i) == 1) result.add("INC b");
                    if (i < factor.size() - 1) result.add("SHL b");
                }
                result.add("STORE b a");
                variables.put(v, memCounter++);
                register.set("b", v);
            }
        }
        program.getCommands().getLocal().forEach(v -> {
            variables.put(v, memCounter++);
            variables.put(new Variable(v.getName() + "1"), memCounter++);
        });
        program.getDeclaration().getVars().forEach(v -> variables.put(v, memCounter++));

        program.getDeclaration().getArrays().forEach(a -> {
            arrays.put(a, new Pair<>(memCounter, a.getStart()));
            memCounter += a.size();
        });
//        log.debug("MEMORY: " + variables);
//        log.debug("MEMORY-ARRAYS: " + arrays);
        return result;
    }

    private List<String> commands(Commands commands, Register register) {
        List<String> output = new ArrayList<>();
        for (Command c : commands.getCommandList()) {
            switch (c.getCommandType()) {
                case WHILE -> output.addAll(parseWhile((WhileBlock) c, register));
                case FOR -> output.addAll(parseFor((ForBlock) c, register));
                case IF -> output.addAll(parseIf((IfBlock) c, register));
                case ASSIGN -> output.addAll(parseAssign((Assign) c, register));
                case READ -> output.addAll(parseRead((IOCommand) c, register));
                case WRITE -> output.addAll(parseWrite((IOCommand) c, register));
            }
        }
        return output;
    }


    /*
    UTIL
     */
    private List<Integer> factorBinary(long num) {
        List<Integer> result = new ArrayList<>();
        if (num == 0) return result;
        String temp = Long.toBinaryString(num);
        for (int i = 0; i < temp.length(); i++)
            result.add(Integer.parseInt(String.valueOf(temp.charAt(i))));
        return result;
    }

    private int costOfFactoringConstant(Variable var) {
        long val = Long.parseLong(var.getName());
        List<Integer> bin = factorBinary(val);
        int cost = bin.size();
        cost += bin.stream().mapToInt(i -> i).sum();
        return cost;
    }

    private boolean isPowerOfTwo(Variable var) {
        long val = Long.parseLong(var.getName());
        List<Integer> bin = factorBinary(val);
        return bin.stream().mapToInt(i -> i).sum() == 1;
    }

    private int getPowerOfTwo(Variable var) {
        long val = Long.parseLong(var.getName());
        List<Integer> bin = factorBinary(val);
        return bin.size() - 1;
    }


    /*
    MEMORY
     */
    private Pair<String, List<String>> loadPointerTo(Variable variable, Register register, String... notRemovableReg) {
        List<String> result = new ArrayList<>();
        if (register.containsPointer(new Pointer(variable))) {
            return new Pair<>(register.getRegOfPointer(new Pointer(variable)), result);
        }

        String resultReg = register.getReg(notRemovableReg);
        result.add("RESET " + resultReg);
        if (variable.isArrayVar()) {
            String index = variable.getIndex();
            Pair<Long, Long> arrInfo = arrays.entrySet().stream()
                    .filter(e -> e.getKey().getName().equals(variable.getName()))
                    .findFirst().get().getValue();

            if (index.matches("^[0-9]+$")) {
                long temp = Long.parseLong(index) + arrInfo.fst - arrInfo.snd;
                List<Integer> factor = factorBinary(temp);
                for (int i = 0; i < factor.size(); i++) {
                    if (factor.get(i) == 1) result.add("INC " + resultReg);
                    if (i < factor.size() - 1) result.add("SHL " + resultReg);
                }
            } else {
                String[] nr = new String[notRemovableReg.length + 1];
                nr[0] = resultReg;
                System.arraycopy(notRemovableReg, 0, nr, 1, notRemovableReg.length);
                Variable indexVar = new Variable(index);
                Pair<String, List<String>> pointer = loadPointerTo(indexVar, register, nr);
                result.addAll(pointer.snd);
                result.add("LOAD " + pointer.fst + " " + pointer.fst);
                long temp = arrInfo.fst - arrInfo.snd;
                boolean sub = false;
                if (temp < 0) {
                    sub = true;
                    temp = -temp;
                }
                List<Integer> factor = factorBinary(temp);
                for (int i = 0; i < factor.size(); i++) {
                    if (factor.get(i) == 1) result.add("INC " + resultReg);
                    if (i < factor.size() - 1) result.add("SHL " + resultReg);
                }
                if (sub) result.add("SUB " + pointer.fst + " " + resultReg);
                else result.add("ADD " + pointer.fst + " " + resultReg);
                register.clearReg(resultReg);
                resultReg = pointer.fst;
            }
        } else {
            long index = variables.get(variable);
            List<Integer> factor = factorBinary(index);
            for (int i = 0; i < factor.size(); i++) {
                if (factor.get(i) == 1) result.add("INC " + resultReg);
                if (i < factor.size() - 1) result.add("SHL " + resultReg);
            }
        }
        register.set(resultReg, new Pointer(variable));
        return new Pair<>(resultReg, result);
    }

    private List<String> loadConstantToRegister(Variable var, String register, Register registerAll, String... notRemovable) { // EDIT
        List<String> result = new ArrayList<>();
        if (costOfFactoringConstant(var) <= LOAD_CONSTANT_AVG_COST) {
            result.add("RESET " + register);
            List<Integer> factor = factorBinary(Long.parseLong(var.getName()));
            for (int i = 0; i < factor.size(); i++) {
                if (factor.get(i) == 1) result.add("INC " + register);
                if (i < factor.size() - 1) result.add("SHL " + register);
            }
        } else {
            Pair<String, List<String>> pointer = loadPointerTo(var, registerAll, notRemovable);
            result.addAll(pointer.snd);
            result.add("LOAD " + register + " " + pointer.fst);
        }
        registerAll.set(register, var);
        return result;
    }

    private List<String> loadToRegister(Variable var, String register, Register registerAll, String... notRemovable) {
        List<String> result = new ArrayList<>();
        if (registerAll.equals(register, var)) return result;
        if (var.isConstant()) return loadConstantToRegister(var, register, registerAll, notRemovable);
        Pair<String, List<String>> pointer = loadPointerTo(var, registerAll, notRemovable);
        result.addAll(pointer.snd);
        result.add("LOAD " + register + " " + pointer.fst);
        registerAll.set(register, var);
        return result;
    }

    private Pair<String, List<String>> load(Variable var, Register register, String... notRemovable) {
        if (register.containsVar(var))
            return new Pair<>(register.getRegOf(var), new ArrayList<>());
        String reg = register.getReg(notRemovable);
        return new Pair<>(reg, loadToRegister(var, reg, register, notRemovable));
    }

    private List<String> store(String reg, Variable to, Register register, String... notRemovable) {
        String[] nr = new String[notRemovable.length + 1];
        nr[0] = reg;
        System.arraycopy(notRemovable, 0, nr, 1, notRemovable.length);
        Pair<String, List<String>> pointer = loadPointerTo(to, register, nr);
        List<String> result = new ArrayList<>(pointer.snd);
        result.add("STORE " + reg + " " + pointer.fst);
        register.varValueChanged(to);
        register.set(reg, to);
        return result;
    }


    /*
    IO
     */
    private List<String> parseRead(IOCommand io, Register register) {
//        log.debug("READ");
        Pair<String, List<String>> temp = loadPointerTo(io.getVar(), register);
        List<String> result = new ArrayList<>(temp.snd);
        result.add("GET " + temp.fst);
        register.varValueChanged(io.getVar());
        return result;
    }

    private List<String> parseWrite(IOCommand io, Register register) {
//        log.debug("WRITE");
        Pair<String, List<String>> temp = loadPointerTo(io.getVar(), register);
        List<String> result = new ArrayList<>(temp.snd);
        result.add("PUT " + temp.fst);
        return result;
    }


    /*
    ASSIGNMENTS
     */
    private List<String> parseAssign(Assign assign, Register register) {
//        log.debug("ASSIGNMENT");
        Variable to = assign.getVar();
        Expression expression = assign.getExpression();
        Variable var1 = expression.getVar1();
        Variable var2 = expression.getVar2();

        if (expression.isVarWrap()) return parseOneVarAssign(to, expression, register);
        if (var1.isConstant() || var2.isConstant()) return parseAssignWithConst(to, expression, register);
        return parseTwoVarAssign(to, expression, register);
    }

    private List<String> parseOneVarAssign(Variable to, Expression expression, Register register) {
//        log.debug("ASSIGNMENT ONE VAR");
        List<String> result = new ArrayList<>();
        Variable var = expression.getVar1();
        if (var.equals(to)) return result;
        String reg;
        if (register.containsVar(var)) {
            if (register.containsVar(to))
                register.clearReg(register.getRegOf(to));
            reg = register.getRegOf(var);
        } else {
            if (register.containsVar(to)) reg = register.getRegOf(to);
            else reg = register.getReg();
            result.addAll(loadToRegister(var, reg, register));
        }
        result.addAll(store(reg, to, register));
        register.set(reg, to);
        return result;
    }

    private List<String> parseAssignWithConst(Variable to, Expression expression, Register register) {
//        log.debug("ASSIGNMENT WITH CONST");
        Variable var1 = expression.getVar1();
        Variable var2 = expression.getVar2();

        Variable cons = var1.isConstant() ? var1 : var2;
        Variable var = var1.isConstant() ? var2 : var1;

        long consVal = Long.parseLong(cons.getName());
        boolean containsCons = register.containsVar(cons);

        ExpressionType type = expression.getType();
        if (type == ExpressionType.PLUS || type == ExpressionType.MINUS) {
            if ((!containsCons && consVal > 10) || (containsCons && consVal > 5))
                return parseTwoVarAssign(to, expression, register);
            Pair<String, List<String>> p1 = load(var, register);
            List<String> result = new ArrayList<>(p1.snd);
            String r1 = p1.fst;
            switch (type) {
                case PLUS -> {
                    for (long i = 0; i < consVal; i++)
                        result.add("INC " + r1);
                    register.clearReg(r1);
                    result.addAll(store(r1, to, register));
                }
                case MINUS -> {
                    for (long i = 0; i < consVal; i++)
                        result.add("DEC " + r1);
                    register.clearReg(r1);
                    result.addAll(store(r1, to, register));
                }
            }
            return result;
        } else if (isPowerOfTwo(cons)) {
            int power = getPowerOfTwo(cons);
            if (type == ExpressionType.TIMES) {
                Pair<String, List<String>> p1 = load(var, register);
                List<String> result = new ArrayList<>(p1.snd);
                String r1 = p1.fst;
                for (int i = 0; i < power; i++)
                    result.add("SHL " + r1);
                register.clearReg(r1);
                result.addAll(store(r1, to, register));
                return result;
            } else if (cons.equals(var2) && type == ExpressionType.DIVIDE) {
                Pair<String, List<String>> p1 = load(var, register);
                List<String> result = new ArrayList<>(p1.snd);
                String r1 = p1.fst;
                for (int i = 0; i < power; i++)
                    result.add("SHR " + r1);
                register.clearReg(r1);
                result.addAll(store(r1, to, register));
                return result;
            } else if (power == 1 && type == ExpressionType.MOD && cons.equals(var2)) { // czy parzyste k % 2
                Pair<String, List<String>> p1 = load(var, register);
                List<String> result = new ArrayList<>(p1.snd);
                String r1 = p1.fst;
                result.add("JODD " + r1 + " 3");
                result.add("RESET " + r1);
                result.add("JUMP 3");
                result.add("RESET " + r1);
                result.add("INC " + r1);
                register.clearReg(r1);
                result.addAll(store(r1, to, register));
                return result;
            }
        }
        return parseTwoVarAssign(to, expression, register);
    }

    private List<String> parseTwoVarAssign(Variable to, Expression expression, Register register) {
//        log.debug("ASSIGNMENT TWO VAR");
        Variable var1 = expression.getVar1();
        Variable var2 = expression.getVar2();

        String r1, r2, r3;
        Pair<String, List<String>> p2 = load(var1, register);
        List<String> result = new ArrayList<>(p2.snd);
        r2 = p2.fst;
        if (var1.equals(var2)) {
            r3 = register.getReg(r2);
            result.addAll(loadToRegister(var2, r3, register, r2));
        } else {
            Pair<String, List<String>> p3 = load(var2, register, r2);
            result.addAll(p3.snd);
            r3 = p3.fst;
        }
        r1 = register.getReg(r2, r3);

        switch (expression.getType()) {
            case PLUS -> {
                result.add("ADD " + r2 + " " + r3);
                result.addAll(store(r2, to, register));
            }
            case MINUS -> {
                result.add("SUB " + r2 + " " + r3);
                result.addAll(store(r2, to, register));
            }
            case TIMES -> {
                // r1-result, r2-var1, r3-var2
                result.addAll(loadToRegister(var1, r1, register, r2, r3));
                result.add("SUB " + r1 + " " + r3);
                result.add("JZERO " + r1 + " 10");

                // a>= b
                result.add("RESET " + r1);
                result.add("JZERO " + r3 + " 7");
                result.add("JODD " + r3 + " 2");
                result.add("JUMP 2");
                result.add("ADD " + r1 + " " + r2);
                result.add("SHR " + r3);
                result.add("SHL " + r2);
                result.add("JUMP -6");

                result.add("JUMP 9");

                // a <= b
                result.add("RESET " + r1);
                result.add("JZERO " + r2 + " 7");
                result.add("JODD " + r2 + " 2");
                result.add("JUMP 2");
                result.add("ADD " + r1 + " " + r3);
                result.add("SHR " + r2);
                result.add("SHL " + r3);
                result.add("JUMP -6");

                register.clearReg(r2);
                register.clearReg(r3);
                result.addAll(store(r1, to, register));
            }
            case DIVIDE -> {
                List<String> tempresult = new ArrayList<>();
                // var1 / var2
                //r1 - mul, r2-var1, r3-var2, pnt-pointer to var1, r4 - var1
                Pair<String, List<String>> pnt = loadPointerTo(var1, register, r1, r2, r3);
                tempresult.addAll(pnt.snd);
                String r4 = register.getReg(r1, r2, r3, pnt.fst);
                tempresult.add("RESET " + r1);
                tempresult.add("INC " + r1);  // mul = 1
                tempresult.add("LOAD " + r4 + " " + pnt.fst); // r4 = var1
                tempresult.add("SUB " + r4 + " " + r3); // while (var2 < var1 )
                tempresult.add("JZERO " + r4 + " 2");
                tempresult.add("JUMP 2");
                tempresult.add("JUMP 4"); // wyjscie z while
                tempresult.add("SHL " + r3);
                tempresult.add("SHL " + r1);
                tempresult.add("JUMP -7");

                pnt = loadPointerTo(to, register, r1, r2, r3, r4);
                tempresult.addAll(pnt.snd);
                tempresult.add("RESET " + r4);

                // r1 - mul, r2 - remainder=var2, r3-divisor, pnt - pointer to 'to', r4 - result, r5 -temp_divisor
                tempresult.addAll(store(r3, to, register, r1, r2, r4)); // to = divisor
                String r5 = register.getReg(r1, r2, r3, r4, pnt.fst);
                // do
                tempresult.add("LOAD " + r5 + " " + pnt.fst);
                tempresult.add("SUB " + r5 + " " + r2);
                tempresult.add("JZERO " + r5 + " 2");
                tempresult.add("JUMP 3");
                // if (remainder >= divisor)
                tempresult.add("SUB " + r2 + " " + r3); // remainder -= divisor
                tempresult.add("ADD " + r4 + " " + r1); // result += mul
                // endif
                tempresult.add("SHR " + r3); // divisor >> 1
                tempresult.add("SHR " + r1); // mul >> 1

                tempresult.add("STORE " + r3 + " " + pnt.fst);
                tempresult.add("JZERO " + r1 + " 2");
                tempresult.add("JUMP -10");
                // while (mul != 0)


                result.add("JZERO " + r3 + " " + (tempresult.size() + 2)); // dzielenie przez 0
                result.addAll(tempresult);
                result.add("JUMP 2");
                result.add("RESET " + r4);

                register.clearReg(r1);
                register.clearReg(r2);
                register.clearReg(r3);
                register.clearReg(r4);
                register.clearReg(r5);
                result.addAll(store(r4, to, register));
            }
            case MOD -> {
                List<String> tempresult = new ArrayList<>();
                // var1 / var2
                //r1 - mul, r2-var1, r3-var2, pnt-pointer to var1, r4 - var1
                Pair<String, List<String>> pnt = loadPointerTo(var1, register, r1, r2, r3);
                tempresult.addAll(pnt.snd);
                String r4 = register.getReg(r1, r2, r3, pnt.fst);
                tempresult.add("RESET " + r1);
                tempresult.add("INC " + r1);  // mul = 1
                tempresult.add("LOAD " + r4 + " " + pnt.fst); // r4 = var1
                tempresult.add("SUB " + r4 + " " + r3); // while (var2 < var1 )
                tempresult.add("JZERO " + r4 + " 2");
                tempresult.add("JUMP 2");
                tempresult.add("JUMP 4"); // wyjscie z while
                tempresult.add("SHL " + r3);
                tempresult.add("SHL " + r1);
                tempresult.add("JUMP -7");

                pnt = loadPointerTo(to, register, r1, r2, r3, r4);
                tempresult.addAll(pnt.snd);
                tempresult.add("RESET " + r4);

                // r1 - mul, r2 - remainder=var2, r3-divisor, pnt - pointer to 'to', r4 - result, r5 -temp_divisor
                tempresult.addAll(store(r3, to, register, r1, r2, r4)); // to = divisor
                String r5 = register.getReg(r1, r2, r3, r4, pnt.fst);
                // do
                tempresult.add("LOAD " + r5 + " " + pnt.fst);
                tempresult.add("SUB " + r5 + " " + r2);
                tempresult.add("JZERO " + r5 + " 2");
                tempresult.add("JUMP 3");
                // if (remainder >= divisor)
                tempresult.add("SUB " + r2 + " " + r3); // remainder -= divisor
                tempresult.add("ADD " + r4 + " " + r1); // result += mul
                // endif
                tempresult.add("SHR " + r3); // divisor >> 1
                tempresult.add("SHR " + r1); // mul >> 1

                tempresult.add("STORE " + r3 + " " + pnt.fst);
                tempresult.add("JZERO " + r1 + " 2");
                tempresult.add("JUMP -10");
                // while (mul != 0)


                result.add("JZERO " + r3 + " " + (tempresult.size() + 2)); // dzielenie przez 0
                result.addAll(tempresult);
                result.add("JUMP 2");
                result.add("RESET " + r2);

                register.clearReg(r1);
                register.clearReg(r2);
                register.clearReg(r3);
                register.clearReg(r4);
                register.clearReg(r5);
                result.addAll(store(r2, to, register));
            }
        }
        return result;
    }


    /*
    CONDITIONAL
     */

    /**
     * @return 0-TRUE, 1-FALSE on returned register
     */
    private Pair<String, List<String>> condition(Condition condition, Register register, String... notRemovable) {
//        log.debug("CONDITION");
        List<String> result = new ArrayList<>();

        Variable var1 = condition.getVar1();
        Variable var2 = condition.getVar2();

        ConditionType type = condition.getType();
        if (List.of(G, GE).contains(type)) {
            Variable temp = var1;
            var1 = var2;
            var2 = temp;
        }

        Pair<String, List<String>> p1 = load(var1, register);
        Pair<String, List<String>> p2 = load(var2, register, p1.fst);
        result.addAll(p1.snd);
        result.addAll(p2.snd);

        String r1 = p1.fst;
        String r2 = p2.fst;
        String r3 = register.getReg(r1, r2);

        if (type == E || type == NE) {
            result.addAll(loadToRegister(var2, r3, register, r1, r2));
            register.clearReg(r3);
            register.clearReg(r1);
            result.add("SUB " + r3 + " " + r1);
            result.add("SUB " + r1 + " " + r2);
            if (type == E) {
                result.add("JZERO " + r1 + " 2");
                result.add("INC " + r3);
            } else {
                result.add("JZERO " + r1 + " 3");
                result.add("RESET " + r3);
                result.add("JUMP 6");
                result.add("JZERO " + r3 + " 2");
                result.add("JUMP 3");
                result.add("INC " + r3);
                result.add("JUMP 2");
                result.add("RESET " + r3);
            }
            return new Pair<>(r3, result);
        } else if (type == GE || type == LE) {
            result.add("SUB " + r1 + " " + r2);
            register.clearReg(r1);
            return new Pair<>(r1, result);
        } else {
            result.add("RESET " + r3);
            result.add("SUB " + r2 + " " + r1);
            result.add("JZERO " + r2 + " 2");
            result.add("JUMP 2");
            result.add("INC " + r3);
            register.clearReg(r2);
            register.clearReg(r3);
            return new Pair<>(r3, result);
        }
    }


    /*
   IF
    */
    private List<String> parseIf(IfBlock ifBlock, Register register) {
//        log.debug("IF");
        Pair<String, List<String>> cond = condition(ifBlock.getCondition(), register);
        List<String> result = new ArrayList<>(cond.snd);

        Register c1 = register.copy();
        Register c2 = register.copy();

        List<String> ifCommands = commands(ifBlock.getIfCommands(), c1);
        if (ifBlock.isElse()) {
            List<String> elseCommands = commands(ifBlock.getElseCommands(), c2);

            result.add("JZERO " + cond.fst + " 2");
            result.add("JUMP " + (ifCommands.size() + 2));
            result.addAll(ifCommands);
            result.add("JUMP " + (elseCommands.size() + 1));
            result.addAll(elseCommands);
        } else {
            result.add("JZERO " + cond.fst + " 2");
            result.add("JUMP " + (ifCommands.size() + 1));
            result.addAll(ifCommands);
        }

        Register common = Register.getCommon(c1, c2);
        register.setRegisters(common.getRegisters());
//        register.clear();
        return result;
    }


    /*
    WHILE
     */
    private List<String> parseWhile(WhileBlock whileBlock, Register register) {
//        log.debug("WHILE");
        List<String> result = new ArrayList<>();
        register.clear();
        //TODO: register management in loop
//        Register register = registerAll.copy(); // EDIT
//        Register copy = registerAll.copy();  // EDIT

        if (whileBlock.isDoWhile()) {
            List<String> comm = commands(whileBlock.getCommands(), register);
            result.addAll(comm);
            Pair<String, List<String>> cond = condition(whileBlock.getCondition(), register);
            result.addAll(cond.snd);
            result.add("JZERO " + cond.fst + " 2");
        } else {
            Pair<String, List<String>> cond = condition(whileBlock.getCondition(), register);
            result.addAll(cond.snd);
            List<String> comm = commands(whileBlock.getCommands(), register);
            result.add("JZERO " + cond.fst + " 2");
            result.add("JUMP " + (comm.size() + 2));
            result.addAll(comm);
        }
        result.add("JUMP -" + result.size());
//        registerAll.setRegisters(Register.getCommon(register, copy).getRegisters()); // EDIT
        register.clear();
        return result;
    }


    /*
    FOR
     */
    private List<String> parseFor(ForBlock forBlock, Register register) {
//        log.debug("FOR");
        List<String> result = new ArrayList<>();
        Commands commands = forBlock.getCommands();
        Variable iter = forBlock.getIterVar();
        Variable bound = new Variable(iter.getName() + "1");
        Variable min = forBlock.getMin();
        Variable max = forBlock.getMax();

        String r1 = register.getReg();
        String r2 = register.getReg(r1);

        result.addAll(loadToRegister(min, r1, register));
        result.addAll(store(r1, iter, register));
        result.addAll(loadToRegister(max, r2, register, r1));
        if (forBlock.isDown()) {
            result.add("INC " + r1);
            result.add("SUB " + r1 + " " + r2);
            register.clearReg(r1);
//            result.add("INC " + r1);
            result.addAll(store(r1, bound, register));
            register.clear();
            result.add("JZERO " + r1 + " 2");
            result.add("JUMP 2");

            List<String> temp = new ArrayList<>();
            temp.addAll(commands(commands, register));

            Pair<String, List<String>> loadIter = load(iter, register);
            temp.addAll(loadIter.snd);
            temp.add("DEC " + loadIter.fst);
            register.clearReg(loadIter.fst);
            temp.addAll(store(loadIter.fst, iter, register));
            temp.addAll(loadToRegister(bound, r1, register));
            temp.add("DEC " + r1);
            register.clearReg(r1);
            temp.addAll(store(r1, bound, register));
            temp.add("JUMP -" + (temp.size() + 3));

            result.add("JUMP " + (temp.size() + 1));
            result.addAll(temp);
        } else {
            result.add("INC " + r2);
            result.add("SUB " + r2 + " " + r1);
            register.clearReg(r2);
//            result.add("INC " + r2);
            result.addAll(store(r2, bound, register));
            register.clear();
            result.add("JZERO " + r2 + " 2");
            result.add("JUMP 2");

            List<String> temp = new ArrayList<>();
            temp.addAll(commands(commands, register));

            Pair<String, List<String>> loadIter = load(iter, register);
            temp.addAll(loadIter.snd);
            temp.add("INC " + loadIter.fst);
            register.clearReg(loadIter.fst);
            temp.addAll(store(loadIter.fst, iter, register));
            temp.addAll(loadToRegister(bound, r2, register));
            temp.add("DEC " + r2);
            register.clearReg(r2);
            temp.addAll(store(r2, bound, register));
            temp.add("JUMP -" + (temp.size() + 3));

            result.add("JUMP " + (temp.size() + 1));
            result.addAll(temp);
        }
        return result;
    }
}

