import soot.Scene;
import soot.SootClass;
import soot.PackManager;
import soot.Transform;
import soot.G;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Required 2 arguments: APK path and path to Android SDKs");
        }
        String apk = args[0];
        String android_sdks = args[1];

        String[] opts = {
                "-process-dir", apk,
                "-android-jars", android_sdks,
                "-ire",
                "-allow-phantom-refs",
                "-process-multiple-dex",
                "-android-api-version", "26",
                "-src-prec", "apk",
                "-w",
                "-p", "cg", "enabled:false",
                "-p", "jop.cpf", "enabled:true",
                "-output-format", "dex"
        };
        G.reset();
        Instrumentor instrumentor = new Instrumentor();
        Scene.v().addBasicClass("android.util.Log", SootClass.SIGNATURES);
        PackManager.v().getPack("jtp").add(new Transform("jtp.Instrumentor", instrumentor));
        soot.Main.main(opts);
    }
}
