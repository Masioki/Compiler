package domain.tree;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Variable {
    private String name;
    private boolean isConstant;
    private String index;

    public Variable(String name) {
        this.name = name;
        this.isConstant = false;
        this.index = "-1";
    }

    public Variable(long num) {
        this.name = Long.toString(num);
        this.isConstant = true;
        this.index = "-1";
    }

    public Variable(String name, String index) {
        this.name = name;
        this.index = index;
        this.isConstant = false;
    }

    public Variable(String name, long index) {
        this(name, Long.toString(index));
    }


    public Variable copy() {
        Variable variable = new Variable(name);
        variable.setName(this.name);
        variable.setConstant(this.isConstant);
        variable.setIndex(this.index);
        return variable;
    }

    public boolean isArrayVar() {
        return !index.equals("-1");
    }

    public boolean isIndexConstant() {
        if (index.equals("-1")) return true;
        return index.matches("^[0-9]+$");
    }


    public boolean isInitialized(List<Variable> initialized) {
        if (isArrayVar()) {
            if (index.matches("^[0-9]+$"))
                return initialized.contains(this);
            else return initialized.contains(new Variable(index));
        }
        return isConstant() || initialized.contains(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable)) return false;
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name) && Objects.equals(index, variable.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index);
    }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                ", index='" + index + '\'' +
                '}';
    }
}
