package ca.alexcomeau.texmobile.db;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DatabaseBundler {
    private Context baseContext;

    public DatabaseBundler(Context base) { baseContext = base; }

    public boolean bundle(String dbName)
    {
        try {
            // build the full path to the database in the databases folder (where our db goes!)
            String destPath = baseContext.getApplicationInfo().dataDir + "/databases/" + dbName;
            // construct a file object
            File f = new File(destPath);

            if (!f.exists()) {
                // we have to bundle the database with app - first run!

                // manually make the databases folder
                File directory = new File(baseContext.getApplicationInfo().dataDir + "/databases");
                directory.mkdir();

                copyDB(baseContext.getAssets().open(dbName), new FileOutputStream(destPath));
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void copyDB(InputStream inputStream, FileOutputStream fileOutputStream) throws IOException {
        // array of 1024 bytes of data (1K)
        byte[] buffer = new byte[1024];

        int length;
        // read the first 1K of data from inputStream
        length = inputStream.read(buffer);
        while (length > 0){
            // write the data to the outputstream
            fileOutputStream.write(buffer, 0, length);
            // read the next 1K of data
            length = inputStream.read(buffer);
        }

        // close the streams
        inputStream.close();
        fileOutputStream.close();;
    }
}
