public class MultiThreadReadByLine {
    public static void main(String[] args){
        FileReader fileReader = new FileReader("D:\\abc.csv",100,3);
        fileReader.registerHanlder(new FileLineDataHandler());
        fileReader.startRead();
    }
}
