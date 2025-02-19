/*
    This file is part of XPrivacyLua.

    XPrivacyLua is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    XPrivacyLua is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XPrivacyLua.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2017-2019 Marcel Bokhorst (M66B)
 */

package eu.faircode.xlua;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.InputDevice;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

import de.robv.android.xposed.XC_MethodHook;
import eu.faircode.xlua.api.xmock.XMockCall;
import eu.faircode.xlua.interceptors.ShellIntercept;
import eu.faircode.xlua.utilities.MemoryUtilEx;
import eu.faircode.xlua.utilities.MockCpuUtil;
import eu.faircode.xlua.display.MotionRangeUtil;
import eu.faircode.xlua.utilities.FileUtil;
import eu.faircode.xlua.utilities.LuaLongUtil;
import eu.faircode.xlua.utilities.MemoryUtil;
import eu.faircode.xlua.utilities.NetworkUtil;
import eu.faircode.xlua.utilities.ReflectUtil;
import eu.faircode.xlua.utilities.StringUtil;
import eu.faircode.xlua.utilities.MockUtils;

public class XParam {
    private static final String TAG = "XLua.XParam";

    private final Context context;
    private final Field field;
    private final XC_MethodHook.MethodHookParam param;
    private final Class<?>[] paramTypes;
    private final Class<?> returnType;
    private final Map<String, String> settings;

    private static final Map<Object, Map<String, Object>> nv = new WeakHashMap<>();

    // Field param
    public XParam(
            Context context,
            Field field,
            Map<String, String> settings) {
        this.context = context;
        this.field = field;
        this.param = null;
        this.paramTypes = null;
        this.returnType = field.getType();
        this.settings = settings;
    }

    // Method param
    public XParam(
            Context context,
            XC_MethodHook.MethodHookParam param,
            Map<String, String> settings) {
        this.context = context;
        this.field = null;
        this.param = param;
        if (param.method instanceof Constructor) {
            this.paramTypes = ((Constructor) param.method).getParameterTypes();
            this.returnType = null;
        } else {
            this.paramTypes = ((Method) param.method).getParameterTypes();
            this.returnType = ((Method) param.method).getReturnType();
        }
        this.settings = settings;
    }

    //
    //Start of FILTER Functions
    //

    @SuppressWarnings("unused")
    public boolean isDriverFiles(String pathOrFile) { return FileUtil.isDeviceDriver(pathOrFile); }

    @SuppressWarnings("unused")
    public String filterBuildProperty(String property) {
        if(!StringUtil.isValidString(property) || MockUtils.isPropVxpOrLua(property))
            return MockUtils.NOT_BLACKLISTED;

        if(DebugUtil.isDebug())
            Log.i(TAG, "Filtering Property=" + property);

        //we can also use the local cached settings
        return XMockCall.getPropertyValue(getApplicationContext(), property, getPackageName(), 0);
    }

    @SuppressWarnings("unused")
    public boolean filterSettingsSecure(String setting) throws Throwable {
        Object arg = getArgument(1);
        if(arg == null)
            return false;

        if (!(arg instanceof String))
            return false;

        String set = (String)arg;

        switch (setting) {
            case "android_id":
                if(set.equals("android_id")) {
                    String fake = getSetting("value.android_id", "0000000000000000");
                    setResult(fake);
                    return true;
                }
                break;
            case "bluetooth_name":
                if(set.equals("bluetooth_name")) {
                    //setResult("00:00:00:00:00:00");//FIX
                    setResult(getSetting("bludtooth.id", "00:00:00:00:00:00"));
                    return true;
                }
                break;
        }

        return false;
    }

    //
    //End of FILTER Functions
    //

    @SuppressWarnings("unused")
    public boolean isDriverFile(String path) { return FileUtil.isDeviceDriver(path); }

    @SuppressWarnings("unused")
    public File[] filterFilesArray(File[] files) { return FileUtil.filterFileArray(files); }

    @SuppressWarnings("unused")
    public List<File> filterFilesList(List<File> files) { return FileUtil.filterFileList(files); }

    @SuppressWarnings("unused")
    public String[] filterFileStringArray(String[] files) { return FileUtil.filterArray(files); }

    @SuppressWarnings("unused")
    public List<String> filterFileStringList(List<String> files) { return FileUtil.filterList(files); }


    //
    //Shell Intercept
    //

    @SuppressWarnings("unused")
    public String interceptCommand(String command) { return ShellIntercept.interceptOne(command, getApplicationContext()); }

    @SuppressWarnings("unused")
    public String interceptCommandArray(String[] commands) { return ShellIntercept.interceptTwo(commands, getApplicationContext()); }

    @SuppressWarnings("unused")
    public String interceptCommandList(List<String> commands) { return ShellIntercept.interceptThree(commands, getApplicationContext()); }

    @SuppressWarnings("unused")
    public Process execEcho(String command) { return ShellIntercept.echo(command); }

    //
    //End of Shell Intercept
    //

    //
    //String Utils
    //

    @SuppressWarnings("unused")
    public String joinArray(String[] array) { return StringUtil.joinDelimiter(" ", array); }

    @SuppressWarnings("unused")
    public String joinList(List<String> list) { return StringUtil.joinDelimiter(" ", list); }

    //
    //End of String Utils
    //

    //
    //Start of Memory/CPU Functions
    //

    @SuppressWarnings("unused")
    public File createFakeMeminfoFile(int totalGigabytes, int availableGigabytes) { return FileUtil.generateFakeFile(MemoryUtil.generateFakeMeminfoContents(totalGigabytes, availableGigabytes)); }

    @SuppressWarnings("unused")
    public FileDescriptor createFakeMeminfoFileDescriptor(int totalGigabytes, int availableGigabytes) { return FileUtil.generateFakeFileDescriptor(MemoryUtil.generateFakeMeminfoContents(totalGigabytes, availableGigabytes)); }

    @SuppressWarnings("unused")
    public void populateMemoryInfo(ActivityManager.MemoryInfo memoryInfo, int totalMemoryInGB, int availableMemoryInGB) { MemoryUtilEx.populateMemoryInfo(memoryInfo, totalMemoryInGB, availableMemoryInGB); }

    @SuppressWarnings("unused")
    public ActivityManager.MemoryInfo getFakeMemoryInfo(int totalMemoryInGB, int availableMemoryInGB) { return MemoryUtilEx.getMemory(totalMemoryInGB, availableMemoryInGB); }

    @SuppressWarnings("unused")
    public FileDescriptor createFakeCpuinfoFileDescriptor() { return MockCpuUtil.generateFakeFileDescriptor(XMockCall.getSelectedMockCpu(getApplicationContext())); }

    @SuppressWarnings("unused")
    public File createFakeCpuinfoFile() { return MockCpuUtil.generateFakeFile(XMockCall.getSelectedMockCpu(getApplicationContext())); }

    //
    //End of Memory/CPU Functions
    //

    //
    //Start of Display Functions
    //

    @SuppressWarnings("unused")
    public InputDevice.MotionRange createXAxis(int height) { return MotionRangeUtil.createXAxis(height); }

    @SuppressWarnings("unused")
    public InputDevice.MotionRange createYAxis(int width) { return MotionRangeUtil.createYAxis(width); }

    //
    //End of Display Functions
    //

    //
    //Start of ETC Util Functions
    //

    @SuppressWarnings("unused")
    public String generateRandomString(int min, int max) { return generateRandomString(ThreadLocalRandom.current().nextInt(min, max + 1)); }

    @SuppressWarnings("unused")
    public String generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            stringBuilder.append(randomChar);
        }

        return stringBuilder.toString();
    }

    @SuppressWarnings("unused")
    public byte[] getIpAddressBytes(String ipAddress) { return NetworkUtil.stringIpAddressToBytes(ipAddress); }

    @SuppressWarnings("unused")
    public byte[] getFakeIpAddressBytes() { return NetworkUtil.stringIpAddressToBytes(getSetting("net.host_address")); }

    @SuppressWarnings("unused")
    public int getFakeIpAddressInt() { return NetworkUtil.stringIpAddressToInt(getSetting("net.host_address")); }

    @SuppressWarnings("unused")
    public byte[] getFakeMacAddressBytes() { return NetworkUtil.macStringToBytes(getSetting("net.mac")); }

    @SuppressWarnings("unused")
    public String gigabytesToBytesString(int gigabytes) { return Long.toString((long) gigabytes * 1073741824L); }

    //
    //End of ETC Util Functions
    //

    //
    //Start of Query / Call Functions
    //

    @SuppressWarnings("unused")
    public String[] extractSelectionArgs() {
        String[] sel = null;
        if(paramTypes[2].getName().equals(Bundle.class.getName())){
            Bundle bundle = (Bundle) getArgument(2);
            if(bundle != null) {
                sel = bundle.getStringArray("android:query-arg-sql-selection-args");
            }
        }
        else if(paramTypes[3].getName().equals(String[].class.getName()))
            sel = (String[]) getArgument(3);

        return sel;
    }

    @SuppressWarnings("unused")
    public boolean queryFilterAfter(String filter) throws Throwable {
        Uri uri = (Uri)getArgument(0);
        if(uri == null) return false;
        return queryFilterAfter(filter, uri);
    }

    @SuppressWarnings("unused")
    public boolean queryFilterAfter(String filter, Uri uri) throws Throwable {
        String authority = uri.getAuthority();
        Cursor ret = (Cursor) getResult();

        if(ret == null || authority == null)
            return false;

        switch (filter) {
            case "gsf_id":
                if(authority.equals("com.google.android.gsf.gservices")) {
                    String[] args = extractSelectionArgs();
                    if (args == null)
                        return false;

                    for(String arg : args) {
                        if(arg.equals("android_id")) {
                            setResult(new MatrixCursor(ret.getColumnNames()));
                            return true;
                        }
                    }
                }
                break;
        }

        return false;
    }

    //
    //End of Query / Call Functions
    //

    @SuppressWarnings("unused")
    public Context getApplicationContext() {
        return this.context;
    }

    @SuppressWarnings("unused")
    public String getPackageName() {
        return this.context.getPackageName();
    }

    @SuppressWarnings("unused")
    public int getUid() {
        return this.context.getApplicationInfo().uid;
    }

    @SuppressWarnings("unused")
    public Object getScope() {
        return this.param;
    }

    @SuppressWarnings("unused")
    public int getSDKCode() {
        return Build.VERSION.SDK_INT;
    }

    @SuppressWarnings("unused")
    public void printFileContents(String filePath) {
        FileUtil.printContents(filePath);
    }

    //
    //Start of REFLECT Functions
    //

    @SuppressWarnings("unused")
    public Class<Byte> getByteType() { return Byte.TYPE; }

    @SuppressWarnings("unused")
    public Class<Integer> getIntType() { return Integer.TYPE; }

    @SuppressWarnings("unused")
    public Class<Character> getCharType() { return Character.TYPE; }

    @SuppressWarnings("unused")
    public byte[] createByteArray(int size) { return new byte[size]; }

    @SuppressWarnings("unused")
    public int[] createIntArray(int size) { return new int[size]; }

    @SuppressWarnings("unused")
    public Character[] createCharArray(int size) { return new Character[size]; }

    @SuppressWarnings("unused")
    public boolean javaMethodExists(String className, String methodName) { return ReflectUtil.javaMethodExists(className, methodName); }

    @SuppressWarnings("unused")
    public Class<?> getClassType(String className) { return ReflectUtil.getClassType(className); }

    @SuppressWarnings("unused")
    public Object createReflectArray(String className, int size) { return ReflectUtil.createArray(className, size); }

    @SuppressWarnings("unused")
    public Object createReflectArray(Class<?> classType, int size) { return ReflectUtil.createArray(classType, size); }

    //
    //End of REFLECT Functions
    //


    //
    //START OF LONG HELPER FUNCTIONS
    //

    @SuppressWarnings("unused")
    public String getFieldLong(Object instance, String fieldName) { return Long.toString(LuaLongUtil.getFieldValue(instance, fieldName)); }

    @SuppressWarnings("unused")
    public void setFieldLong(Object instance, String fieldName, String longValue) { LuaLongUtil.setLongFieldValue(instance, fieldName, longValue); }

    @SuppressWarnings("unused")
    public void parcelWriteLong(Parcel parcel, String longValue) { LuaLongUtil.parcelWriteLong(parcel, longValue); }

    @SuppressWarnings("unused")
    public String parcelReadLong(Parcel parcel) { return LuaLongUtil.parcelReadLong(parcel); }

    @SuppressWarnings("unused")
    public void bundlePutLong(Bundle bundle, String key, String longValue) { LuaLongUtil.bundlePutLong(bundle, key, longValue); }

    @SuppressWarnings("unused")
    public String bundleGetLong(Bundle bundle, String key) { return LuaLongUtil.bundleGetLong(bundle, key); }

    @SuppressWarnings("unused")
    public void setResultToLong(String long_value) throws Throwable {
        try {
            setResult(Long.parseLong(long_value));
        }catch (Exception e) { Log.e(TAG, "Failed to set Result as Long, make sure its a NUMERIC Value: " + e); }
    }

    @SuppressWarnings("unused")
    public String getResultLong() throws Throwable {
        try {
            return Long.toString((long)getResult());
        }catch (Exception e) {
            Log.e(TAG, "Failed Get Result as Long String:\n" + e + "\n" + Log.getStackTraceString(e));
            return "0";
        }
    }

    public void printStackTrace() {

    }

    public void printStackTraceEx() {

    }

    //
    //END OF LONG HELPER FUNCTIONS
    //

    @SuppressWarnings("unused")
    public Object getThis() {
        if (this.field == null)
            return this.param.thisObject;
        else
            return null;
    }

    @SuppressWarnings("unused")
    public Object getArgument(int index) {
        if (index < 0 || index >= this.paramTypes.length)
            throw new ArrayIndexOutOfBoundsException("Argument #" + index);
        return this.param.args[index];
    }

    @SuppressWarnings("unused")
    public void setArgumentString(int index, Object value) { setArgument(index, (String)String.valueOf(value)); }

    @SuppressWarnings("unused")
    public void setArgumentString(int index, String value) { setArgument(index, value); }

    @SuppressWarnings("unused")
    public void setArgument(int index, Object value) {
        if (index < 0 || index >= this.paramTypes.length)
            throw new ArrayIndexOutOfBoundsException("Argument #" + index);

        if (value != null) {
            value = coerceValue(this.paramTypes[index], value);
            if (!boxType(this.paramTypes[index]).isInstance(value))
                throw new IllegalArgumentException(
                        "Expected argument #" + index + " " + this.paramTypes[index] + " got " + value.getClass());
        }

        this.param.args[index] = value;
    }

    @SuppressWarnings("unused")
    public Throwable getException() {
        Throwable ex = (this.field == null ? this.param.getThrowable() : null);
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Get " + this.getPackageName() + ":" + this.getUid() + " result=" + ex.getMessage());
        return ex;
    }

    @SuppressWarnings("unused")
    public Object getResult() throws Throwable {
        Object result = (this.field == null ? this.param.getResult() : this.field.get(null));
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Get " + this.getPackageName() + ":" + this.getUid() + " result=" + result);
        return result;
    }

    @SuppressWarnings("unused")
    public void setResultString(Object result) throws Throwable { setResult(String.valueOf(result)); }

    @SuppressWarnings("unused")
    public void setResultString(String result) throws Throwable { setResult(result); }

    @SuppressWarnings("unused")
    public void setResultByteArray(Byte[] result) throws Throwable { setResult(result); }

    @SuppressWarnings("unused")
    public void setResult(Object result) throws Throwable {
        //Do Note they have support if you pass a LONG String
        //
        if (BuildConfig.DEBUG)
            Log.i(TAG, "Set " + this.getPackageName() + ":" + this.getUid() +
                    " result=" + result + " return=" + this.returnType);
        if (result != null && !(result instanceof Throwable) && this.returnType != null) {
            result = coerceValue(this.returnType, result);
            if (!boxType(this.returnType).isInstance(result))
                throw new IllegalArgumentException(
                        "Expected return " + this.returnType + " got " + result.getClass());
        }

        if (this.field == null)
            if (result instanceof Throwable)
                this.param.setThrowable((Throwable) result);
            else
                this.param.setResult(result);
        else
            this.field.set(null, result);
    }

    @SuppressWarnings("unused")
    public int getSettingInt(String name, int defaultValue) {
        String setting = getSetting(name);
        if(!StringUtil.isValidString(setting))
            return defaultValue;

        try {
            return Integer.parseInt(setting);
        }catch (Exception e) {
            Log.e(TAG, "Invalid Numeric Input::\n", e);
            return defaultValue;
        }
    }

    @SuppressWarnings("unused")
    public String getSetting(String name, String defaultValue) {
        String setting = getSetting(name);
        return StringUtil.isValidString(setting) ? setting : defaultValue;
    }

    @SuppressWarnings("unused")
    public String getSetting(String name) {
        synchronized (this.settings) {
            String value = (this.settings.containsKey(name) ? this.settings.get(name) : null);
            Log.i(TAG, "Get setting " + this.getPackageName() + ":" + this.getUid() + " " + name + "=" + value);
            return value;
        }
    }

    @SuppressWarnings("unused")
    public void putValue(String name, Object value, Object scope) {
        Log.i(TAG, "Put value " + this.getPackageName() + ":" + this.getUid() + " " + name + "=" + value + " @" + scope);
        synchronized (nv) {
            if (!nv.containsKey(scope))
                nv.put(scope, new HashMap<String, Object>());
            nv.get(scope).put(name, value);
        }
    }

    @SuppressWarnings("unused")
    public Object getValue(String name, Object scope) {
        Object value = getValueInternal(name, scope);
        Log.i(TAG, "Get value " + this.getPackageName() + ":" + this.getUid() + " " + name + "=" + value + " @" + scope);
        return value;
    }

    private static Object getValueInternal(String name, Object scope) {
        synchronized (nv) {
            if (!nv.containsKey(scope))
                return null;
            if (!nv.get(scope).containsKey(name))
                return null;
            return nv.get(scope).get(name);
        }
    }

    private static Class<?> boxType(Class<?> type) {
        if (type == boolean.class)
            return Boolean.class;
        else if (type == byte.class)
            return Byte.class;
        else if (type == char.class)
            return Character.class;
        else if (type == short.class)
            return Short.class;
        else if (type == int.class)
            return Integer.class;
        else if (type == long.class)
            return Long.class;
        else if (type == float.class)
            return Float.class;
        else if (type == double.class)
            return Double.class;
        return type;
    }

    private static Object coerceValue(Class<?> type, Object value) {
        // TODO: check for null primitives

        // Lua 5.2 auto converts numbers into floating or integer values
        if (Integer.class.equals(value.getClass())) {
            if (long.class.equals(type))
                return (long) (int) value;
            else if (float.class.equals(type))
            return (float) (int) value;
        else if (double.class.equals(type))
            return (double) (int) value;
    } else if (Double.class.equals(value.getClass())) {
        if (float.class.equals(type))
            return (float) (double) value;
    } else if (value instanceof String && int.class.equals(type))
            return Integer.parseInt((String) value);
        else if (value instanceof String && long.class.equals(type))
            return Long.parseLong((String) value);
        else if (value instanceof String && float.class.equals(type))
            return Float.parseFloat((String) value);
        else if (value instanceof String && double.class.equals(type))
            return Double.parseDouble((String) value);

        return value;
    }
}
