package gitlet;
import java.io.Serializable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Blob implements Serializable, Dumpable{
    private final byte[] fileContent;
    private String fileID;

    public Blob(String fileName, byte[] fileContent) {
        this.fileContent = fileContent;
        this.fileID = generateID(fileName);
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void saveBlob() {
        File blobFolder = Utils.join(Repository.OBJ_DIR, this.fileID.substring(0, 2));
        blobFolder.mkdir();
        File blob = Utils.join(blobFolder, this.fileID.substring(2));
        Utils.writeObject(blob, this);
    }
    private String generateID(String fileName) {
        return Utils.sha1(fileName, (Object) this.fileContent);
    }

    public String getFileID() {
        return fileID;
    }

    @Override
    public void dump() {
        System.out.printf("Blob%nFileID: %s%nFileContent: %s%n", fileID, new String(fileContent, StandardCharsets.US_ASCII));
    }
}
