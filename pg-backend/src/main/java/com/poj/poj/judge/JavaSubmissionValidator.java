package com.poj.poj.judge;

import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse declarations only: never compile or execute submitted code in the backend. */
public final class JavaSubmissionValidator {
    private JavaSubmissionValidator() { }

    public static void validate(String code) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "后端需要使用完整 JDK 启动以校验 Java 入口");
        }
        JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///Main.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
        };
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(new StringWriter(), files, diagnostics,
                    Collections.singletonList("-proc:none"), null, Collections.singletonList(source));
            for (CompilationUnitTree unit : task.parse()) {
                if (unit.getPackageName() != null) reject("提交代码不能声明 package，请删除包名后再提交");
                for (Tree declaration : unit.getTypeDecls()) {
                    if (declaration.getKind() != Tree.Kind.CLASS) continue;
                    ClassTree mainClass = (ClassTree) declaration;
                    if (!mainClass.getSimpleName().contentEquals("Main")
                            || !mainClass.getModifiers().getFlags().contains(Modifier.PUBLIC)) continue;
                    for (Tree member : mainClass.getMembers()) {
                        if (member.getKind() != Tree.Kind.METHOD) continue;
                        MethodTree method = (MethodTree) member;
                        Set<Modifier> flags = method.getModifiers().getFlags();
                        if (!method.getName().contentEquals("main") || method.getReturnType() == null
                                || !"void".equals(method.getReturnType().toString())
                                || !flags.contains(Modifier.PUBLIC) || !flags.contains(Modifier.STATIC)
                                || method.getParameters().size() != 1 || method.getBody() == null) continue;
                        String parameter = method.getParameters().get(0).getType().toString().replace(" ", "");
                        if ("String[]".equals(parameter) || "java.lang.String[]".equals(parameter)) return;
                    }
                    reject("Java 程序必须在 Main 类中包含 public static void main(String[] args) 入口");
                }
            }
            reject("请提交完整的 Java 程序，必须包含顶层 public class Main，不能只提交数字或文字");
        } catch (BusinessException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Java 入口校验暂时不可用");
        }
    }

    private static void reject(String message) { throw new BusinessException(ErrorCode.PARAMS_ERROR, message); }
}
