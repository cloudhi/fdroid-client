package org.fdroid.fdroid;

import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import junit.framework.AssertionFailedError;
import mock.MockInstallablePackageManager;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtils {

    private static final String TAG = "org.fdroid.fdroid.TestUtils";

    public static <T extends Comparable> void assertContainsOnly(List<T> actualList, T[] expectedArray) {
        List<T> expectedList = new ArrayList<T>(expectedArray.length);
        Collections.addAll(expectedList, expectedArray);
        assertContainsOnly(actualList, expectedList);
    }

    public static <T extends Comparable> void assertContainsOnly(T[] actualArray, List<T> expectedList) {
        List<T> actualList = new ArrayList<T>(actualArray.length);
        Collections.addAll(actualList, actualArray);
        assertContainsOnly(actualList, expectedList);
    }

    public static <T extends Comparable> void assertContainsOnly(T[] actualArray, T[] expectedArray) {
        List<T> expectedList = new ArrayList<T>(expectedArray.length);
        Collections.addAll(expectedList, expectedArray);
        assertContainsOnly(actualArray, expectedList);
    }

    public static <T> String listToString(List<T> list) {
        String string = "[";
        for (int i = 0; i < list.size(); i ++) {
            if (i > 0) {
                string += ", ";
            }
            string += "'" + list.get(i) + "'";
        }
        string += "]";
        return string;
    }

    public static <T extends Comparable> void assertContainsOnly(List<T> actualList, List<T> expectedContains) {
        if (actualList.size() != expectedContains.size()) {
            String message =
                "List sizes don't match.\n" +
                "Expected: " +
                listToString(expectedContains) + "\n" +
                "Actual:   " +
                listToString(actualList);
            throw new AssertionFailedError(message);
        }
        for (T required : expectedContains) {
            boolean containsRequired = false;
            for (T itemInList : actualList) {
                if (required.equals(itemInList)) {
                    containsRequired = true;
                    break;
                }
            }
            if (!containsRequired) {
                String message =
                    "List doesn't contain \"" + required + "\".\n" +
                    "Expected: " +
                    listToString(expectedContains) + "\n" +
                    "Actual:   " +
                    listToString(actualList);
                throw new AssertionFailedError(message);
            }
        }
    }

    public static void insertApp(ContentResolver resolver, String appId, String name) {
        insertApp(resolver, appId, name, new ContentValues());
    }

    public static void insertApp(ContentResolver resolver, String id, String name, ContentValues additionalValues) {

        ContentValues values = new ContentValues();
        values.put(AppProvider.DataColumns.APP_ID, id);
        values.put(AppProvider.DataColumns.NAME, name);

        // Required fields (NOT NULL in the database).
        values.put(AppProvider.DataColumns.SUMMARY, "test summary");
        values.put(AppProvider.DataColumns.DESCRIPTION, "test description");
        values.put(AppProvider.DataColumns.LICENSE, "GPL?");
        values.put(AppProvider.DataColumns.IS_COMPATIBLE, 1);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, 0);

        values.putAll(additionalValues);

        Uri uri = AppProvider.getContentUri();

        resolver.insert(uri, values);
    }

    public static Uri insertApk(FDroidProviderTest<ApkProvider> providerTest, String id, int versionCode) {
        return insertApk(providerTest, id, versionCode, new ContentValues());
    }

    public static Uri insertApk(FDroidProviderTest<ApkProvider> providerTest, String id, int versionCode, ContentValues additionalValues) {

        ContentValues values = new ContentValues();

        values.put(ApkProvider.DataColumns.APK_ID, id);
        values.put(ApkProvider.DataColumns.VERSION_CODE, versionCode);

        // Required fields (NOT NULL in the database).
        values.put(ApkProvider.DataColumns.REPO_ID, 1);
        values.put(ApkProvider.DataColumns.VERSION, "The good one");
        values.put(ApkProvider.DataColumns.HASH, "11111111aaaaaaaa");
        values.put(ApkProvider.DataColumns.NAME, "Test Apk");
        values.put(ApkProvider.DataColumns.SIZE, 10000);
        values.put(ApkProvider.DataColumns.IS_COMPATIBLE, 1);

        values.putAll(additionalValues);

        Uri uri = ApkProvider.getContentUri();

        return providerTest.getMockContentResolver().insert(uri, values);
    }

    /**
     * Will tell {@code pm} that we are installing {@code appId}, and then alert the
     * {@link org.fdroid.fdroid.PackageAddedReceiver}. This will in turn update the
     * "installed apps" table in the database.
     *
     * Note: in order for this to work, the {@link AppProviderTest#getSwappableContext()}
     * will need to be aware of the package manager that we have passed in. Therefore,
     * you will have to have called
     * {@link mock.MockContextSwappableComponents#setPackageManager(android.content.pm.PackageManager)}
     * on the {@link AppProviderTest#getSwappableContext()} before invoking this method.
     */
    public static void installAndBroadcast(
            Context context,  MockInstallablePackageManager pm,
            String appId, int versionCode, String versionName) {

        pm.install(appId, versionCode, versionName);
        Intent installIntent = new Intent(Intent.ACTION_PACKAGE_ADDED);
        installIntent.setData(Uri.parse("package:" + appId));
        new PackageAddedReceiver().onReceive(context, installIntent);

    }

    /**
     * @see org.fdroid.fdroid.TestUtils#installAndBroadcast(android.content.Context context, mock.MockInstallablePackageManager, String, int, String)
     */
    public static void upgradeAndBroadcast(
            Context context, MockInstallablePackageManager pm,
            String appId, int versionCode, String versionName) {
        /*
        removeAndBroadcast(context, pm, appId);
        installAndBroadcast(context, pm, appId, versionCode, versionName);
        */
        pm.install(appId, versionCode, versionName);
        Intent installIntent = new Intent(Intent.ACTION_PACKAGE_CHANGED);
        installIntent.setData(Uri.parse("package:" + appId));
        new PackageUpgradedReceiver().onReceive(context, installIntent);

    }

    /**
     * @see org.fdroid.fdroid.TestUtils#installAndBroadcast(android.content.Context context, mock.MockInstallablePackageManager, String, int, String)
     */
    public static void removeAndBroadcast(Context context, MockInstallablePackageManager pm, String appId) {

        pm.remove(appId);
        Intent installIntent = new Intent(Intent.ACTION_PACKAGE_REMOVED);
        installIntent.setData(Uri.parse("package:" + appId));
        new PackageRemovedReceiver().onReceive(context, installIntent);

    }

    public static File copyAssetToDir(Context context, String assetName, File directory) {
        File tempFile;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(assetName + "-", ".testasset", directory);
            Log.d(TAG, "Copying asset file " + assetName + " to directory " + directory);
            input = context.getResources().getAssets().open(assetName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;
    }

    /**
     * Prefer internal over external storage, because external tends to be FAT filesystems,
     * which don't support symlinks (which we test using this method).
     */
    public static File getWriteableDir(Instrumentation instrumentation) {
        Context context = instrumentation.getContext();
        Context targetContext = instrumentation.getTargetContext();
        File dir = context.getCacheDir();
        Log.d(TAG, "Looking for writeable dir, trying context.getCacheDir()" );
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying context.getFilesDir()");
            dir = context.getFilesDir();
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying targetContext.getCacheDir()");
            dir = targetContext.getCacheDir();
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying targetContext.getFilesDir()");
            dir = targetContext.getFilesDir();
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying context.getExternalCacheDir()");
            dir = context.getExternalCacheDir();
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying context.getExternalFilesDir(null)");
            dir = context.getExternalFilesDir(null);
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying targetContext.getExternalCacheDir()");
            dir = targetContext.getExternalCacheDir();
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying targetContext.getExternalFilesDir(null)");
            dir = targetContext.getExternalFilesDir(null);
        }
        if (dir == null || !dir.canWrite()) {
            Log.d(TAG, "Looking for writeable dir, trying Environment.getExternalStorageDirectory()");
            dir = Environment.getExternalStorageDirectory();
        }
        Log.d(TAG, "Writeable dir found: " + dir);
        return dir;
    }
}
