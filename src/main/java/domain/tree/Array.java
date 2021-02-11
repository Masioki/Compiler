package domain.tree;


import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

@Getter
public class Array {

    private final String name;
    private final long start, end;

    public Array(@NonNull String name, long start, long end) throws IndexOutOfBoundsException {
        if (start > end)
            throw new IndexOutOfBoundsException("Array start index should be less or equal to end index: t(a:b) a<= b");
        this.name = name;
        this.start = start;
        this.end = end;
    }

    private boolean inRange(long ind) {
        return ind >= start && ind <= end;
    }

    public boolean inRange(String ind) {
        if (ind.matches("^[0-9]+$")) return inRange(Long.parseLong(ind));
        return true;
    }

    public long size() {
        return end - start + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Array)) return false;
        Array array = (Array) o;
        return Objects.equals(name, array.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Array{" +
                "name='" + name + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
