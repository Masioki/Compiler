package domain.compiler;

import domain.tree.Variable;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class Register {
    private final LinkedList<String> usageList = new LinkedList<>();
    private Map<String, Variable> registers = getMap();

    private static Map<String, Variable> getMap() {
        Map<String, Variable> result = new HashMap<>();
        result.put("a", null);
        result.put("b", null);
        result.put("c", null);
        result.put("d", null);
        result.put("e", null);
        result.put("f", null);
        return result;
    }

    //TODO: those pointers!!!

    private static boolean varsEqual(Variable v1, Variable v2) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null || v2 == null) return false;
        if (v1.equals(v2)) {
            if ((v1 instanceof Pointer) && (v2 instanceof Pointer)) return true;
            return !(v1 instanceof Pointer) && !(v2 instanceof Pointer);
        }
        return false;
    }

    public static Register getCommon(Register r1, Register r2) {
        Register result = new Register();

        for (Map.Entry<String, Variable> e1 : r1.registers.entrySet()) {
            for (Map.Entry<String, Variable> e2 : r2.registers.entrySet()) {
                if (e1.getKey().equals(e2.getKey())) {
                    if (varsEqual(e1.getValue(), e2.getValue()))
                        result.registers.put(e1.getKey(), e1.getValue());
                }
            }
        }
        return result;
    }

    private List<String> toList(String... s) {
        return Arrays.stream(s).collect(Collectors.toList());
    }

    private void addToUsageList(String reg) {
        usageList.remove(reg);
        usageList.addLast(reg);
    }

    public Register copy() {
        Register register = new Register();
        register.setRegisters(new HashMap<>(this.registers));
        return register;
    }

    public void clearReg(String reg) {
        registers.put(reg, null);
    }

    public void clear() {
        registers = getMap();
    }

    public boolean containsVar(Variable var) {
//        return registers.containsValue(var);
        return registers.values().stream()
                .filter(v -> !(v instanceof Pointer))
                .anyMatch(var::equals);
    }

    public boolean containsPointer(Pointer pointer) {
        return registers.values().stream()
                .filter(v -> v instanceof Pointer)
                .anyMatch(v -> v.equals(pointer));
    }

    public String getRegOfPointer(Pointer pointer) {
        String result = registers.entrySet().stream()
                .filter(e -> e.getValue() instanceof Pointer)
                .filter(e -> e.getValue().equals(pointer))
                .findFirst().get().getKey();
        addToUsageList(result);
        return result;
    }


    public String getReg(String... notRemovable) {
        List<String> notRem = toList(notRemovable);

        Optional<Map.Entry<String, Variable>> free = registers.entrySet().stream()
                .filter(e -> !notRem.contains(e.getKey()))
                .filter(e -> null == e.getValue())
                .findFirst();

        String result;
        if (free.isPresent()) {
            result = free.get().getKey();
//            addToUsageList(result);
            return result;
        }

        Optional<String> used = usageList.stream()
                .filter(e -> !notRem.contains(e))
                .findFirst();
        result = used.orElseGet(() -> registers.entrySet().stream()
                .filter(e -> !notRem.contains(e.getKey()))
                .findFirst().get().getKey());
        addToUsageList(result);
        return result;
    }

    public String getRegOf(Variable var) {
        String result = registers.entrySet().stream()
                .filter(e -> !(e.getValue() instanceof Pointer))
                .filter(e -> var.equals(e.getValue()))
                .findFirst().get().getKey();
        addToUsageList(result);
        return result;
    }

    public void varValueChanged(Variable var) {
        List<String> toReset = new ArrayList<>();
        for (Map.Entry<String, Variable> e : registers.entrySet()) {
            if (var.equals(e.getValue())) toReset.add(e.getKey());
            if (e.getValue() != null && !var.isArrayVar() && e.getValue().isArrayVar()) {
                if (var.getName().equals(e.getValue().getIndex())) toReset.add(e.getKey());
            }
        }
        toReset.forEach(r -> registers.put(r, null));
    }

    public boolean equals(String register, Variable var) {
        Variable el = registers.get(register);
        if (el == null) return false;
        if ((var instanceof Pointer) && (el instanceof Pointer) && el.equals(var)) return true;
        return !(var instanceof Pointer) && !(el instanceof Pointer) && el.equals(var);
    }

    public void set(String reg, Variable val) {
        addToUsageList(reg);
        registers.put(reg, val);
    }
}
