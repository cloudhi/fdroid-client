
package org.fdroid.fdroid.updater;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.TestUtils;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.updater.RepoUpdater.UpdateException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@TargetApi(8)
public class SignedRepoUpdaterTest extends InstrumentationTestCase {
    private static final String TAG = "SignedRepoUpdaterTest";

    private Context context;
    private RepoUpdater repoUpdater;
    private File testFilesDir;

    String simpleIndexPubkey = "308201ee30820157a0030201020204300d845b300d06092a864886f70d01010b0500302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e301e170d3134303432373030303633315a170d3431303931323030303633315a302a3110300e060355040b1307462d44726f6964311630140603550403130d70616c6174736368696e6b656e30819f300d06092a864886f70d010101050003818d0030818902818100a439472e4b6d01141bfc94ecfe131c7c728fdda670bb14c57ca60bd1c38a8b8bc0879d22a0a2d0bc0d6fdd4cb98d1d607c2caefbe250a0bd0322aedeb365caf9b236992fac13e6675d3184a6c7c6f07f73410209e399a9da8d5d7512bbd870508eebacff8b57c3852457419434d34701ccbf692267cbc3f42f1c5d1e23762d790203010001a321301f301d0603551d0e041604140b1840691dab909746fde4bfe28207d1cae15786300d06092a864886f70d01010b05000381810062424c928ffd1b6fd419b44daafef01ca982e09341f7077fb865905087aeac882534b3bd679b51fdfb98892cef38b63131c567ed26c9d5d9163afc775ac98ad88c405d211d6187bde0b0d236381cc574ba06ef9080721a92ae5a103a7301b2c397eecc141cc850dd3e123813ebc41c59d31ddbcb6e984168280c53272f6a442b";

    @Override
    protected void setUp() {
        context = getInstrumentation().getContext();
        testFilesDir = TestUtils.getWriteableDir(getInstrumentation());
        Repo repo = new Repo();
        repo.pubkey = this.simpleIndexPubkey;
        repoUpdater = RepoUpdater.createUpdaterFor(context, repo);
    }

    public void testExtractIndexFromJar() {
        if (!testFilesDir.canWrite())
            return;
        File simpleIndexXml = TestUtils.copyAssetToDir(context, "simpleIndex.xml", testFilesDir);
        File simpleIndexJar = TestUtils.copyAssetToDir(context, "simpleIndex.jar", testFilesDir);
        File testFile = null;

        // these are supposed to succeed
        try {
            testFile = repoUpdater.getIndexFromFile(simpleIndexJar);
            assertTrue(testFile.length() == simpleIndexXml.length());
            assertEquals(FileUtils.readFileToString(testFile),
                    FileUtils.readFileToString(simpleIndexXml));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testExtractIndexFromJarWithoutSignatureJar() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "simpleIndexWithoutSignature.jar", testFilesDir));
            fail();
        } catch (UpdateException e) {
            // success!
        }
    }

    public void testExtractIndexFromJarWithCorruptedManifestJar() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedManifest.jar", testFilesDir));
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    public void testExtractIndexFromJarWithCorruptedSignature() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedSignature.jar", testFilesDir));
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    public void testExtractIndexFromJarWithCorruptedCertificate() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedCertificate.jar", testFilesDir));
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    public void testExtractIndexFromJarWithCorruptedEverything() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "simpleIndexWithCorruptedEverything.jar", testFilesDir));
            fail();
        } catch (UpdateException e) {
            e.printStackTrace();
            fail();
        } catch (SecurityException e) {
            // success!
        }
    }

    public void testExtractIndexFromMasterKeyIndexJar() {
        if (!testFilesDir.canWrite())
            return;
        // this is supposed to fail
        try {
            repoUpdater.getIndexFromFile(TestUtils.copyAssetToDir(context, "masterKeyIndex.jar", testFilesDir));
            fail();
        } catch (UpdateException | SecurityException e) {
            // success!
        }
    }
}
