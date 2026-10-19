package com.kooo.evcam.update;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.R;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

/** Real Android JSON semantics; invalid metadata must fail before any network or file write. */
@RunWith(AndroidJUnit4.class)
public class GithubDigestAndroidTest {
    @Test public void jsonNullAndMissingDigestRejectDownloadBeforeNetworkOrFileCreation() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File folder=new File(context.getCacheDir(),"digest-test-"+java.util.UUID.randomUUID());
        File target=new File(folder,"update.apk");
        try {
            JSONObject explicitNull=new JSONObject("{\"digest\":null}");
            assertEquals("Android optString returns a literal null string","null",explicitNull.optString("digest",""));
            for(JSONObject asset:new JSONObject[]{explicitNull,new JSONObject("{}")}) {
                GithubReleases.Release release=new GithubReleases.Release("v9.0.0","update.apk",
                        "http://127.0.0.1:9/must-not-connect",1,asset.optString("digest",""),"","","");
                IOException failure=assertThrows(IOException.class,()->GithubReleases.download(release,target,
                        (done,total)->fail("Invalid digest must never reach download progress")));
                assertTrue("Fail on metadata before even validating the download URL",failure instanceof GithubReleases.Failure);
                assertEquals(R.string.upd_err_integrity,((GithubReleases.Failure)failure).messageRes);
                assertFalse("Do not create an installable or partial APK",target.exists());
                assertFalse("Metadata rejection precedes download-folder creation",folder.exists());
            }
        } finally {
            if(target.exists())target.delete();
            if(folder.exists())folder.delete();
        }
    }
}
