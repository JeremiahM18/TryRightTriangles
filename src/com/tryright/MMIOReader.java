package com.tryright;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Use memory-mapped I/O to efficiently read a 4-byte integer
 * from the file at the position: index * 4 bytes
 */
public class MMIOReader {

    private static final int INTEGER_SIZE = 4; // Java int is 4 bytes

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java MMIOReader <filename> <index>");
            System.exit(1);
        }
        String filename = args[0];
        int index = Integer.parseInt(args[1]);

        try (RandomAccessFile file = new RandomAccessFile(filename, "r");
             FileChannel channel = file.getChannel()) {
            long position = (long) index * INTEGER_SIZE;

            MappedByteBuffer buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    position,
                    INTEGER_SIZE
            );
            int value = buffer.getInt();
            System.out.println("Integer at index: " + index + ":" + value);
        }
    }
}
