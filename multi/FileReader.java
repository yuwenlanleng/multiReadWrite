import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileReader {
    private int threadNum = 3;//�߳���,Ĭ��Ϊ3
    private String filePath;//�ļ�·��
    private int bufSize = 10*1024*1024;//��������С,Ĭ��Ϊ1024
    private DataProcessHandler dataProcessHandler;//���ݴ���ӿ�
    private ExecutorService threadPool;

    public FileReader(String filePath,int bufSize,int threadNum){
        this.threadNum = threadNum;
        this.bufSize = bufSize;
        this.filePath = filePath;
        this.threadPool = Executors.newFixedThreadPool(threadNum);
    }

    /**
     * �������̶߳�ȡ�ļ�
     */
    public void startRead(){
        FileChannel infile = null;
        try {
            RandomAccessFile raf = new RandomAccessFile(filePath,"r");
            infile = raf.getChannel();
            long size = infile.size();
            long subSize = size/threadNum;
            for(int i = 0; i < threadNum; i++){
                long startIndex = i*subSize;
                if(size%threadNum > 0 && i == threadNum - 1){
                    subSize += size%threadNum;
                }
                RandomAccessFile accessFile = new RandomAccessFile(filePath,"r");
                FileChannel inch = accessFile.getChannel();
                threadPool.execute(new MultiThreadReader(inch,startIndex,subSize));
            }
            threadPool.shutdown();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                if(infile != null){
                    infile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ע�����ݴ���ӿ�
     * @param dataHandler
     */
    public void registerHanlder(DataProcessHandler dataHandler){
        this.dataProcessHandler = dataHandler;
    }

    /**
     * ���̰߳��ж�ȡ�ļ�����ʵ����
     * @author zyh
     *
     */
    public class MultiThreadReader implements Runnable{
        private FileChannel channel;
        private long startIndex;
        private long rSize;

        public MultiThreadReader(FileChannel channel,long startIndex,long rSize){
            this.channel = channel;
            this.startIndex = startIndex > 0?startIndex - 1:startIndex;
            this.rSize = rSize;
        }

        public void run(){
            readByLine();
        }

        /**
         * ���ж�ȡ�ļ�ʵ���߼�
         * @return
         */
        public synchronized void readByLine(){
            try {
                ByteBuffer rbuf = ByteBuffer.allocate(bufSize);
                channel.position(startIndex);//���ö�ȡ�ļ�����ʼλ��
                long endIndex = startIndex + rSize;//��ȡ�ļ����ݵĽ���λ��
                byte[] temp = new byte[0];//���������ϴζ�ȡʣ�µĲ���
                int LF = "\n".getBytes()[0];//���з�
                boolean isEnd = false;//�����ж������Ƿ��ȡ��
                boolean isWholeLine = false;//�����жϵ�һ�ж�ȡ�����Ƿ���������һ��
                long lineCount = 0;//����ͳ��
                long endLineIndex = startIndex;//��ǰ�����ֽ�����λ��
                while(channel.read(rbuf) != -1 && !isEnd){
                    int position = rbuf.position();
                    byte[] rbyte = new byte[position];
                    rbuf.flip();
                    rbuf.get(rbyte);
                    int startnum = 0;//ÿ�е���ʼλ���±꣬����ڵ�ǰ����ȡ����byte����
                    //�ж��Ƿ��л��з�
                    //�����ȡ�����һ�в���������һ��ʱ������������ȡֱ����ȡ��������һ�вŽ���
                    for(int i = 0; i < rbyte.length; i++){
                        endLineIndex++;
                        if(rbyte[i] == LF){//�����ڻ��з�
                            if(channel.position() == startIndex){//��������Ƭ�ε�һ���ֽ�Ϊ���з���˵����һ�ж�ȡ������������һ��
                                isWholeLine = true;
                                startnum = i + 1;
                            }else{
                                 byte[] line = new byte[temp.length + i - startnum + 1];
                                System.arraycopy(temp, 0, line, 0, temp.length);
                                System.arraycopy(rbyte, startnum, line, temp.length, i - startnum + 1);
                                startnum = i + 1;
                                lineCount++;
                                temp = new byte[0];
                                //��������
                                if(startIndex != 0){//������ǵ�һ�����ݶ�
                                    if(lineCount == 1){
                                        if(isWholeLine){//���ҽ�����һ��Ϊ������ʱ�Ŵ���
                                            dataProcessHandler.process(line);
                                        }
                                    }else{
                                        dataProcessHandler.process(line);
                                    }
                                }else{
                                    dataProcessHandler.process(line);
                                }
                                //������ȡ���ж�
                                if(endLineIndex >= endIndex){
                                    isEnd = true;
                                    break;
                                }
                            }
                        }
                    }
                    if(!isEnd && startnum < rbyte.length){//˵��rbyte���ʣ��������һ��
                        byte[] temp2 = new byte[temp.length + rbyte.length - startnum];
                        System.arraycopy(temp, 0, temp2, 0, temp.length);
                        System.arraycopy(rbyte, startnum, temp2, temp.length, rbyte.length - startnum);
                        temp = temp2;
                    }
                    rbuf.clear();
                }
                //�������һ��û�л��е����
                if(temp.length > 0){
                    if(dataProcessHandler != null){
                        dataProcessHandler.process(temp);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getThreadNum() {
        return threadNum;
    }

    public String getFilePath() {
        return filePath;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }
    public int getBufSize() {
        return bufSize;
    }

}
