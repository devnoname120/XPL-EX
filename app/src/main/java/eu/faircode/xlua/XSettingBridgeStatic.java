package eu.faircode.xlua;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import eu.faircode.xlua.api.XCommandService;
import eu.faircode.xlua.api.standard.CallCommandHandler;
import eu.faircode.xlua.api.standard.QueryCommandHandler;
import eu.faircode.xlua.api.xlua.call.AssignHooksCommand;
import eu.faircode.xlua.api.xlua.call.ClearAppCommand;
import eu.faircode.xlua.api.xlua.call.ClearDataCommand;
import eu.faircode.xlua.api.xlua.call.GetGroupsCommand;
import eu.faircode.xlua.api.xlua.call.GetSettingCommand;
import eu.faircode.xlua.api.xlua.call.GetVersionCommand;
import eu.faircode.xlua.api.xlua.call.InitAppCommand;
import eu.faircode.xlua.api.xlua.call.PutHookCommand;
import eu.faircode.xlua.api.xlua.call.PutSettingCommand;
import eu.faircode.xlua.api.xlua.call.ReportCommand;
import eu.faircode.xlua.api.xlua.query.GetAppsCommand;
import eu.faircode.xlua.api.xlua.query.GetAssignedHooksCommand;
import eu.faircode.xlua.api.xlua.query.GetHooksCommand;
import eu.faircode.xlua.api.xlua.query.GetLogCommand;
import eu.faircode.xlua.api.xlua.query.GetSettingsCommand;
import eu.faircode.xlua.api.xmock.call.GetMockCpusCommand;
import eu.faircode.xlua.api.xmock.call.GetMockPropValueCommand;
import eu.faircode.xlua.api.xmock.call.PutGroupStateCommand;
import eu.faircode.xlua.api.xmock.call.PutMockConfigCommand;
import eu.faircode.xlua.api.xmock.call.PutMockCpuCommand;
import eu.faircode.xlua.api.xmock.query.GetMockConfigsCommand;
import eu.faircode.xlua.api.xmock.query.GetMockPropGroupsCommand;
import eu.faircode.xlua.utilities.BundleUtil;

public class XSettingBridgeStatic {
    private static final String TAG = "XLua.XSettingBridgeStatic";
    private static final String xLUA_PACKAGE = "eu.faircode.xlua";//pretty sure this is a string somewhere already

    private static final XCommandService service = new XCommandService();

    public static Bundle vxpCall(Context context, String arg, Bundle extras, String method) throws Exception { return service.executeCall(context, method, arg, extras, xLUA_PACKAGE); }
    public static Cursor vxpCursor(Context context, String arg, String[] selection, String method) throws Exception { return service.executeCursor(context, method, arg, selection, xLUA_PACKAGE); }

    public static void handleQuery(XC_MethodHook.MethodHookParam param, String packageName) {
        try {
            String[] projection = (String[]) param.args[1];
            String[] selection = (String[]) param.args[3];

            if (projection != null && projection.length > 0 && projection[0] != null) {
                //Hardcoded check for now
                if(!projection[0].startsWith("xlua.") && !projection[0].startsWith("mock."))
                    return;

                Method mGetContext = param.thisObject.getClass().getMethod("getContext");
                Context context = (Context) mGetContext.invoke(param.thisObject);

                String[] split = projection[0].split("\\.");
                String method = split[0];
                String arg = split[1];      //sub method being invoked like "getSettings"

                if(DebugUtil.isDebug())
                    Log.i(TAG , "query pkg=" + packageName + " method=" + method + " arg=" + arg);

                param.setResult(service.executeCursor(context, method, arg, selection, packageName));
            }
        }catch (Throwable e) {
            Log.e(TAG, "Query Error: \n" + e);
        }
    }

    public static void handeCall(XC_MethodHook.MethodHookParam param, String packageName)  {
        try {
            String method = (String) param.args[0];
            String arg = (String) param.args[1];        //sub method being invoked like "getSetting"
            Bundle extras = (Bundle) param.args[2];

            //hardcoded here for now
            if(!method.contains("xlua") && !method.contains("mock"))
                return;

            Method mGetContext = param.thisObject.getClass().getMethod("getContext");
            Context context = (Context) mGetContext.invoke(param.thisObject);

            if(DebugUtil.isDebug())
                Log.i(TAG , "call package=" + packageName + " method=" + method + " arg=" + arg);

            if(arg.equals("getVersion")) param.setResult(BundleUtil.createSingleInt("version", XLua.version));
            else param.setResult(service.executeCall(context, method, arg, extras, packageName));
        }catch (Exception e) {
            Log.e(TAG, "Call Error: \n" + e);
        }
    }

    public static Map<String, QueryCommandHandler> getXLuaQueryCommands() {
        HashMap<String, QueryCommandHandler> hs = new HashMap<>();
        hs.put("getApps", GetAppsCommand.create(false));
        hs.put("getApps2", GetAppsCommand.create(true));
        hs.put("getHooks", GetHooksCommand.create(false));
        hs.put("getHooks2", GetHooksCommand.create(true));
        hs.put("getSettings", GetSettingsCommand.create());
        hs.put("getAssignedHooks", GetAssignedHooksCommand.create(false));
        hs.put("getAssignedHooks2", GetAssignedHooksCommand.create(true));
        hs.put("getLog", GetLogCommand.create());
        return hs;
    }

    public static Map<String, QueryCommandHandler> getMockQueryCommands(){
        HashMap<String, QueryCommandHandler> hs = new HashMap<>();
        hs.put("getMockConfigs", GetMockConfigsCommand.create(false));
        hs.put("getMockConfigs2", GetMockConfigsCommand.create(true));
        hs.put("getMockPropGroups", GetMockPropGroupsCommand.create(false));
        hs.put("getMockPropGroups2", GetMockPropGroupsCommand.create(true));
        return hs;
    }

    public static Map<String, CallCommandHandler> getXLuaCallCommands() {
        HashMap<String, CallCommandHandler> hs = new HashMap<>();
        hs.put("assignHooks", AssignHooksCommand.create());
        hs.put("getVersion", GetVersionCommand.create());
        hs.put("putHook", PutHookCommand.create());
        hs.put("getGroups", GetGroupsCommand.create());
        hs.put("report", ReportCommand.create());
        hs.put("getSetting", GetSettingCommand.create());
        hs.put("putSetting", PutSettingCommand.create());
        hs.put("initApp", InitAppCommand.create());
        hs.put("clearApp", ClearAppCommand.create());
        hs.put("clearData", ClearDataCommand.create());
        return hs;
    }

    public static Map<String, CallCommandHandler> getXMockCallCommands() {
        HashMap<String, CallCommandHandler> hs = new HashMap<>();
        hs.put("getMockCpu", GetMockCpusCommand.create());
        hs.put("getMockCpus", GetMockCpusCommand.create());
        hs.put("putMockCpu", PutMockCpuCommand.create());
        //get
        hs.put("getMockPropValue", GetMockPropValueCommand.create());
        hs.put("putGroupState", PutGroupStateCommand.create());
        //hs.put("getMockProps", GetMockPropsCommand.create());
        //hs.put("putMockProp", PutMockPropCommand.create());
        //hs.put("putMockProps", PutMockPropsCommand.create());

        hs.put("putMockConfig", PutMockConfigCommand.create());
        return hs;
    }
}
