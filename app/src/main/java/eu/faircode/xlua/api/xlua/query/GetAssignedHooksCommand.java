package eu.faircode.xlua.api.xlua.query;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import java.util.List;

import eu.faircode.xlua.XDatabase;
import eu.faircode.xlua.XGlobalCore;
import eu.faircode.xlua.XUtil;
import eu.faircode.xlua.api.XProxyContent;
import eu.faircode.xlua.api.standard.QueryCommandHandler;
import eu.faircode.xlua.api.standard.command.QueryPacket;
import eu.faircode.xlua.api.hook.assignment.XLuaAssignment;
import eu.faircode.xlua.api.standard.database.SqlQuerySnake;
import eu.faircode.xlua.api.xlua.provider.XLuaHookProvider;

public class GetAssignedHooksCommand extends QueryCommandHandler {
    public static GetAssignedHooksCommand create(boolean marshall) { return new GetAssignedHooksCommand(marshall); };

    private boolean marshall;
    public GetAssignedHooksCommand(boolean marshall) {
        name = marshall ? "getAssignedHooks" : "getAssignedHooks2";
        this.marshall = marshall;
        requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        //This is fine tho make sure marshall works properly
        MatrixCursor result = new MatrixCursor(new String[]{marshall ? "blob" : "json", "used"});
        String[] selection = commandData.getSelection();
        XDatabase db = commandData.getDatabase();

        if(selection == null || selection.length == 0)
            return result;

        String packageName = selection[0];
        int uid = Integer.parseInt(selection[1]);

        List<String> collection = XLuaHookProvider.getCollections(db, XUtil.getUserId(uid));
        SqlQuerySnake snake = SqlQuerySnake
                .create(db, XLuaAssignment.Table.name)
                .whereColumn("package", packageName)
                .whereColumn("uid", uid)
                .onlyReturnColumns("hook", "used")
                .orderBy("hook");

        db.readLock();
        Cursor c = snake.query();
        try {
            int colHook = c.getColumnIndex("hook");
            int colUsed = c.getColumnIndex("used");
            while (c.moveToNext()) {
                String hook_id = c.getString(colHook);
                String used = c.getString(colUsed);
                XGlobalCore.writeHookFromCache(result, hook_id, used, packageName, collection, marshall);
            }
        }finally {
            snake.clean(c);
            db.readUnlock();
        }

        return result;
    }

    public static Cursor invoke(Context context, String packageName, int uid) {
        SqlQuerySnake snake = SqlQuerySnake
                .create()
                .whereColumn("pkg", packageName)
                .whereColumn("uid", uid);

        return XProxyContent.luaQuery(context, "getAssignedHooks", snake.getSelectionCompareValues(), snake.getSelectionArgs());
    }

    public static Cursor invokeEx(Context context, String packageName, int uid) {
        SqlQuerySnake snake = SqlQuerySnake
                .create()
                .whereColumn("pkg", packageName)
                .whereColumn("uid", uid);

        return XProxyContent.luaQuery(context, "getAssignedHooks2", snake.getSelectionCompareValues(), snake.getSelectionArgs());
    }
}
