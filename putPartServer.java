import java.io.*;

public class putPartServer extends Thread{
	
	protected ObjectInputStream oin;
    protected int part;
    protected String punt;
    Thread father;

    public putPartServer(Thread father, int part, String puntatore, ObjectInputStream in){
        this.father = father;
        this.part = part;
        oin = in;
        punt = puntatore;
    }

    public synchronized void run(){
        try{
            File inFile = (File)oin.readObject();
            String comando = ("cmd /C ATTRIB +H "+punt+inFile.getName());
            Runtime.getRuntime().exec(comando);
            long lung = oin.readLong();
            FileOutputStream fos = new FileOutputStream(punt+inFile.getName());
            int bufSize = 5120;
            try{
                for(; lung > 0L; lung -= bufSize){
                    byte b[] = (byte[])oin.readObject();
                    fos.write(b);
                    fos.flush();
                }

            }catch(Exception exception) { }
            fos.close();
            System.gc();
        }catch(Exception e){
            GraficaServer.scriviOutput("Errore putPartServer: "+e.getMessage().toString());
        }
    }
}
