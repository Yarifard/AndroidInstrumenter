package ir.ac.um.AndroidInstrumenter.Utils;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupCreator {

    //TODO:: if it failed to create backup, the process should not continue

    public static void createBackup(VirtualFile baseDirectory, VirtualFile javaDirectory) throws IOException {
        String sourceDirectory = javaDirectory.getCanonicalPath();
        //String targetDirectory = baseDirectory.getCanonicalPath() + "\\backup\\";
        String timestamp = Utils.getTimestamp();
        String backupDirectory = baseDirectory.getCanonicalPath() + "\\javaFolder_backup_" + timestamp + ".zip";


        //copy source to target using Files Class
        //FileUtils.copyDirectory(new File(sourceDirectory), new File(targetDirectory));
        zipDirectory(backupDirectory, sourceDirectory);
    }

    private static void zipDirectory(String zipFileName, String rootDirectoryPath) throws IOException {
        File directoryObject = new File(rootDirectoryPath);
        if (!zipFileName.endsWith(".zip")) {
            zipFileName = zipFileName + ".zip";
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        System.out.println("Creating : " + zipFileName);
        addDirectory(directoryObject, out);
        out.close();
    }

    private static void addDirectory(File directoryObject, ZipOutputStream out) throws IOException {
        File[] files = directoryObject.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (File file : files) {
            if (file.isDirectory()) {
                addDirectory(file, out);
                continue;
            }

            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            System.out.println(" Adding: " + file.getAbsolutePath());


            out.putNextEntry(new ZipEntry(file.getAbsolutePath()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }
}

