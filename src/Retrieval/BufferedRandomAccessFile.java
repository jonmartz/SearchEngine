package Retrieval;

import java.io.*;

public class BufferedRandomAccessFile extends RandomAccessFile {

    private int BUF_SIZE;
    private byte buffer[];
    private int buf_end = 0;
    private int buf_pos = 0;
    private long real_pos = 0;

    public BufferedRandomAccessFile(String filename, String mode)
            throws IOException {
        super(filename,mode);
        invalidate();
        BUF_SIZE = 8192;
        buffer = new byte[BUF_SIZE];
    }

    public final int read() throws IOException{
        if(buf_pos >= buf_end) {
            if(fillBuffer() < 0)
                return -1;
        }
        if(buf_end == 0) {
            return -1;
        } else {
            return buffer[buf_pos++];
        }
    }
    private int fillBuffer() throws IOException {
        int n = super.read(buffer, 0, BUF_SIZE);
        if(n >= 0) {
            real_pos +=n;
            buf_end = n;
            buf_pos = 0;
        }
        return n;
    }
    private void invalidate() throws IOException {
        buf_end = 0;
        buf_pos = 0;
        real_pos = super.getFilePointer();
    }

    public int read(byte b[], int off, int len) throws IOException {
        int leftover = buf_end - buf_pos;
        if(len <= leftover) {
            System.arraycopy(buffer, buf_pos, b, off, len);
            buf_pos += len;
            return len;
        }
        for(int i = 0; i < len; i++) {
            int c = this.read();
            if(c != -1)
                b[off+i] = (byte)c;
            else {
                return i;
            }
        }
        return len;
    }

    public long getFilePointer() throws IOException{
        long l = real_pos;
        return (l - buf_end + buf_pos) ;
    }

    public void seek(long pos) throws IOException {
        int n = (int)(real_pos - pos);
        if(n >= 0 && n <= buf_end) {
            buf_pos = buf_end - n;
        } else {
            super.seek(pos);
            invalidate();
        }
    }

    /**
     * return a next line in String
     */
    public final String getNextLine() throws IOException {
        String str = null;
        if(buf_end-buf_pos <= 0) {
            if(fillBuffer() < 0) {
                return null;
            }
        }
        int lineend = -1;
        for(int i = buf_pos; i < buf_end; i++) {
            if(buffer[i] == '\n') {
                lineend = i;
                break;
            }
        }
        if(lineend < 0) {
            StringBuffer input = new StringBuffer(256);
            int c;
            while (((c = read()) != -1) && (c != '\n')) {
                input.append((char)c);
            }
            if ((c == -1) && (input.length() == 0)) {
                return null;
            }
            return input.toString();
        }
        if(lineend > 0 && buffer[lineend-1] == '\r')
            str = new String(buffer, 0, buf_pos, lineend - buf_pos -1);
        else str = new String(buffer, 0, buf_pos, lineend - buf_pos);
        buf_pos = lineend +1;
        return str;
    }

//    public static void main (String[] args) throws IOException {
//        String path = "C:\\Users\\Jonathan\\Documents\\BGU\\Semester 5\\Information Retrieval\\index\\WithStemming\\postings\\Q";
//        String line;
//
//        long start = System.currentTimeMillis();
//        BufferedRandomAccessFile braf = new BufferedRandomAccessFile(path, "rw");
//        HashMap<String, Long> pointers = new HashMap<>();
//        long pointer = braf.getFilePointer();
//        pointers.put(braf.getNextLine(), pointer);
//        try {
//            while ((line = braf.getNextLine()) != null) {
//                if (line.isEmpty()) {
//                    pointer = braf.getFilePointer();
//                    pointers.put(braf.getNextLine(), pointer);
//                }
////            System.out.println(line);
//            }
//        } catch (Exception e) {e.printStackTrace();}
//        braf.close();
//
//        braf = new BufferedRandomAccessFile(path, "rw");
//        for (Map.Entry<String, Long> entry : pointers.entrySet()){
//            braf.seek(entry.getValue());
//            System.out.println(entry.getKey() + " = " + braf.getNextLine());
//        }
//
//        System.out.println("braf: " + (System.currentTimeMillis() - start));
//
////        start = System.currentTimeMillis();
////        RandomAccessFile raf = new RandomAccessFile(path, "rw");
////        while ((line = raf.readLine()) != null);
////        raf.close();
////        System.out.println("raf: " + (System.currentTimeMillis() - start));
////
////        start = System.currentTimeMillis();
////        BufferedReader br = new BufferedReader(new InputStreamReader(
////                new FileInputStream(path), StandardCharsets.UTF_8));
////        while ((line = br.readLine()) != null);
////        br.close();
////        System.out.println("br: " + (System.currentTimeMillis() - start));
//    }
}