package eu.faircode.xlua.api.standard;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.Nullable;

import eu.faircode.xlua.BuildConfig;
import eu.faircode.xlua.XSecurity;
import eu.faircode.xlua.api.standard.command.QueryPacket;

public abstract class QueryCommandHandler {
    public String name;
    public String parent;
    public boolean requiresPermissionCheck;

    public QueryCommandHandler() { }
    public QueryCommandHandler(String name, boolean requirePermissionCheck) {
        this.name = name;
        this.requiresPermissionCheck = requirePermissionCheck;
    }

    public abstract Cursor handle(QueryPacket commandData) throws Throwable;

    public void throwOnPermissionCheck(Context context) {
        if(BuildConfig.DEBUG) Log.i("XLua.QueryCommandHandler", "CHECKING COMMAND SECURITY name=" + name);
        if(!requiresPermissionCheck) XSecurity.checkCaller(context);
    }
    public String getName() { return name; }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof String))
            return false;
        String other = (String) obj;
        return this.getName().equals(other);
    }
}
