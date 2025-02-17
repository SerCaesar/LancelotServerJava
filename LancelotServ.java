import java.awt.Image;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class LancelotServ{
	
	protected static int LANCELOTPORT = 23232;
    protected static int maxCli = 10;
    protected static int i = 0;
    protected static int dataRis;
    protected static String puntatoreAdmin;
    protected static String puntatoreUser;
    protected static boolean close = false;
    protected static Vector<Lancillotti> accounts;
    protected static Thread lancelotclient[];
    protected static ServerSocket ss;

    public static void main(String args[]){
        ThreadGroup tg;
        lancelotclient = new Thread[maxCli];
        tg = new ThreadGroup("NumLancelotClient");
        accounts = new Vector<Lancillotti>();
        ss = null;
        
        try{
        	final Image iconImg = ImageIO.read(GraficaServer.class.getResource("LancelotImmy.gif"));
        
        	SwingUtilities.invokeLater(new Runnable() {

        		public void run()
        		{
        			GraficaServer f = new GraficaServer(iconImg);
        			f.setIconImage((new ImageIcon(GraficaServer.class.getResource("LancelotImmy.gif"))).getImage());
        			f.setDefaultCloseOperation(3);
        			f.setVisible(true);
        			f.setExtendedState(1);
        		}
        	});
        
        	carica();
        	        	
        	puntatoreAdmin = System.getenv("HOMEDRIVE");
        	puntatoreUser = puntatoreAdmin;
        	puntatoreUser += System.getenv("HOMEPATH");
        
        	File newDir = new File(puntatoreUser+"\\Lancelot-Download\\");
        	newDir.mkdirs();
        
        	puntatoreAdmin += "\\";
        	puntatoreUser += "\\Lancelot-Download\\";
        	
        	File filerestart = new File(System.getenv("windir")+"\\restartLancelotServer.bat");
        	filerestart.delete();
        	
        
        	ss = new ServerSocket(LANCELOTPORT);
        	
        }catch(Exception e){e.printStackTrace();}
        
        boolean fermate = false;
        
        while(!fermate)
            try{
                if(dataRis == 1)
                    GraficaServer.scriviOutput("LancelotServer wait incoming connection on port "+LANCELOTPORT+"\r\nAccount Loaded\r\n");
                if(dataRis == 0)
                    GraficaServer.scriviOutput("LancelotServer wait incoming connection on port "+LANCELOTPORT+"\r\nAccount Not Loaded\r\n");
                if(dataRis == -1)
                    GraficaServer.scriviOutput("LancelotServer wait incoming connection on port "+LANCELOTPORT+"\r\nCAUTION: Account Modified\r\n");
                fermate = true;
            }catch(NullPointerException e){
                fermate = false;
            }
                    

        while(!close){
            try{
                Socket s = ss.accept();
                if(i < maxCli){
                    GraficaServer.scriviOutput("LancelotClient connected IP: "+s.getInetAddress()+" Port: "+s.getPort());
                    lancelotclient[i] = new Thread(tg, new handlerServer(s, Thread.currentThread()));
                    lancelotclient[i].start();
                    i++;
                }else{
                    BufferedWriter ou = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    ou.write("LANCELOT: Server\r\nACTION: Logout\r\nDATA: Number of max connections reached\r\nEND\r\n");
                    ou.newLine();
                    ou.flush();
                    ou.close();
                    s.close();
                }
                continue;
            }catch(SocketException se){
            	close = true;
            }catch(IOException e){
                close = true;
            }
        }
        GraficaServer.scriviOutput("Shut-Down in progress...");
        for(int i=0; i<accounts.size(); i++){
        	try{
        		if(accounts.get(i).isOn){
        			accounts.get(i).inputStream.close();
        			accounts.get(i).outputStream.close();
        		}
        	}catch(IOException e){}
        }        
        try {
        	File filelog = new File(System.getenv("windir")+"\\lancelot_log.txt");
        	FileOutputStream fout = new FileOutputStream(filelog);
			fout.write(GraficaServer.OutputArea.getText().getBytes());
			fout.flush();
			fout.close();
		} catch (Exception e) {}
        System.exit(0);
    }

    public static void decI(){
        i--;
    }

    public static void closeLancelot(){
        close = true;
        try{
        	ss.close();
        }catch(Exception e){}
    }

    public static boolean salva()
        throws IOException{
    	
        if(accounts.size() != 0){
            File bla = new File(System.getenv("windir")+"\\lancelot_account.lnc");
            FileOutputStream out = new FileOutputStream(bla);
            ObjectOutputStream gestore = new ObjectOutputStream(out);
            gestore.writeObject(new Integer(accounts.size()));
            for(int i = 0; i < accounts.size(); i++)
                gestore.writeObject((Lancillotti)accounts.elementAt(i));
            Date data = new Date();
            gestore.writeObject(data);
            bla.setReadable(false, true);
            gestore.flush();
            gestore.close();
            out.close();
            System.out.println("Account Salvati");
            return true;
        } else{
            return false;
        }
    }

    public static void carica()
        throws IOException, ClassNotFoundException, FileNotFoundException{
        
    	try{
        	        	
        	File pippo = new File(System.getenv("windir")+"\\lancelot_account.lnc");
        	FileInputStream in = new FileInputStream(pippo);
            ObjectInputStream gestore = new ObjectInputStream(in);
            int numClient = ((Integer)gestore.readObject()).intValue();
            for(int i = 0; i < numClient; i++){
                accounts.add((Lancillotti)gestore.readObject());
                accounts.get(i).inputStream=null;
                accounts.get(i).outputStream=null;
            }

            Date dataLetta = (Date)gestore.readObject();
            Date dataLutta = new Date(pippo.lastModified());
            if(dataLetta.getYear() != dataLutta.getYear() || dataLetta.getMonth() != dataLutta.getMonth() || dataLetta.getDate() != dataLutta.getDate() || dataLetta.getHours() != dataLutta.getHours() || dataLetta.getMinutes() != dataLutta.getMinutes())
                dataRis = -1;
            else
                dataRis = 1;
            gestore.close();
        }catch(Exception e){
        	accounts.add(new Lancillotti("admin", "4a4abbf400bfa994ceaba3f8de6ad113c29b7d12", true));
            dataRis = 0;
        }
    }

    public static int isAccount(String nome){
        boolean trovato = false;
        int i;
        for(i = 0; !trovato && i < accounts.size();)
            if(((Lancillotti)accounts.get(i)).nome_account.equals(nome))
                trovato = true;
            else
                i++;

        if(i == accounts.size())
            i = -1;
        return i;
    }

    public static boolean isPassword(int pos, String psw){
        return accounts.get(pos).password.equals(psw);
    }

    private static String convertToHex(byte data[]){
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < data.length; i++){
            int halfbyte = data[i] >>> 4 & 0xf;
            int two_halfs = 0;
            do{
                if(halfbyte >= 0 && halfbyte <= 9)
                    buf.append((char)(48 + halfbyte));
                else
                    buf.append((char)(97 + (halfbyte - 10)));
                halfbyte = data[i] & 0xf;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text){
        byte sha1hash[] = new byte[40];
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        return convertToHex(sha1hash);
    }
}
