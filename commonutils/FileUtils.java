package commonutils;

import java.io.*;
import java.util.zip.*;

public class FileUtils
{
    /**
     * zipFile -
     * Accepts two files. One file source to compress and one file desination to
     * write compressed contents to. Uses recursive zip function to compress
     *
     * @param fileToZip - File, source
     * @param zipFile - File desination
     * @throws IOException
     */
    public static void zipFile(File fileToZip, File zipFile) throws IOException
    {
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
        recurseZipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
    }

    /**
     * recurseZipFile -
     * Recursivley zips a file or folder
     *
     * @param fileToZip - source file
     * @param fileName - name of file, used for zip entry
     * @param zipOut - zip output stream
     * @throws IOException
     */
    private static void recurseZipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException
    {
        // skip if hidden
        if (!fileToZip.isHidden())
        {
            if (fileToZip.isDirectory())
            {
                // create new entry for a directory
                zipOut.putNextEntry((new ZipEntry(fileName + "/")));
                zipOut.closeEntry();

                // go through each file in directory
                File[] children = fileToZip.listFiles();
                for (File childFile : children)
                {
                    recurseZipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            else
            {
                // if file, just add to zip archive
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);

                // write to zip file out
                int length;
                byte[] bytes = new byte[1024];
                while ((length = fis.read(bytes)) >= 0)
                {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }
        }
    }

    /**
     * unzipFile -
     * Iteratevely unzips a file to the given destination file
     *
     * @param zipFile - zip file to decompress
     * @param unzippedDest - destination to place file
     * @throws IOException
     */
    public static void unzipFile(File zipFile, File unzippedDest) throws IOException
    {
        byte[] buffer = new byte[1024];
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));

        // for each entry, create a file
        ZipEntry zipEntry = zipIn.getNextEntry();
        while (zipEntry != null)
        {
            File newFile = newFile(unzippedDest, zipEntry);
            if (zipEntry.isDirectory())
            {
                newFile.mkdirs();
            }
            else
            {
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zipIn.read(buffer)) > 0)
                {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zipIn.getNextEntry();
        }
        zipIn.closeEntry();
        zipIn.close();
    }

    /**
     * new File -
     * Helper function that creates a new file based on the zip entry passed in
     *
     * @param destinationDir - where the file will be written to
     * @param zipEntry - zip entry from decompressing zip
     * @return - File, new file from zip entry
     * @throws IOException
     */
    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException
    {
        File destFile = new File(destinationDir, zipEntry.getName());

        return destFile;
    }
}
