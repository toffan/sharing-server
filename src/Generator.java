import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;


public class Generator {

    private static int indent;
    private static String newline() {
        String line = "";
        for (int i = 0; i < indent; ++i) {
            line += "    ";
        }
        return line;
    }

    private static final HashSet<String> forbidden_meths = new HashSet<>(
        Arrays.asList("lock_read", "lock_write", "unlock", "wait", "equals",
            "toString", "hashCode", "getClass", "notify", "notifyAll"));
    private static LinkedList<String> lines = new LinkedList<String>();


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

    private static String generate_prototype(Method meth) {
        String line = newline();
        line += Modifier.toString(meth.getModifiers());
        line += " " + meth.getReturnType().getName();
        line += " " + meth.getName();
        line += "(" + generate_params(meth, true) + ")";
        return line;
    }

    private static void generate_meth(Class cls, Method meth) {
        String line;

        // Signature
        lines.add(generate_prototype(meth) + " {");

        // Cast
        ++indent;
        line = newline();
        line += cls.getName() + " o = (" + cls.getName() + ") obj;";
        lines.add(line);

        // Exec
        line = newline();
        if (!meth.getReturnType().equals(Void.TYPE)) {
            line += "return ";
        }
        line += "o." + meth.getName();
        line += "(" + generate_params(meth, false) + ");";
        lines.add(line);

        // End
        --indent;
        lines.add(newline() + "}");
    }

    private static void generate_stub(String name) {
        Class cls;
        try {
            cls = Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        String line;

        // Signature
        line = newline();
        line += "public class " + name + "_stub";
        line += " extends SharedObject";
        line += " implements " + name + "_itf, java.io.Serializable {";
        lines.add(line);

        // Body
        ++indent;
        // Constructor
        lines.add(newline() + "public " + name + "_stub (int id) {");
        ++indent;
        lines.add(newline() + "super(id);");
        --indent;
        lines.add(newline() + "}");
        lines.add("");

        // Methods
        for (Method m : cls.getMethods()) {
            if (!forbidden_meths.contains(m.getName())) {
                generate_meth(cls, m);
                lines.add("");
            }
        }
        --indent;

        // End
        line = newline();
        line += "}";
        lines.add(line);
    }

    private static void generate_itf(String name) {
        Class cls;
        try {
            cls = Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        String line;

        // Signature
        line = newline();
        line += "public interface " + name + "_itf";
        line += " extends SharedObject_itf {";
        lines.add(line);

        // Prototypes
        ++indent;
        for (Method m : cls.getMethods()) {
            if (!forbidden_meths.contains(m.getName())) {
                lines.add(generate_prototype(m) + ";");
            }
        }
        --indent;

        lines.add(newline() + "}");
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, ClassNotFoundException, IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        for (String cls_name : args) {
            generate_itf(cls_name);
            String itf_filename = cls_name + "_itf.java";
            Files.write(Paths.get(itf_filename), lines, Charset.forName("UTF-8"));
            lines.clear();
            if (compiler.run(null, null, null, itf_filename) != 0) {
                throw new RuntimeException("Itf compilation failed");
            }

            generate_stub(cls_name);
            String stub_filename = cls_name + "_stub.java";
            Files.write(Paths.get(stub_filename), lines, Charset.forName("UTF-8"));
            lines.clear();
            if (compiler.run(null, null, null, stub_filename) != 0) {
                throw new RuntimeException("Stub compilation failed");
            }
        }
    }
}

