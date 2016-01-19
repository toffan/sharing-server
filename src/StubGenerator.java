import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.util.HashSet;
import java.util.Arrays;


public class StubGenerator {

    StubGenerator() {}
    private static final HashSet<String> forbidden_meths = new HashSet<>(
        Arrays.asList("lock_read", "lock_write", "unlock"));
    private static int indent;

    private static String generate_params(Method meth, boolean decorate) {
        String code = "";

        int nb_params = 0;
        for (Class p: meth.getParameterTypes()) {
            if (nb_params > 0) {
                code += ", ";
            }
            if (decorate) {
                code += p.getName() + " ";
            }
            code += String.format("p%d", nb_params);
            nb_params++;
        }
        return code;
    }

    private static String generate_meth(Class cls, Method meth) {
        String code = "";
        String line;

        // Signature
        line = newline();
        line += Modifier.toString(meth.getModifiers());
        line += " " + meth.getReturnType().getName();
        line += " " + meth.getName();
        line += "(" + generate_params(meth, true) + ") {\n";
        code += line;

        // Cast
        ++indent;
        line = newline();
        line += cls.getName() + " o = (" + cls.getName() + ") obj;\n";
        code += line;

        // Exec
        line = newline();
        if (!meth.getReturnType().equals(Void.TYPE)) {
            line += "return ";
        }
        line += "o." + meth.getName();
        line += "(" + generate_params(meth, false) + ");\n";
        code += line;

        // End
        --indent;
        line = newline() + "}\n";
        code += line;

        return code;
    }

    private static String generate_stub(Class cls) {
        assert (cls.getName().endsWith("_itf"));
        String name = cls.getName().substring(0, cls.getName().length()-4);

        String code = "";
        String line;

        // Signature
        line = newline();
        line += "public class " + name + "_stub";
        line += " extends SharedObject";
        line += " implements " + name + "_itf, java.io.Serializable {\n";
        code += line;

        // Methods
        ++indent;
        for (Method m : cls.getMethods()) {
            if (!forbidden_meths.contains(m.getName())) {
                code += "\n" + generate_meth(cls, m);
            }
        }
        --indent;

        // End
        line = newline();
        line += "}\n";
        code += line;

        return code;
    }

    private static String newline() {
        String line = "";
        for (int i = 0; i < indent; ++i) {
            line += "    ";
        }
        return line;
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("test.java", "UTF-8");
        writer.println(generate_stub(Sentence_itf.class));
        writer.close();
    }
}

