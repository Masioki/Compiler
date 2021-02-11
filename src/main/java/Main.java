import domain.compiler.MemoryManager;

import java.io.File;
import java.io.FileReader;

public class Main {
    public static void main(String[] argv) {
        MemoryManager.OUT_FILE = argv[1];
        try {
            parser p = new parser(new Lexer(new FileReader(argv[0])));
            Object result = p.parse().value;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // compile all in folder
//        File path = new File("path");
//
//        File[] files = path.listFiles();
//        for (File file : files) {
//            if (file.isFile()) {
//                fun(file);
//            }
//        }
    }

    public static void fun(File file) {
        MemoryManager.OUT_FILE = "output/"+file.getName().replace(".imp", ".mr");
        try {
            parser p = new parser(new Lexer(new FileReader(file.getAbsolutePath())));
            Object result = p.parse().value;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}