import javassist.*;

import java.io.*;
import java.net.URL;
import org.apache.commons.io.FileUtils;

public class JaBRO {

    private String className;
    private String methodName;
    private Object[] args;
    private String dependencies;

    JaBRO(String className, String methodName, Object[] args, String dependencies) throws IOException {
        this.className = className;
        this.methodName = methodName;
        this.args = args;
        this.dependencies = dependencies;

        URL sootSource = getClass().getResource("/sootclasses-trunk-jar-with-dependencies.jar");
        File soot = new File(System.getProperty("user.dir")+File.separator+"sootclasses-trunk-jar-with-dependencies.jar");
        FileUtils.copyURLToFile(sootSource,soot);
    }

    JaBRO(String className,Object[] args, String dependencies) throws IOException {
        this.className = className;
        this.args = args;
        this.dependencies = dependencies;

        URL sootSource = getClass().getResource("/sootclasses-trunk-jar-with-dependencies.jar");
        File soot = new File(System.getProperty("user.dir")+File.separator+"sootclasses-trunk-jar-with-dependencies.jar");
        FileUtils.copyURLToFile(sootSource,soot);
    }

    public File runMediumMethod() throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ct = pool.getAndRename(className,"New"+className);
        CtMethod cm = ct.getDeclaredMethod(methodName);

        for (int i = 0; i < args.length; i++){
            if (args[i] == null){
                cm.insertBefore("$"+(i+1)+" = "+args[i]+";");
            }else if (args[i].getClass() == String.class){
                cm.insertBefore("$"+(i+1)+" = \""+args[i]+"\";");
            }else if (args[i].getClass() == String[].class){
                String[] astr = (String[]) args[i];
                for (int j = 0; j < astr.length; j++){
                    if (astr[j] == null){
                        cm.insertBefore("$"+(i+1)+"["+j+"] = "+astr[j]+";");
                    }else{
                        cm.insertBefore("$"+(i+1)+"["+j+"] = \""+astr[j]+"\";");
                    }
                }
            } else if (args[i].getClass() == File.class){
                File f = (File) args[i];
                String path = f.getAbsolutePath();
                System.out.println("File path: "+path);
                cm.insertBefore("$"+(i+1)+"= new java.io.File(\""+path+"\");");
            } else {
                System.out.println(args[i]);
                cm.insertBefore("$"+(i+1)+" = "+args[i]+";");
            }
        }

        ct.writeFile(System.getProperty("user.dir")+ File.separator+"JaBRO");
        ct.defrost();

        Process p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp .:"+dependencies+" -O -process-dir "+System.getProperty("user.dir")+File.separator+"JaBRO " +"New"+className);
        getMessage(p);
        String newClass = className.replace('.','/');

        return new File(System.getProperty("user.dir")+File.separator+"sootOutput"+File.separator+"New"+newClass+".class");

    }

    public File runMediumConstructor() throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ct = pool.getAndRename(className,"New"+className);
        CtClass[] params = new CtClass[args.length];
        for (int i = 0; i < args.length; i++){
            params[i] = pool.get(args[i].getClass().getName());
        }
        CtConstructor cc = ct.getDeclaredConstructor(params);

        for (int i = 0; i < args.length; i++){
            if (args[i] == null){
                cc.insertBefore("$"+(i+1)+" = "+args[i]+";");
            }else if (args[i].getClass() == String.class){
                cc.insertBefore("$"+(i+1)+" = \""+args[i]+"\";");
            }else if (args[i].getClass() == String[].class){
                String[] astr = (String[]) args[i];
                for (int j = 0; j < astr.length; j++){
                    if (astr[j] == null){
                        cc.insertBefore("$"+(i+1)+"["+j+"] = "+astr[j]+";");
                    }else{
                        cc.insertBefore("$"+(i+1)+"["+j+"] = \""+astr[j]+"\";");
                    }
                }
            } else{
                cc.insertBefore("$"+(i+1)+" = "+args[i]+";");
            }
        }

        ct.writeFile(System.getProperty("user.dir")+ File.separator+"JaBRO");
        ct.defrost();

        Process p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp "+dependencies+" -O -process-dir "+System.getProperty("user.dir")+File.separator+"JaBRO " +"New"+className);
        getMessage(p);

        String newClass = className.replace('.','/');
        return new File(System.getProperty("user.dir")+File.separator+"sootOutput"+File.separator+"New"+newClass+".class");

    }

    public File runCoarse() throws IOException, NotFoundException, CannotCompileException {

        ClassPool pool = ClassPool.getDefault();
        CtClass ct = pool.getAndRename(className, "New"+className);
        CtMethod cm = ct.getDeclaredMethod(methodName);

        if (args != null){
            if(args.getClass() == String[].class){
                String[] mArgs = (String[]) args;
                for (int i = 0; i < mArgs.length; i++){
                    if (mArgs[i] == null){
                        cm.insertBefore("$1["+i+"] = "+mArgs[i]+";");
                    }else{
                        cm.insertBefore("$1["+i+"] = \""+mArgs[i]+"\";");
                    }
                }
            }
        }

        ct.writeFile(System.getProperty("user.dir")+File.separator+"JaBRO");
        ct.defrost();

        Process p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp .:"
                +dependencies+" -W -process-dir "+System.getProperty("user.dir")+File.separator+"JaBRO "+"New"+className);
        getMessage(p);
        p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp .:"
                +dependencies+" -app -process-dir "+System.getProperty("user.dir")+File.separator+"sootOutput "+"New"+className);
        getMessage(p);

        String newClass = className.replace('.','/');
        return new File(System.getProperty("user.dir")+File.separator+"sootOutput"+File.separator+"New"+newClass+".class");
    }

    private void getMessage(Process p) throws IOException {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String s = null;
        while((s = stdInput.readLine()) != null){
            System.out.println(s);
        }


        while ((s = stdErr.readLine()) != null){
            System.out.println("Error: " + s);
        }
    }
}
