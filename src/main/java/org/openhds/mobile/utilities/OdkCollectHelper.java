package org.openhds.mobile.utilities;

import static org.openhds.mobile.provider.InstanceProviderAPI.InstanceColumns.CONTENT_URI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openhds.mobile.provider.InstanceProviderAPI;
import org.openhds.mobile.model.form.FormInstance;
import org.openhds.mobile.repository.Query;

import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class OdkCollectHelper {

    public static List<FormInstance> getAllUnsentFormInstances(ContentResolver resolver) {

        ArrayList<FormInstance> formInstances = new ArrayList<>();
        Cursor cursor = resolver.query(CONTENT_URI, new String[]{
                        InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH,
                        InstanceProviderAPI.InstanceColumns._ID,
                        InstanceProviderAPI.InstanceColumns.JR_FORM_ID,
                        InstanceProviderAPI.InstanceColumns.DISPLAY_NAME,
                        InstanceProviderAPI.InstanceColumns.STATUS}, InstanceProviderAPI.InstanceColumns.STATUS + " != ?",
                new String[]{InstanceProviderAPI.STATUS_SUBMITTED}, null);

        if (null == cursor) {
            return null;
        }
        while (cursor.moveToNext()) {
            FormInstance formInstance = new FormInstance();
            String filePath, formName, fileName;
            filePath = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
            Uri uri = Uri.withAppendedPath(CONTENT_URI, cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID)));
            formName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID));
            fileName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME));
            formInstance.setFilePath(filePath);
            formInstance.setUriString(uri.toString());
            formInstance.setFormName(formName);
            formInstance.setFileName(fileName);
            formInstances.add(formInstance);
        }
        cursor.close();
        return formInstances;
    }

    public static List<FormInstance> getAllFormInstances(ContentResolver resolver) {

        ArrayList<FormInstance> formInstances = new ArrayList<>();
        Cursor cursor = resolver.query(CONTENT_URI, new String[]{
                InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH,
                InstanceProviderAPI.InstanceColumns._ID,
                InstanceProviderAPI.InstanceColumns.JR_FORM_ID,
                InstanceProviderAPI.InstanceColumns.DISPLAY_NAME}, null, null, null);

        if (null == cursor) {
            return null;
        }

        while (cursor.moveToNext()) {
            FormInstance formInstance = new FormInstance();
            String filePath, formName, fileName;
            filePath = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
            Uri uri = Uri.withAppendedPath(CONTENT_URI, cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID)));
            formName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID));
            fileName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME));
            formInstance.setFilePath(filePath);
            formInstance.setFormName(formName);
            formInstance.setFileName(fileName);
            formInstance.setUriString(uri.toString());
            formInstances.add(formInstance);
        }
        cursor.close();
        return formInstances;
    }

    public static void deleteInstance(ContentResolver resolver, Uri uri, String filePath) {
        resolver.delete(uri, InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH + "=?", new String[]{filePath});
    }

    public static void setStatusSubmitted(ContentResolver resolver, Uri uri) {

        ContentValues cv = new ContentValues();
        cv.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.STATUS_SUBMITTED);
        resolver.update(uri, cv, null, null);

    }

    public static void setStatusIncomplete(ContentResolver resolver, Uri uri) {

        ContentValues cv = new ContentValues();
        cv.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.STATUS_INCOMPLETE);
        resolver.update(uri, cv, null, null);

    }

    public static void setStatusComplete(ContentResolver resolver, Uri uri) {

        ContentValues cv = new ContentValues();
        cv.put(InstanceProviderAPI.InstanceColumns.STATUS, InstanceProviderAPI.STATUS_COMPLETE);
        resolver.update(uri, cv, null, null);

    }

    public static String makePlaceholders(int len) {
        if (len < 1) {
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    public static List<FormInstance> getByPaths(ContentResolver resolver, Collection<String> ids) {
        ArrayList<FormInstance> formInstances = new ArrayList<>();
        Cursor cursor = resolver.query(CONTENT_URI, new String[]{
                        InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH,
                        InstanceProviderAPI.InstanceColumns._ID,
                        InstanceProviderAPI.InstanceColumns.JR_FORM_ID,
                        InstanceProviderAPI.InstanceColumns.DISPLAY_NAME,
                        InstanceProviderAPI.InstanceColumns.STATUS},
                InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH
                        + " IN (" + makePlaceholders(ids.size()) + ")",
                ids.toArray(new String[ids.size()]), null);

        if (null == cursor) {
            return null;
        }

        while (cursor.moveToNext()) {
            FormInstance formInstance = new FormInstance();
            String filePath, formName, fileName;
            filePath = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
            Uri uri = Uri.withAppendedPath(CONTENT_URI, cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID)));
            formName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.JR_FORM_ID));
            fileName = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME));
            formInstance.setFilePath(filePath);
            formInstance.setUriString(uri.toString());
            formInstance.setFormName(formName);
            formInstance.setFileName(fileName);
            formInstances.add(formInstance);
        }
        cursor.close();
        return formInstances;
    }

    public static List<String> getSentFormPaths(ContentResolver resolver, Collection<String> ids) {
        ArrayList<String> sentPaths = new ArrayList<>();
        for (String path : ids) {
            Query query = new Query(CONTENT_URI,
                    new String[]{InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH, InstanceProviderAPI.InstanceColumns.STATUS},
                    new String[]{path, InstanceProviderAPI.STATUS_SUBMITTED}, null, "=");
            Cursor cursor = query.select(resolver);
            if (null == cursor) {
                return null;
            }
            if (cursor.moveToFirst()) {
                String sentPath;
                sentPath = cursor.getString(cursor.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
                sentPaths.add(sentPath);
            }
            cursor.close();
        }
        return sentPaths;
    }
}
