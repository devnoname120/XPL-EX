package eu.faircode.xlua.api.xlua.query;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import eu.faircode.xlua.XDatabase;
import eu.faircode.xlua.XUtil;
import eu.faircode.xlua.api.XProxyContent;
import eu.faircode.xlua.api.standard.QueryCommandHandler;
import eu.faircode.xlua.api.standard.command.QueryPacket;
import eu.faircode.xlua.api.standard.database.SqlQuerySnake;

import eu.faircode.xlua.api.settings.XLuaLuaSetting;


public class GetSettingsCommand extends QueryCommandHandler {
    public static GetSettingsCommand create() { return new GetSettingsCommand(); };

    public GetSettingsCommand() {
        name = "getSettings";
        requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        MatrixCursor result = new MatrixCursor(new String[]{"name", "value"});
        String[] selection = commandData.getSelection();
        XDatabase db = commandData.getDatabase();

        if(selection == null || selection.length == 0)
            return result;

        String packageName = selection[0];
        int uid = Integer.parseInt(selection[1]);
        int userid = XUtil.getUserId(uid);

        SqlQuerySnake snake = SqlQuerySnake
                .create(db, XLuaLuaSetting.Table.name)
                .whereColumn("user", Integer.toString(userid))
                .whereColumn("category", packageName)
                .onlyReturnColumns("name", "value");

        db.readLock();
        Cursor c = snake.query();
        try {
            while (c.moveToNext())
                result.addRow(new String[]{c.getString(0), c.getString(1)});
        }finally {
            snake.clean(c);
            db.readUnlock();
        }

        return result;
    }

    public static Cursor invoke(Context context, String packageOrName, int uid) {
        SqlQuerySnake snake = SqlQuerySnake
                .create()
                .whereColumn("pkg", packageOrName)
                .whereColumn("uid", uid);

        return XProxyContent.luaQuery(
                context,
                "getSettings",
                snake.getSelectionCompareValues(),
                snake.getSelectionArgs());
    }
}
