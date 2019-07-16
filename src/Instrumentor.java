import soot.*;
import soot.jimple.*;

import java.util.Iterator;
import java.util.Map;

public class Instrumentor extends BodyTransformer {
    private static final String TAG = "Tracker";
    private static final String[] commonLibs = {
            "android.support", "com.google", "android.arch", "androidx",
            "com.appsflyer", "io.fabric", "com.umeng", "com.flurry", "com.crashlytics", "com.airbnb", "com.tencent",
            "com.facebook", "com.getui", "cn.com.chinatelecom", "okhttp3", "okio", "org.greenrobot",
            "com.meitu.library.analytics", "com.meitu.business.ads", "com.meitu.hubble", "com.meitu.pug",
            "com.meitu.pushkit", "com.meitu.webcore", "com.meitu.webcore"
    };

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        String clsName = b.getMethod().getDeclaringClass().getName();
        String methodName = b.getMethod().getName();
        if (filter(b, clsName)) {
            return;
        }
        if (!skipSelfValidation(b, clsName, methodName) && !outputFaceNumLog(b, clsName, methodName)) {
            addLogStmts(b);
        }

    }

    private boolean filter(Body b, String clsName) {
        boolean filtered = false;
        if (b.getMethod().isNative() || b.getMethod().isPhantom() || b.getMethod().isAbstract() ||
                b.getMethod().isConstructor() || b.getMethod().isStaticInitializer()) {
            filtered = true;
        } else {
            for (String lib : commonLibs) {
                if (clsName.startsWith(lib)) {
                    filtered = true;
                }
            }
        }
        return filtered;
    }

    private boolean skipSelfValidation(Body b, String clsName, String methodName) {
        boolean skipped = false;
        if (clsName.equals("com.mt.util.tools.AppTools") && methodName.equals("isApplicationLegal")) {
            Stmt returnStmt = Jimple.v().newReturnStmt(IntConstant.v(1));
            PatchingChain<Unit> units = b.getUnits();
            Unit point = getInsertPoint(units);
            units.insertBefore(returnStmt, point);
//            b.validate();
           skipped = true;
        }
        return skipped;
    }

    private boolean outputFaceNumLog(Body b, String clsName, String methodName) {
        boolean skipped = false;
        if (clsName.equals("com.meitu.face.detect.MTFaceDetector") && methodName.equals("isEnableDetectUseTimePrint")) {
            Stmt returnTrue = Jimple.v().newReturnStmt(IntConstant.v(1));
            PatchingChain<Unit> units = b.getUnits();
            Unit point = getInsertPoint(units);
            units.insertBefore(returnTrue, point);
//            b.validate();
            skipped = true;
        }
        return skipped;
    }

    private void addLogStmts(Body methodBody) {
        String signature = methodBody.getMethod().getSignature();
        PatchingChain<Unit> units = methodBody.getUnits();
        SootMethod logd = Scene.v().getSootClass("android.util.Log")
                .getMethod("int d(java.lang.String,java.lang.String)");
        Local tagStr = Jimple.v().newLocal("tagStr", RefType.v("java.lang.String"));
        Local msgStr = Jimple.v().newLocal("msgStr", RefType.v("java.lang.String"));
        methodBody.getLocals().add(tagStr);
        methodBody.getLocals().add(msgStr);
        Unit insertPoint = getInsertPoint(units);
        units.insertBefore(Jimple.v().newAssignStmt(tagStr, StringConstant.v(TAG)), insertPoint);
        units.insertBefore(Jimple.v().newAssignStmt(msgStr, StringConstant.v(signature)), insertPoint);
        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logd.makeRef(), tagStr, msgStr)), insertPoint);
//        methodBody.validate();
    }

    private Unit getInsertPoint(PatchingChain<Unit> units) {
        Unit point = null;
        for (Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext(); ) {
            Stmt u = (Stmt) iter.next();
            if (!(u instanceof IdentityStmt)) {
                point = u;
                break;
            }
        }
        return point;
    }
}
