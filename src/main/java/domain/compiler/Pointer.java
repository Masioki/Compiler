package domain.compiler;

import domain.tree.Variable;

public class Pointer extends Variable {
    public Pointer(String name) {
        super(name);
    }

    public Pointer(long num) {
        super(num);
    }

    public Pointer(String name, String index) {
        super(name, index);
    }

    public Pointer(String name, long index) {
        super(name, index);
    }

    public Pointer(Variable variable) {
        super(variable.getName(), variable.getIndex());
    }

    @Override
    public String toString() {
        return "Pointer{" +
                "name='" + getName() + '\'' +
                ", index='" + getIndex() + '\'' +
                '}';
    }
}
