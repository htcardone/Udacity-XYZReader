package com.example.xyzreader.data;

import android.database.Cursor;

public class CursorRepository {
    private static Cursor mCursor;

    public static Cursor getCursor() {
        return mCursor;
    }

    public static void setCursor(Cursor cursor) {
        mCursor = cursor;
    }
}
