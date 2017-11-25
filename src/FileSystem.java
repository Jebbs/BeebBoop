import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class FileSystem{
    private static int inodesPerBlock;

    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;
    private Scheduler scheduler;//this is to have access to the TCB's

    public FileSystem(int diskBlocks, Scheduler sched)
    {
        inodesPerBlock = Disk.blockSize/Inode.iNodeSize;
        superBlock = new SuperBlock(diskBlocks);

        if(superBlock.totalInodes  == 0)
            diskFormat(72);

        directory = new Directory(superBlock.totalInodes);

        fileTable = new FileTable(directory);

        scheduler = sched;

        //directory reconstruction?
        FileTableEntry dirEntry = open("/","r");
        int dirSize = fsize(dirEntry);
    }


    /**
     * Write all blocks back to the disk?
     */
    void sync(){

    }

    /**
     * format the disk such that there are n files allowed in the system.
     */
    boolean format(int files){
        if(!diskFormat(files))
            return false;

        //do some other stuff here later
        return true;
    }

    /**
     * Open a file.
     */
    FileTableEntry open(String filename, String mode){
        short thing = -1;
        return new FileTableEntry(new Inode(), thing, "");
    }

    boolean close(FileTableEntry entry){
        return false;
    }

    int fsize(FileTableEntry entry){
        return -1;
    }

    int read(FileTableEntry entry, byte[] buffer){
        return -1;
    }

    int write(FileTableEntry entry, byte[] buffer){
        return -1;
    }

    private boolean deallocAllBlocks(FileTableEntry entry){
        return false;
    }

    boolean delete(String filename){
        return false;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 0;
    private final int SEEK_END = 0;

    int seek(FileTableEntry entry, int offset, int whence){
        return -1;
    }


    private boolean diskFormat(int files)
    {
        superBlock.clearFreeList();
        superBlock.totalInodes = files;

        //get an array of bytes for a block of blank inodes
        byte[] inodeBlock = new byte[Disk.blockSize];
        Inode blankInode = new Inode();
        for(int i = 0; i < inodesPerBlock; i++)
            Inode.inodeToBytes(blankInode, inodeBlock, i*Inode.iNodeSize);

        //rewrite the inode blocks
        for(int i = files, j = 0; i > 0; i-= inodesPerBlock, j=0)
        {
            //get next free block (though it will be early)
            int block = superBlock.findFirstFreeBlock();
            superBlock.setFreeList(block, true);
            SysLib.rawwrite(block, inodeBlock);
        }

        //set up first inode to refer to a directory
        Inode directory = new Inode();
        directory.length = 32;
        directory.direct[0] = superBlock.findFirstFreeBlock();
        Inode.inodeToBytes(directory, inodeBlock, 0);
        SysLib.rawwrite(1, inodeBlock);//1 is the first block after superblock


        //write the directory block to disk
        byte[] directoryBlock = new byte[Disk.blockSize];
        char[] name = new char[30];
        name[0] = '/';
        name[1] = '\0';
        SysLib.short2bytes((short)0, directoryBlock, 0);//inode 0 refers to root
        byte[] nameInBytes = toBytes(name);

        for(int i = 0,j=2; i < name.length; i++,j++)
        {
            directoryBlock[j] = nameInBytes[i];
        }

        SysLib.rawwrite(directory.direct[0], directoryBlock);

        return true;
    }

    //taken from stack overflow
    // https://stackoverflow.com/questions/5513144/converting-char-to-byte
    private byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());

        return bytes;
    }
}