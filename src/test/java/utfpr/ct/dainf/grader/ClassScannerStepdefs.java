package utfpr.ct.dainf.grader;

import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.ParseException;
import bsh.TargetError;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.testng.Assert.*;
import org.testng.Reporter;
import utfpr.ct.dainf.grader.support.ClassEntry;
import utfpr.ct.dainf.grader.support.ClassScanner;
import utfpr.ct.dainf.grader.support.InterpreterRunner;

/**
 *
 * @author Wilson
 */
public class ClassScannerStepdefs {
    
    public static final String DEFAULT_PACKAGE_ALIAS = "<default>";
    
    private String expectedMainClass;
    private double maxGrade;
    private static double grade;
    private Class<?> mainClass;
    private Class<?> currentClass;
    private final ClassScanner scanner = new ClassScanner();
    private final static Interpreter bsh = new Interpreter();
    private int scriptTimeout = 0;
    private static final PrintStream SYSTEM_OUT = System.out;
    private static final InputStream SYSTEM_IN = System.in;
    private static final DelegateInputStream testInput = new DelegateInputStream(SYSTEM_IN);
    
    static {
        System.setIn(testInput);
    }
    
    @Given("^the main class is '(.+)'$")
    public void setExpectedMainClass(String expectedMainClass) {
        this.expectedMainClass = expectedMainClass;
    }

    @Given("^the maximum grade is (\\d*\\.?\\d+)$")
    public void setMaxGrade(double maxGrade) {
        this.maxGrade = maxGrade;
    }
    
    @Then("^award (\\d*\\.?\\d+) points$")
    public void addGrade(double points) {
        grade += points;
        Reporter.log(String.format("Atribuir %f/%f/%f pontos", points, grade, maxGrade), true);
    }
      
    @Given("^I set the script timeout to (\\d{1,6})$")
    public void setScriptTimeout(int timeout) {
        scriptTimeout = timeout;
    }
    
    @Given("^class '(.+)' exists somewhere store class in <(\\w+)>$")
    public ClassEntry classExistsSomewhere(String className, String var) throws Throwable {
        ClassEntry ce = scanner.findClassByName(className);
        assertNotNull(ce, String.format("Class '%s' not found", className));
        Class<?> clazz = ce.getClassObject();
        Reporter.log(String.format("Class '%s' found", clazz.getName()), true);
        bsh.set(var, clazz);
        return ce;
    }
    
    @Given("^class '(.+)' exists somewhere$")
    public ClassEntry classExistsSomewhere(String className) {
        ClassEntry ce = scanner.findClassByName(className);
        assertNotNull(ce, String.format("Class '%s' not found", className));
        Reporter.log(String.format("Class '%s' found", className), true);
        return ce;
    }
    
    @Given("^class '(.+)' exists$")
    public ClassEntry classExists(String qcn) {
        ClassEntry ce = scanner.findEntry(qcn);
        assertNotNull(ce, String.format("Class '%s' not found", qcn));
        Reporter.log(String.format("Class '%s' found", qcn));
        currentClass = ce.getClassObject();
        return ce;
    }
    
    @Given("^class '(.+)' exists store class in <(\\w+)>$")
    public void classExists(String qcn, String var) throws Throwable {
        Class<?> clazz;
        try {
            clazz = Class.forName(qcn, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            clazz = null;
            Reporter.log(String.format("Class '%s' not found", qcn), true);
        }
        bsh.set(var, clazz);
        assertNotNull(clazz, String.format("Class '%s' found", qcn));
    }
    
    @Given("^class <(.+)> implements '(.+)'$")
    public void classImplementsInterface(String clsVar, String intName) throws Throwable {
        Class<?> clazz = (Class) bsh.get(clsVar);
        Type[] genTypes = clazz.getGenericInterfaces();
        boolean found = false;
        int i = 0;
        while (!found && i < genTypes.length) {
            found = genTypes[i].getTypeName().startsWith(intName);
            i++;
        }
        assertTrue(found, String.format("'%s' does not implement '%s'", clazz.getName(), intName));
        Reporter.log(String.format("'%s' implements '%s'", clazz.getName(), intName), true);        
    }

    @Given("^class <(.+)> is generic$")
    public void isClassGeneric(String clsVar) throws Throwable {
        Class<?> clazz = (Class) bsh.get(clsVar);
        TypeVariable[] tvars = clazz.getTypeParameters();
        assertTrue(tvars.length > 0, String.format("Class '%s' is not generic", clazz.getName()));
        Reporter.log(String.format("Class '%s' is generic", clazz.getName()), true);
    }
    
    @Given("^I store <(.+)> generic type in  <(\\w+)>$")
    public void storeGenericType(String clsVar, String typeVar) throws Throwable {
        Class<?> clazz = (Class) bsh.get(clsVar);
        TypeVariable<?>[] tvars = clazz.getTypeParameters();
        assertEquals(tvars.length, 1, String.format("More than 1 type found in '%s'", clazz.getName()));
        bsh.set(typeVar, tvars[0]);
    }
    
    @Given("^generic type <(.+)> has upper bound '(.+)'$")
    public void genericTypeBoundsContain(String typeVar, String bound) throws Throwable {
        TypeVariable<?> tvar = (TypeVariable) bsh.get(typeVar);
        Type[] types = tvar.getBounds();
        boolean found = false;
        for (int i = 0; i < types.length && !found; i++) {
            found = types[i].getTypeName().startsWith(bound);
        }
        assertTrue(found, String.format("No upper bound '%s' found for '%s'.", bound, tvar.getName()));
        Reporter.log(String.format("Upper bound '%s' found for '%s'.", bound, tvar.getName()), true);
    }

    
    @Given("^class '(.+)' is assignable from <(.+)>$")
    public void classExtends(String superClassName, String clsVar) throws Throwable {
        Class<?> clazz = (Class) bsh.get(clsVar);
        Class<?> superClass = Class.forName(superClassName, false, ClassLoader.getSystemClassLoader());
        assertTrue(superClass.isAssignableFrom(clazz),
            String.format("'%s' is not assignable from '%s'", superClassName, clazz.getName()));
        Reporter.log(String.format("'%s' is assignable from '%s'", superClassName, clazz.getName()), true);        
    }

    @Given("^constructor '(.+)\\((.+)\\)' exists$")
    public void constructorExists(String className, List<String> args) {
        Class<?> clazz = scanner.findEntry(className).getClassObject();
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean found = false;
        for (Constructor<?> c: constructors) {
            Class<?>[] params = c.getParameterTypes();
            int i;
            for (i = 0; i < params.length; i++) {
                if (!params[i].getName().equals(args.get(i))) break; 
            }
            found = i == params.length;
            if (found) break;
        }
        assertTrue(found, String.format("Constructor for '%s' not found", className));
        Reporter.log(String.format("Constructor for '%s' found", className), true);
    }
    
    @Given("^I import <(\\w+)>$")
    public void importClass(String var) throws Throwable, ClassNotFoundException {
        Class<?> clazz = (Class)bsh.get(var);
        bsh.eval("import " + clazz.getName());
    }
    
    @Given("^I evaluate '(.+)'$")
    public void evaluateCode(String code) throws Throwable {
        InterpreterRunner ir = new InterpreterRunner(bsh, code);
        ir.start();
        ir.join(scriptTimeout);
        if (ir.isError()) {
            Throwable t = ir.getScriptException();
            if (t instanceof ParseException) {
                Reporter.log(String.format("Parsing error evaluating '%s' : %s", code, t.getLocalizedMessage()), true);
            } else if (t instanceof InterpreterError) {
                Reporter.log(String.format("Internal error evaluating '%s' : %s", code, t.getLocalizedMessage()), true);
            } else if (t instanceof TargetError) {
                Throwable te = ((TargetError) t).getTarget();
                if (te instanceof OutOfMemoryError) {
                    Reporter.log(String.format("Out of memory evaluating '%s' : %s", code, te.getLocalizedMessage()), true);
                } else {
                    Reporter.log(String.format("Targer error evaluating '%s' : %s", code, te.getLocalizedMessage()), true);
                }
            } else {
                Reporter.log(String.format("Could not evaluate '%s' : %s", code, t.getLocalizedMessage()), true);
            }
            throw t;
        }
    }
    
    @Given("^I evaluate '(.+)' returning <(\\w+)>$")
    public void evaluateCode(String code, String var) throws Throwable {
        InterpreterRunner ir = new InterpreterRunner(bsh, code);
        ir.start();
        ir.join(scriptTimeout);
        if (ir.isError()) {
            Throwable t = ir.getScriptException();
            if (t instanceof ParseException) {
                Reporter.log(String.format("Parsing error evaluating '%s' : %s", code, t.getLocalizedMessage()), true);
            } else if (t instanceof InterpreterError) {
                Reporter.log(String.format("Internal error evaluating '%s' : %s", code, t.getLocalizedMessage()), true);
            } else if (t instanceof TargetError) {
                Throwable te = ((TargetError) t).getTarget();
                if (te instanceof OutOfMemoryError) {
                    Reporter.log(String.format("Out of memory evaluating '%s' : %s", code, te.getLocalizedMessage()), true);
                } else {
                    Reporter.log(String.format("Targer error evaluating '%s' : %s", code, te.getLocalizedMessage()), true);
                }
            } else {
                Reporter.log(String.format("Could not evaluate '%s' : %s", code, t.getLocalizedMessage()), true);
            }
            throw t;
        }
        
        Object result = ir.getResult();
        bsh.set(var, result);
    }
    
    @Given("^I set field '(\\w+)' in <(\\w+)> to <(\\w+)>$")
    public void setFieldInObject(String fieldVar, String objVar, String fieldValue)
            throws Throwable {
        Object o = bsh.get(objVar);
        Field f = o.getClass().getDeclaredField(fieldVar);
        f.setAccessible(true);
        f.set(o, bsh.eval(fieldValue));
    }
    
    @Given("^I get field '(\\w+)' value in super class of <(\\w+)> save in <(\\w+)>$")
    public void getFieldValueInObject(String fieldVar, String objVar, String var)
            throws Throwable {
        Object o = bsh.get(objVar);
        Class clazz = o.getClass();
        Field f = null;
        while (f == null && clazz != null) {
            try {
                f = clazz.getDeclaredField(fieldVar);
            } catch (NoSuchFieldException nsfe) {
                clazz = clazz.getSuperclass();
            }
        }
        if (f != null) {
            f.setAccessible(true);
            bsh.set(var, f.get(o));
        }
    }
    
    @Given("^expression '(.+)' evaluates to <(.+)>$")
    public void expressionEvaluatesTo(String expression, String value)
            throws Throwable {
        InterpreterRunner ir = new InterpreterRunner(bsh, expression);
        ir.start();
        ir.join(scriptTimeout);
        Object expValue = ir.getResult();
        Object testValue = bsh.eval(value);
        assertEquals(expValue, testValue, String.format("Expression '%s' value does not equal expected '%s' value", expression, testValue));
        Reporter.log(String.format("Expression '%s' value equals expected '%s' value", expression, testValue), true);
    } 
    
    @Given("^<(\\w+)> matches regex '(.+)'$")
    public void regexMatches(String var, String regex) throws Throwable {
        String value = bsh.get(var).toString();
        assertTrue(value.matches(regex), String.format("'%s' does not match '%s'", value, regex));
        Reporter.log(String.format("'%s' matches '%s'", value, regex), true);
    }
    @Given("^<(\\w+)> matches regex '(.+)' with '(.+)' options?$")
    public void regexMatchesWithFlags(String var, String regex, String flags) throws Throwable {
        bsh.eval("import java.util.regex.Pattern");
        String value = bsh.get(var).toString();
        Pattern pattern = Pattern.compile(regex, (int)bsh.eval(flags));
        Matcher matcher = pattern.matcher(value);
        assertTrue(matcher.matches(), String.format("'%s' does not match '%s'", value, regex));
        Reporter.log(String.format("'%s' matches '%s'", value, regex), true);
    }
    
  
    @Given("^<(\\w+)> matches regex <(\\w+)>$")
    public void regexVarMatches(String var, String reVar) throws Throwable {
        String value = bsh.get(var).toString();
        String regex = bsh.get(reVar).toString();
        assertTrue(value.matches(regex), String.format("'%s' does not match '%s'", value, regex));
        Reporter.log(String.format("'%s' matches '%s'", value, regex), true);
    }
    
    @Given("^<(\\w+)> matches regex <(\\w+)> with '(.+)' options?$")
    public void regexVarMatchesWithFlags(String var, String reVar, String flags) throws Throwable {
        String value = bsh.get(var).toString();
        String regex = bsh.get(reVar).toString();
        Pattern pattern = Pattern.compile(regex, (int)bsh.eval(flags));
        Matcher matcher = pattern.matcher(value);
        assertTrue(matcher.matches(), String.format("'%s' does not match '%s'", value, regex));
        Reporter.log(String.format("'%s' matches '%s'", value, regex), true);
    }
    
    @Given("^<(\\w+)> declares field '(\\w+)' save in <(\\w+)>$")
    public void objectDeclaresField(String objName, String fieldName, String var)
            throws Throwable {
        Object obj = bsh.get(objName);
        Field f = obj.getClass().getDeclaredField(fieldName);
        Reporter.log(String.format("Class '%s' declares field '%s'", obj.getClass().getName(), fieldName), true);
        bsh.set(var, f);
    }
    
    @Given("^class <(\\w+)> declares field '(\\w+)' save in <(\\w+)>$")
    public void classDeclaresField(String objName, String fieldName, String var)
            throws Throwable {
        Class clazz = (Class) bsh.get(objName);
        Field f = clazz.getDeclaredField(fieldName);
        Reporter.log(String.format("Class '%s' declares field '%s'", clazz.getName(), fieldName), true);
        bsh.set(var, f);
    }
    
    @Given("^field <(\\w+)> value in <(\\w+)> is '(.+)'$")
    public void fieldValueIs(String fieldVar, String objVar, String fieldValue)
            throws Throwable {
        Field f = (Field) bsh.get(fieldVar);
        f.setAccessible(true);
        Object o = bsh.get(objVar);
        bsh.set("value", f.get(o));
        boolean equal = (boolean)bsh.eval("value.equals(" + fieldValue + ")");
        assertTrue(equal, "Field value does not match expected value");
        Reporter.log(String.format("Field '%s' value in '%s' matches expected value of '%s'",
                fieldVar, objVar, bsh.eval(fieldValue)), true);
    }
        
    @Given("^field <(\\w+)> is of type '(.+)'$")
    public void fieldTypeIs(String fieldVar, String typeName)
            throws Throwable {
        Field f = (Field) bsh.get(fieldVar);
        Class clazz = f.getType();
        assertEquals(clazz.getName(), typeName,
            String.format("Field '%s' is not of type '%s'", f.getName(), clazz.getName()));
        Reporter.log(String.format("Field '%s' is of type '%s'", f.getName(), clazz.getName()), true);
    }
        
    @Given("^generic field <(\\w+)> is of type '(.+)'$")
    public void genericFieldTypeIs(String fieldVar, String typeName)
            throws Throwable {
        Field f = (Field) bsh.get(fieldVar);
        Class clazz = f.getType();
        assertTrue(f.toGenericString().contains(typeName),
            String.format("Field '%s' is not of type '%s'", f.getName(), typeName));
        Reporter.log(String.format("Field '%s' is of type '%s'", f.getName(), typeName), true);
    }
        
    @Given("^the class <(\\w+)> is in the '(.+)' package")
    public void classInPackage(String var, String pkg) throws Throwable {
        Class clazz = (Class)bsh.get(var);
        String pkgName = clazz.getPackage() == null ? "default" : clazz.getPackage().getName();
        assertEquals(pkg, pkgName, String.format("Class '%s' not found in expected package", clazz.getName()));
        Reporter.log(String.format("Class '%s' found in '%s' package",
                clazz.getName(), pkg), true);
    }
    
    @Given("^class <(\\w+)> has '(\\w+)' modifiers?$")
    public void classHasModifiers(String classVar, String modifier) throws Throwable {
        Class clazz = (Class)bsh.get(classVar);
        String classModif = Modifier.toString(clazz.getModifiers());
        assertTrue(classModif.contains(modifier), String.format("Class '%s' is not %s",
                clazz.getName(), modifier));
        Reporter.log(String.format("Class '%s' is %s", clazz.getName(), modifier), true);
    }
    
    @Given("^field <(\\w+)> has '(\\w+)' modifiers?$")
    public void fieldHasModifiers(String fieldVar, String modifier) throws Throwable {
        Field field = (Field)bsh.get(fieldVar);
        String fieldModif = Modifier.toString(field.getModifiers());
        assertTrue(fieldModif.contains(modifier), String.format("Field '%s' is not %s",
                field.getName(), modifier));
        Reporter.log(String.format("Field '%s' is %s", field.getName(), modifier), true);
    }
    
    @Given("^class <(\\w+)> declares '(.+)' constructor save in <(\\w+)>$")
    public void classDeclaresConstructor(String classVar, String signature, String var)
            throws Throwable {
        Constructor ct = null;
        Class clazz = (Class)bsh.get(classVar);
        Constructor[] cts = clazz.getDeclaredConstructors();
        for (Constructor c: cts) {
            if (c.toGenericString().contains(signature)) {
                ct = c;
                bsh.set(var, ct);
                break;
            }
        }
        assertNotNull(ct, String.format("Class '%s' does not declare '%s'", clazz.getName(), signature));
        Reporter.log(String.format("Class '%s' declares '%s'", clazz.getName(), signature), true);
    }
    
    @Given("^class <(\\w+)> declares '(.+)' constructor <(\\w+)> save in <(\\w+)>$")
    public void classDeclaresGenericConstructor(String classVar, String genSignature, String typeVar, String var)
            throws Throwable {
        Constructor ct = null;
        Class clazz = (Class)bsh.get(classVar);
        TypeVariable tvar = (TypeVariable) bsh.get(typeVar);
        String signature = genSignature.replaceAll("_T", tvar.getName());
        Constructor[] cts = clazz.getDeclaredConstructors();
        for (Constructor c: cts) {
            if (c.toGenericString().contains(signature)) {
                ct = c;
                bsh.set(var, ct);
                break;
            }
        }
        assertNotNull(ct, String.format("Class '%s' does not declare '%s'", clazz.getName(), signature));
        Reporter.log(String.format("Class '%s' declares '%s'", clazz.getName(), signature), true);
    }
    
    @Given("^class <(\\w+)> declares '(.+)' method save in <(\\w+)>$")
    public void classDeclaresMethod(String classVar, String methodSig, String var)
            throws Throwable {
        Method method = null;
        Class clazz = (Class)bsh.get(classVar);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m: methods) {
            if (m.toGenericString().contains(methodSig)) {
                method = m;
                bsh.set(var, method);
                break;
            }
        }
        assertNotNull(method, String.format("Class '%s' does not declare '%s'", clazz.getName(), methodSig));
        Reporter.log(String.format("Class '%s' declares '%s'", clazz.getName(), methodSig), true);
    }
    
    @Given("^class <(\\w+)> declares '(.+)' generic <(\\w+)> method save in <(\\w+)>$")
    public void classDeclaresGenericMethod(String classVar, String genericSignature, String typeVar, String var)
            throws Throwable {
        Method method = null;
        Class clazz = (Class)bsh.get(classVar);
        TypeVariable tvar = (TypeVariable) bsh.get(typeVar);
        String signature = genericSignature.replaceAll("_T", tvar.getName());
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m: methods) {
            if (m.toGenericString().contains(signature)) {
                method = m;
                bsh.set(var, method);
                break;
            }
        }
        assertNotNull(method, String.format("Class '%s' does not declare '%s'", clazz.getName(), signature));
        Reporter.log(String.format("Class '%s' declares '%s'", clazz.getName(), signature), true);
    }
    
    @Given("^member <(\\w+)> has '(\\w+)' modifiers?$")
    public void memberHasModifiers(String fieldVar, String modifier) throws Throwable {
        Member member = (Member)bsh.get(fieldVar);
        String memberModif = Modifier.toString(member.getModifiers());
        assertTrue(memberModif.contains(modifier), String.format("%s '%s' is %s",
                member.getClass().getSimpleName(), member.getName(), modifier));
        Reporter.log(String.format("%s '%s' is %s",
                member.getClass().getSimpleName(), member.getName(), modifier), true);
    }

    @Given("^<(\\w+)> is an interface$")
    public void isInterface(String var) throws Throwable {
        Class clazz = (Class) bsh.get(var);
        assertTrue(clazz.isInterface(), String.format("'%s' is not an interface", clazz.getName()));
        Reporter.log(String.format("'%s' is an interface", clazz.getName()), true);
    }
    
    @Given("^evaluating '(.+)' throws instance of '(.+)' save in <(\\w+)>$")
    public void evaluateThrowing(String expression, String exceptionName, String var) throws Throwable {
        final String exp = "Throwable evalException = null;" +
                           "try { %s; } catch (Throwable t) { evalException = t; }";
        InterpreterRunner ir = new InterpreterRunner(bsh, String.format(exp, expression));
        ir.start();
        ir.join(scriptTimeout);
        Throwable t = (Throwable) bsh.get("evalException");
        bsh.set(var, t);
        boolean isInstance = Class.forName(exceptionName, false, ClassLoader.getSystemClassLoader()).isInstance(t);
        assertNotNull(t, String.format("Expression '%s' does not throw any exception", expression));
        t.getClass().isInstance(Class.forName(exceptionName, false, ClassLoader.getSystemClassLoader()));
        assertTrue(isInstance, String.format("Expression '%s' does not throw instance of '%s'", expression, exceptionName));
        Reporter.log(String.format("Expression '%s' throws '%s'", expression, t.getClass().getName()), true);
    }
    
    @Given("^method <(.+)> returns type '(.+)'$")
    public void methodReturnsType(String methodVar, String typeName) throws Throwable {
        Method method = (Method)bsh.get(methodVar);
        Class<?> retType = method.getReturnType();
        assertEquals(retType.getName(), typeName,
            String.format("Method '%s' does not return '%s'", method.getName(), typeName));
        Reporter.log(String.format("Method '%s' returns '%s'", method.getName(), typeName), true);
    }
    
    @Given("^method <(.+)> returns generic type <(\\w+)>$")
    public void methodReturnsGenericType(String methodVar, String typeName) throws Throwable {
        Method method = (Method)bsh.get(methodVar);
        Type rtype = method.getGenericReturnType();
        TypeVariable<?> tvar = (TypeVariable) bsh.get(typeName);
        assertEquals(rtype, tvar,
            String.format("Method '%s' does not return '%s'", method.getName(), tvar.getName()));
        Reporter.log(String.format("Method '%s' returns '%s'", method.getName(), tvar.getName()), true);
    }
    
    @Given("^method <(.+)> returns generic type '(.+)'$")
    public void methodReturnsGenericTypeAsString(String methodVar, String typeName) throws Throwable {
        Method method = (Method)bsh.get(methodVar);
        assertTrue(method.toGenericString().contains(' ' + typeName + ' '),
            String.format("Method '%s' does not return '%s'", method.getName(), typeName));
        Reporter.log(String.format("Method '%s' returns '%s'", method.getName(), typeName), true);
    }
    
    @Given("^I set <(.+)> accessible$")
    public void setMemberAccessible(String memberVar) throws Throwable {
        AccessibleObject member = (AccessibleObject) bsh.get(memberVar);
        if (!member.isAccessible()) member.setAccessible(true);
    }
    
    @Given("^I report <(.+)>$")
    public void reportExpressionValue(String exp) throws Throwable {
        String result = bsh.eval(exp).toString();
        Reporter.log(result, true);
    }
        
    @Given("^I report '(.+)'$")
    public void report(String msg) {
        Reporter.log(msg, true);
    }
    
    @Given("I set output to <(.+)>")
    public void setOutput(String out) throws Throwable {
        if (out.equals("default")) {
            System.setOut(SYSTEM_OUT);
            bsh.unset(out);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));
            bsh.set(out, baos);
        }
    }
    
    @Given("I set input from file <(.+)>")
    public void setInputFile(String inFile) throws Throwable {
        if (inFile.equals("default")) {
            System.setIn(SYSTEM_IN);
            bsh.unset("in");
        } else {
            File file = (File) bsh.get(inFile);
            FileInputStream fis = new FileInputStream(file);
            System.setIn(fis);
            bsh.set("in", fis);
        }
    }
    
    @Given("I set input from resource '(.+)'")
    public void setInputResource(String inFile) throws Throwable {
        if (inFile.equals("default")) {
            InputStream is = (InputStream) bsh.get("in");
            testInput.setInput(SYSTEM_IN);
            bsh.unset("in");
        } else {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(inFile);
            testInput.setInput(is);
            bsh.set("in", is);
        }
    }
    
    @Given("I set input from <(.+)>")
    public void setInputString(String var) throws Throwable {
        if (var.equals("default")) {
            InputStream is = (InputStream) bsh.get("in");
            testInput.setInput(SYSTEM_IN);
            bsh.unset("in");
        } else {
            String s = (String) bsh.get(var);
            InputStream is = new ByteArrayInputStream(s.getBytes());
            testInput.setInput(is);
            bsh.set("in", is);
        }
    }
    
    @Given("I report grade formatted as '(.+)'")
    public void reportCurrentGradeFormatted(String format) {
        Reporter.log(String.format(format, grade), true);
        System.setSecurityManager(null);
    }
}
