import java.io.*;
import java.util.Vector;

public class Lancillotti implements Serializable{

	private static final long serialVersionUID = 1L;
    protected String nome_account;
    protected String password;
    protected String ip;
    protected String port;
    protected String initfolder;
    protected boolean isOn;
    protected boolean isAdmin;
    protected Vector<String> mex_pendenti;
    transient protected BufferedReader inputStream;
    transient protected BufferedWriter outputStream;
	
    public Lancillotti(String name, String psw, boolean admin){
        nome_account = name;
        password = psw;
        mex_pendenti = new Vector<String>();
        isOn = false;
        isAdmin = admin;
    }
}
