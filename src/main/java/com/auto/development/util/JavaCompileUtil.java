package com.auto.development.util;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

/**
 * @author Luchaoxin
 * @version V 1.0
 * @Description: 动态编译代码工具类
 * @date 2019-05-11 23:42
 */
public class JavaCompileUtil {

    /**
     * 编译java文件
     *
     * @param javaSourcePath java代码绝对路径
     * @param classesPath    编译成功之后class文件存放父目录
     * @return
     * @throws IOException
     */
    public static boolean compiler(String javaSourcePath, String classesPath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 建立DiagnosticCollector对象
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            // 建立源文件对象，每个文件被保存在一个从JavaFileObject继承的类中
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(javaSourcePath));
            // options命令行选项,指定的路径一定要存在，javac不会自己创建文件夹
            Iterable<String> options = Arrays.asList("-d", classesPath);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                    compilationUnits);
            // 编译源程序
            boolean success = task.call();
            return success;
        }
    }

    /**
     * 编译java字符串代码
     *
     * @param className   java文件名称
     * @param javaCodeStr java代码 如：
     *                    public class HelloWorld {
     *                    public static void main(String args[]) {
     *                    System.out.println(\"Hello, World\");
     *                    }
     *                    }
     * @param classesPath 编译成功之后class文件存放父目录
     * @return
     * @throws Exception
     */
    public static boolean compiler(String className, String javaCodeStr, String classesPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector diagnostics = new DiagnosticCollector();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            SimpleJavaFileObject file = new JavaSourceFromString(className, javaCodeStr);
            Iterable compilationUnits = Arrays.asList(file);
            // options命令行选项
            Iterable<String> options = Arrays.asList("-d", classesPath);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                    compilationUnits);

            boolean success = task.call();
            return success;
        }

    }

    // 用于传递源程序的JavaSourceFromString类
    static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;
        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
