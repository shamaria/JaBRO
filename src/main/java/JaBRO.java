import javassist.*;

import java.io.*;
import java.net.URL;
import org.apache.commons.io.FileUtils;

public class JaBRO {

    private String className;
    private String methodName;
    private Object[] args;
    private char granularity;
    private String dependencies;

    JaBRO(String className, String methodName, Object[] args, char granularity, String dependencies){
        this.className = className;
        this.methodName = methodName;
        this.args = args;
        this.granularity = granularity;
        this.dependencies = dependencies;
    }

    public void run() throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass ct = pool.getAndRename(className,"New"+className);
        CtMethod cm = ct.getDeclaredMethod(methodName);

        for (int i = 0; i < args.length; i++){
            if (args[i].getClass() == String.class){
                if (args[i] != null){
                    cm.insertBefore("$"+(i+1)+" = \""+args[i]+"\";");
                }else{
                    cm.insertBefore("$"+(i+1)+" = "+args[i]+";");
                }
            } else if (args[i].getClass() == String[].class){
                String[] astr = (String[]) args[i];
                for (int j = 0; j < astr.length; j++){
                    if (astr[j] == null){
                        cm.insertBefore("$"+(i+1)+"["+j+"] = "+astr[j]+";");
                    }else{
                        cm.insertBefore("$"+(i+1)+"["+j+"] = \""+astr[j]+"\";");
                    }
                }
            } else{
                cm.insertBefore("$"+(i+1)+" = "+args[i]+";");
            }
        }

        ct.writeFile(System.getProperty("user.dir")+ File.separator+"JaBRO");
        ct.defrost();

        URL sootSource = getClass().getResource("/sootclasses-trunk-jar-with-dependencies.jar");
        File soot = new File(System.getProperty("user.dir")+File.separator+"sootclasses-trunk-jar-with-dependencies.jar");
        FileUtils.copyURLToFile(sootSource,soot);

        if (granularity == 'c'){
            Process p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp "
                    +dependencies+" -W -process-dir "+System.getProperty("user.dir")+File.separator+"JaBRO " + "New"+className);
            getMessage(p);
        } else if (granularity == 'm'){
            Process p = Runtime.getRuntime().exec("java -cp sootclasses-trunk-jar-with-dependencies.jar soot.Main -cp "+dependencies+" -O -process-dir "+System.getProperty("user.dir")+File.separator+"JaBRO " + "New"+className);
            getMessage(p);
        } else{
            System.out.println("invalid granularity");
            System.exit(1);
        }
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
