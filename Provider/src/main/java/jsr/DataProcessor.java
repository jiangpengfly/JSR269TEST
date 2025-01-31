package jsr;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author olifer
 */
@SupportedAnnotationTypes("jsr.Data")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DataProcessor extends AbstractProcessor {
    private JavacTrees trees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Data.class).stream().map(element -> trees.getTree(element)).forEach(t -> t.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                jcClassDecl.defs.stream().filter(k -> k.getKind().equals(Tree.Kind.VARIABLE)).map(tree -> (JCTree.JCVariableDecl) tree).forEach(jcVariableDecl -> {
                    //添加get方法
                    jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                    //添加set方法
                    jcClassDecl.defs = jcClassDecl.defs.prepend(makeSetterMethodDecl(jcVariableDecl));
                });
                super.visitClassDef(jcClassDecl);
            }
        }));
        return true;
    }

    /**
     * 创建get方法
     *
     * @param jcVariableDecl
     * @return
     */
    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        //方法的访问级别
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //方法名称
        Name methodName = getMethodName(jcVariableDecl.getName());
        //设置返回值类型
        JCTree.JCExpression returnMethodType = jcVariableDecl.vartype;
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        //statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
        JCTree.JCNewClass combatJCTreeMain = treeMaker.NewClass(
                null,
                com.sun.tools.javac.util.List.nil(),
                treeMaker.Ident(names.fromString("Util")),
                com.sun.tools.javac.util.List.nil(),
                null
        );

        statements.append(
                treeMaker.VarDef(
                        treeMaker.Modifiers(Flags.PARAMETER),
                        names.fromString("util"),
                        treeMaker.Ident(names.fromString("Util")),
                        combatJCTreeMain
                )
        );

        JCTree.JCExpressionStatement jcExpressionStatement = treeMaker.Exec(
                treeMaker.Apply(
                        com.sun.tools.javac.util.List.nil(),
                        treeMaker.Select(
                                treeMaker.Ident(names.fromString("util")), // . 左边的内容
                                names.fromString("test") // . 右边的内容
                        ),
                        com.sun.tools.javac.util.List.of(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()))
                )
        );
        //statements.append(treeMaker.Return(jcExpressionStatement.getExpression()));
        List<JCTree.JCExpression> var2 = List.nil();
        var2 = var2.append(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()));
        statements.append(treeMaker.Return(treeMaker.Apply(com.sun.tools.javac.util.List.nil(), memberAccess("jsr.Util.test"), var2)));

        //设置方法体
        JCTree.JCBlock methodBody = treeMaker.Block(0, statements.toList());
        List<JCTree.JCTypeParameter> methodGenericParams = List.nil();
        List<JCTree.JCVariableDecl> parameters = List.nil();
        List<JCTree.JCExpression> throwsClauses = List.nil();
        //构建方法
        return treeMaker.MethodDef(modifiers, methodName, returnMethodType, methodGenericParams, parameters, throwsClauses, methodBody, null);
    }

    /**
     * 创建set方法
     *
     * @param jcVariableDecl
     * @return
     */
    private JCTree.JCMethodDecl makeSetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        try {
            //方法的访问级别
            JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
            //定义方法名
            Name methodName = setMethodName(jcVariableDecl.getName());
            //定义返回值类型
            JCTree.JCExpression returnMethodType = treeMaker.Type((Type) (Class.forName("com.sun.tools.javac.code.Type$JCVoidType").newInstance()));
            ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
            statements.append(treeMaker.Exec(treeMaker.Assign(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()), treeMaker.Ident(jcVariableDecl.getName()))));
            //定义方法体
            JCTree.JCBlock methodBody = treeMaker.Block(0, statements.toList());
            List<JCTree.JCTypeParameter> methodGenericParams = List.nil();
            //定义入参
            JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER, List.nil()), jcVariableDecl.name, jcVariableDecl.vartype, null);
            //设置入参
            List<JCTree.JCVariableDecl> parameters = List.of(param);
            List<JCTree.JCExpression> throwsClauses = List.nil();
            //构建新方法
            return treeMaker.MethodDef(modifiers, methodName, returnMethodType, methodGenericParams, parameters, throwsClauses, methodBody, null);
        } catch (Exception e) {
            System.err.println(e);
        }
        return null;

    }

    private Name getMethodName(Name name) {
        String s = name.toString();
        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }


    private Name setMethodName(Name name) {
        String s = name.toString();
        return names.fromString("set" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }

    //传入一个类的全路径名，获取对应类的JCIdent
    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(componentArray[i]));
        }
        return expr;
    }
}
