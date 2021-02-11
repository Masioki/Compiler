package domain.exceptions;

public class VariableException extends Exception {
    public VariableException(String name, int line) {
        super("Incorrect variable usage: '" + name + "' at line " + line);
    }

    public VariableException(String message) {
        super(message);
    }
}
