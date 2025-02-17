import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class putHandlerServer extends Thread{
	
	protected static int DemonPort;
    protected static ObjectInputStream oin;
    protected static String fileName;
    protected static String punt;
    Thread father;

    public putHandlerServer(Thread father, int port, String file, String puntatore){
        this.father = father;
        DemonPort = port;
        fileName = file;
        punt = puntatore;
    }

    public synchronized void run(){
        try{
            ServerSocket ss = new ServerSocket(DemonPort);
            Socket s = ss.accept();
            oin = new ObjectInputStream(s.getInputStream());
            File saveFile = new File(punt+"\\"+fileName);
            int nsplit = oin.readInt();
            System.gc();
            if(nsplit > 0){
                Thread getPartClient[] = new Thread[nsplit];
                for(int i = 0; i < nsplit; i++){
                    getPartClient[i] = new Thread(new putPartServer(Thread.currentThread(), i, punt, oin));
                    getPartClient[i].start();
                    getPartClient[i].join();
                }

                riunisciParti(saveFile, nsplit);
            }else{
                long lung = oin.readLong();
                FileOutputStream fos = new FileOutputStream(saveFile);
                int bufSize = 5120;
                byte b[] = new byte[bufSize];
                try{
                    for(; lung > 0L; lung -= bufSize){
                        b = (byte[])oin.readObject();
                        fos.write(b);
                        fos.flush();
                    }

                }catch(Exception e){
                    GraficaServer.scriviOutput("Errore putHandlerServer: "+e.getMessage().toString());
                }
                fos.close();
                System.gc();
            }
            oin.close();
            s.close();
            ss.close();
            GraficaServer.scriviOutput("**** Download "+fileName+" completed ****\n");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    void riunisciParti(File destination, int nparti){
        int bufSize = 5120;
        byte c[] = new byte[bufSize];
        try{
            FileOutputStream fos = new FileOutputStream(destination);
            for(int i = 0; i < nparti; i++){
                FileInputStream fis = new FileInputStream(destination+"."+i);
                int amount;
                while((amount = fis.read(c, 0, bufSize)) != -1) 
                    fos.write(c, 0, amount);
                fos.flush();
                fis.close();
                File remFile = new File(destination+"."+i);
                remFile.delete();
            }

            fos.close();
        }catch(Exception e){
            GraficaServer.scriviOutput("Errore riunisci: "+e.getMessage().toString());
        }
    }
}
