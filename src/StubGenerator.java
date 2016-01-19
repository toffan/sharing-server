import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;


public class StubGenerator {

    StubGenerator() {}

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
        // Signature
        String sign = "";
        sign += Modifier.toString(meth.getModifiers());
        sign += " " + meth.getReturnType().getName();
        sign += " " + meth.getName();
        sign += "(" + generate_params(meth, true) + ") {\n";

        // Code
        String code = "";
        code += cls.getName() + " o = (" + cls.getName() + ") obj;\n";
        if (!meth.getReturnType().equals(Void.TYPE)) {
            code += "return ";
        }
        code += "o." + meth.getName();
        code += "(" + generate_params(meth, false) + ");\n";
        code += "}\n";

        return sign + code;
    }

    private static String generate_stub(Class cls) {
        assert (cls.getName().endsWith("_itf"));
        String name = cls.getName().substring(0, cls.getName().length()-4);
        String src;

        // Signature
        String sign = "";
        sign += "public class " + name + "_stub";

        sign += " extends SharedObject";
        sign += " implements " + name + "_itf, java.io.Serializable {\n";

        // Code
        String code = "";

        for (Method m : cls.getMethods()) {
            code += generate_meth(cls, m) + "\n";
        }

        code += "}\n";

        return sign + code;
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("test.java", "UTF-8");
        writer.println(generate_stub(Sentence_itf.class));
        writer.close();
    }
}

