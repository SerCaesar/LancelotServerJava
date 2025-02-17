import java.io.*;
import java.net.Socket;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class handlerServer extends Thread
{
	
	Socket socket;
    int port;
    int posClient;
    Thread father;
    String nomeAccount;
    String puntatore;
    boolean chiudi;

    public handlerServer(Socket s, Thread father)
    {
        this.father = father;
        socket = s;
    }

    public void run()
    {
        posClient = -1;
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
            while(!chiudi)	
            {
                String messaggio = riceviMex(br);
                if(messaggio == null)
                    break;
                GraficaServer.scriviOutput(messaggio);
                gestisciMessaggio(findField("ACTION", messaggio), messaggio, out);
            }

            LancelotServ.decI();
            if(posClient > -1)
            {
                LancelotServ.accounts.get(posClient).isOn = false;
                aggiornaClient();
                LancelotServ.accounts.get(posClient).inputStream.close();
                LancelotServ.accounts.get(posClient).outputStream.close();
            } else
            {
                br.close();
                out.close();
            }
            socket.close();
        }
        catch(IOException e)
        {
            LancelotServ.accounts.get(LancelotServ.isAccount(nomeAccount)).isOn = false;
            LancelotServ.decI();
        }
    }

    boolean login(String messaggio){
        String account = findField("ACCOUNT", messaggio);
        int pos;
        if((pos = LancelotServ.isAccount(account)) != -1 && LancelotServ.isPassword(pos, findField("PASSWORD", messaggio)))
        {
            LancelotServ.accounts.get(pos).ip = socket.getInetAddress().getHostAddress();
            LancelotServ.accounts.get(pos).port = Integer.toString(socket.getPort());
            LancelotServ.accounts.get(pos).isOn = true;
            nomeAccount = account;
            return true;
        }
        return false;
	}

    static String findField(String field, String messaggio){
    	
        String result[] = messaggio.split("\r\n");
        int numToken = result.length - 1;
        for(int i = 0; i < numToken; i++)
        {
            String a = result[i].substring(0, result[i].indexOf(": "));
            String b = result[i].substring(result[i].indexOf(": ") + 2, result[i].length());
            if(a.equals(field))
                return b;
        }

        return null;
        
    }

    String riceviMex(BufferedReader is)
    {
        String str = "";
        boolean fermati = false;
        String bla;
        try
        {
            while((bla = is.readLine()) != null && !fermati) 
            {
                String spliter[] = bla.split("-n-");
                for(int i = 0; i < spliter.length - 1; i++)
                    str = str+spliter[i]+"\n";

                str = str+spliter[spliter.length - 1]+"\r\n";
                if(bla.equals("END"))
                    fermati = true;
            }
        }
        catch(IOException e)
        {
            return null;
        }
        return str;
    }

    boolean inviaMex(BufferedWriter out, String mex)
    {
        try
        {
            String messaggio = "LANCELOT: Server\r\n";
            messaggio += mex;
            messaggio += "END\r\n";
            out.write(messaggio);
            out.newLine();
            out.flush();
        }
        catch(IOException e)
        {
            return false;
        }
        return true;
    }

    boolean aggiornaClient()
    {
        try
        {
            String messaggio = "LANCELOT: Server\r\n";
            messaggio += "ACTION: CLIENT\r\n";
            messaggio += "DATA:    Nome\t|   Ip-n-";
            for(int i = 0; i < LancelotServ.accounts.size(); i++)
                if(LancelotServ.accounts.get(i).isOn)
                {
                    if(LancelotServ.accounts.get(i).isAdmin)
                        messaggio += "* ";
                    else
                        messaggio += "- ";
                    messaggio += LancelotServ.accounts.get(i).nome_account+"\t| "+LancelotServ.accounts.get(i).ip+"-n-";
                }

            messaggio += "\r\n";
            messaggio += "END\r\n";
            for(int i = 0; i < LancelotServ.accounts.size(); i++)
                if(LancelotServ.accounts.get(i).isOn)
                {
                    LancelotServ.accounts.get(i).outputStream.write(messaggio);
                    LancelotServ.accounts.get(i).outputStream.newLine();
                    LancelotServ.accounts.get(i).outputStream.flush();
                }

        }
        catch(IOException e)
        {
            return false;
        }
        return true;
    }

    void chiudiConnessione()
    {
        chiudi = true;
    }

    void gestisciMessaggio(String action, String messaggio, BufferedWriter out)
    {
        if(action.equals("Login")){
            boolean attempt = login(messaggio);
            String mex = "ACTION: Login\r\n";
            if(attempt){
                posClient = LancelotServ.isAccount(nomeAccount);
                int pending = LancelotServ.accounts.get(posClient).mex_pendenti.size();
                
                try{
                	LancelotServ.accounts.get(posClient).inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    LancelotServ.accounts.get(posClient).outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                }catch(IOException ioexception) { }
                
                puntatore = LancelotServ.puntatoreAdmin;
                if(!LancelotServ.accounts.get(posClient).isAdmin)
                    puntatore = LancelotServ.puntatoreUser;
                mex += "ACCOUNT: "+nomeAccount+"\r\n";
                mex += "PATH: "+puntatore+"\r\n";
                mex += "DATA: ***Welcome to Lancelot Server!***-n-";
                if(pending > 1)
                    mex += "You have "+pending+" messages pending\r\n";
                else
                if(pending == 1)
                    mex += "You have one message pending\r\n";
                else
                    mex += "You have no messages pending\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            }else{
                mex += "DATA: Login Not Accepted\r\n";
                chiudiConnessione();
                inviaMex(out, mex);
            }
            aggiornaClient();
            return;
        }
        if(action.equals("Logout")){
            String mex = "ACTION: Logout\r\n";
            mex += "DATA: Bye!\r\n";
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            chiudiConnessione();
            return;
        }
        if(action.equals("LS")){
            File dir = new File(puntatore);
            String richiesta[] = dir.list();
            Vector<String> cartelle = new Vector<String>();
            Vector<String> file = new Vector<String>();
            for(int i = 0; i < richiesta.length; i++){
                File cursor = new File(puntatore+"\\"+richiesta[i]);
                if(!cursor.isHidden())
                    if(cursor.isDirectory())
                        cartelle.add(cursor.getName());
                    else
                        file.add(cursor.getName());
            }

            String mex = "ACTION: LS\r\n";
            mex += "DATA: *** DIRECTORY ***-n-";
            for(int i = 0; i < cartelle.size(); i++)
                mex += cartelle.get(i)+"-n-";

            mex += "*** FILE ***-n-";
            for(int i = 0; i < file.size(); i++)
                mex += file.get(i)+"-n-";

            mex += "\r\n";
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("LSALL")){
            File dir = new File(puntatore);
            String richiesta[] = dir.list();
            Vector<String> cartelle = new Vector<String>();
            Vector<String> file = new Vector<String>();
            for(int i = 0; i < richiesta.length; i++){
                File cursor = new File(puntatore+"\\"+richiesta[i]);
                if(cursor.isDirectory())
                    cartelle.add(cursor.getName());
                else
                    file.add(cursor.getName());
            }

            String mex = "ACTION: LS\r\n";
            mex += "DATA: *** DIRECTORY ***-n-";
            for(int i = 0; i < cartelle.size(); i++)
                mex += cartelle.get(i)+"-n-";

            mex += "*** FILE ***-n-";
            for(int i = 0; i < file.size(); i++)
                mex += file.get(i)+"-n-";

            mex += "\r\n";
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("CD")){
            String newDir = puntatore;
            String fb = findField("DATA", messaggio);
            String mex = "ACTION: CD\r\n";
            if(fb.equals("..\\")){
                if(!LancelotServ.accounts.get(posClient).isAdmin && puntatore.equals(LancelotServ.puntatoreUser)){
                    mex += "PATH: "+puntatore+"\r\n";
                    mex += "DATA: You don't have permiss to do this operation\r\n";
                    inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                    return;
                }
                if(newDir.length() > 3){
                    String bla = newDir.substring(0, newDir.lastIndexOf("\\"));
                    String bla2 = newDir.substring(0, bla.lastIndexOf("\\"));
                    newDir = bla2+"\\";
                }
            } else{
                newDir += fb;
            }
            File isDir = new File(newDir);
            if(isDir.isDirectory()){
                puntatore = newDir;
                mex += "PATH: "+puntatore+"\r\n";
            } else{
                mex += "DATA: '"+newDir+"' is not a valid directory\r\n";
            }
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("INFO")){
            String newDir = puntatore;
            String fb = findField("DATA", messaggio);
            newDir += fb;
            String mex = "ACTION: INFO\r\n";
            File exist = new File(newDir);
            if(exist.exists()){
            	
                if(exist.isDirectory())
                    mex += "DATA: ***INFO DIRECTORY***-n-";
                else
                    mex += "DATA: ***INFO FILE***-n-";
                
                mex += "Name: "+exist.getName()+"-n-";
                mex += "Can-Read: "+exist.canRead()+"-n-";
                mex += "Can-Write: "+exist.canWrite()+"-n-";
                mex += "Can-Execute: "+exist.canExecute()+"-n-";
                mex += "Hidden: "+exist.isHidden()+"-n-";
                if(exist.isFile())
                    mex += "Length: "+exist.length()+"bytes -n-";
                mex += "LastModified: "+exist.lastModified()+"\r\n";
            } else{
                mex += "DATA: '"+newDir+"' is not a valid file or directory\r\n";
            }
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("Pending")){
            Vector<String> pending = LancelotServ.accounts.get(posClient).mex_pendenti;
            String mex = "ACTION: Message\r\n";
            mex += "DATA: ";
            if(pending.size() > 0){
                for(int i = 0; i < pending.size(); i++)
                    mex += "Messaggio "+i + 1+"-n-"+pending.get(i)+"-n-";

                mex += "\r\n";
                LancelotServ.accounts.get(posClient).mex_pendenti.removeAllElements();
            } else{
                mex += "You have no message pending\r\n";
            }
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("SENDTO")){
            String mex = "ACTION: SENDTO\r\n";
            int pos = LancelotServ.isAccount(findField("TO", messaggio));
            if(pos == -1){
                mex += "FROM: Server\r\n";
                mex += "DATA: Invalid User\r\n";
            } else
            if(LancelotServ.accounts.get(pos).isOn){
                try{
                    LancelotServ.accounts.get(pos).outputStream.write(messaggio);
                    LancelotServ.accounts.get(pos).outputStream.newLine();
                    LancelotServ.accounts.get(pos).outputStream.flush();
                    mex += "FROM: "+findField("FROM", messaggio)+"\r\n";
                    mex += "DATA: "+findField("DATA", messaggio)+"\r\n";
                }catch(IOException e){
                    mex += "FROM: Server\r\n";
                    mex += "DATA: Error to send message\r\n";
                }
            } else{
                String messaggiolo = findField("FROM", messaggio);
                messaggiolo += ": ";
                messaggiolo += findField("DATA", messaggio);
                LancelotServ.accounts.get(pos).mex_pendenti.add(messaggiolo);
                mex += "FROM: "+findField("FROM", messaggio)+"\r\n";
                mex += "DATA: "+findField("DATA", messaggio)+"-n-";
                mex += "User is not Online now, the message will send later\r\n";
            }
            inviaMex(out, mex);
        }
        if(action.equals("MKDIR")){
            File newDir = new File(puntatore+findField("DATA", messaggio));
            boolean res = newDir.mkdirs();
            if(!res){
                String mex = "ACTION: MKDIR\r\n";
                mex += "DATA: Creation failed\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            }
            return;
        }
        if(action.equals("RM")){
            File newDir = new File(puntatore+findField("DATA", messaggio));
            boolean res = false;
            if(newDir.isDirectory()){
                String empty[] = newDir.list();
                if(empty.length == 0){
                    res = newDir.delete();
                    if(!res){
                        String mex = "ACTION: RM\r\n";
                        mex += "DATA: Remove failed\r\n";
                        inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                        return;
                    }
                } else{
                    String mex = "ACTION: RM\r\n";
                    mex += "DATA: First remove all file\r\n";
                    inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                    return;
                }
            } else{
                res = newDir.delete();
                if(!res){
                    String mex = "ACTION: RM\r\n";
                    mex += "DATA: Remove failed\r\n";
                    inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                    return;
                }
            }
            return;
        }
        if(action.equals("MKFILE")){
            File newDir = new File(puntatore+findField("DATA", messaggio));
            boolean res = false;
            try{
                res = newDir.createNewFile();
            }catch(IOException e){
                String mex = "ACTION: MKFILE\r\n";
                mex += "DATA: Creation failed\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                return;
            }
            if(!res){
                String mex = "ACTION: MKFILE\r\n";
                mex += "DATA: Creation failed\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            }
            return;
        }
        if(action.equals("RENAME")){
            File newDir = new File(puntatore+findField("FILE", messaggio));
            boolean res = false;
            res = newDir.renameTo(new File(puntatore+findField("DATA", messaggio)));
            if(!res){
                String mex = "ACTION: RENAME\r\n";
                mex += "DATA: Rename failed\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            }
            return;
        }
        if(action.equals("GET")){
            boolean ok = false;
            File checkFile = new File(puntatore);
            String listaFile[] = checkFile.list();
            String file = findField("FILE", messaggio);
            for(int i = 0; i < listaFile.length && !ok; i++)
                if(listaFile[i].equals(file))
                    ok = true;

            if(ok){
                String mex = "ACTION: GET\r\n";
                mex += "PORT: "+findField("PORT", messaggio)+"\r\n";
                mex += "FILE: "+file+"\r\n";
                mex += "DATA: OK\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
                Thread downclient = new Thread(new getHandlerServer(Thread.currentThread(), socket.getInetAddress().getHostAddress(), findField("PORT", messaggio), puntatore+file));
                downclient.start();
            } else{
                String mex = "ACTION: GET\r\n";
                mex += "DATA: File Not Found\r\n";
                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            }
            return;
        }
        if(action.equals("PUT")){
            int copy = 0;
            File checkFile = new File(puntatore);
            String listaFile[] = checkFile.list();
            String file = findField("FILE", messaggio);
            for(int i = 0; i < listaFile.length; i++)
                if(listaFile[i].equals(file))
                    copy++;

            if(copy > 0)
                file += "("+copy+")";
            Thread downclient = new Thread(new putHandlerServer(Thread.currentThread(), Integer.parseInt(findField("PORT", messaggio)), file, puntatore));
            downclient.start();
            String mex = "ACTION: PUT\r\n";
            mex += "FILE: "+findField("DATA", messaggio)+"\r\n";
            mex += "DATA: OK\r\n";
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
            return;
        }
        if(action.equals("NEW_ACCOUNT")){
        	String mex = "ACTION: NEW_ACCOUNT\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            int pos = LancelotServ.isAccount(findField("NAME", messaggio));
	            if(pos != -1)
	                mex += "DATA: Username '"+findField("NAME", messaggio)+"' is already in use\r\n";
	            else{
	            	
	            	if (Integer.parseInt(findField("ADMIN", messaggio)) == 1)
	            		LancelotServ.accounts.add(new Lancillotti(findField("NAME", messaggio),findField("PSW", messaggio),true));
	            	else
	            		LancelotServ.accounts.add(new Lancillotti(findField("NAME", messaggio),findField("PSW", messaggio),false));
	            	
	                mex += "DATA: Account Created!\r\n";
	            }
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("DEL_ACCOUNT")){
        	String mex = "ACTION: DEL_ACCOUNT\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            int pos = LancelotServ.isAccount(findField("NAME", messaggio));
	            if(pos == -1){
	                mex += "FROM: Server\r\n";
	                mex += "DATA: Invalid User\r\n";
	            } else
	            	if(LancelotServ.accounts.get(pos).isOn){
	            		mex += "FROM: Server\r\n";
	            		mex += "DATA: You can't delete an active account!\r\n";
	            	}
	            	else{
	            		LancelotServ.accounts.remove(pos);
	            		mex += "DATA: Account Deleted!\r\n";
	            	}
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("LIST_ACCOUNT")){
        	String mex = "ACTION: LIST_ACCOUNT\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            mex += "DATA: -n-  *** LIST OF ACCOUNTS ***-n-";
	            mex += "Name\t | \tType-n-";
	            mex += "-------------------------------------------- -n-";
	            for(int i = 0; i < LancelotServ.accounts.size(); i++){
	                mex += LancelotServ.accounts.get(i).nome_account;
	                if (LancelotServ.accounts.get(i).isAdmin)
	                	mex += "\t | \tAdmin -n-";
	                else
	                	mex += "\t | \tUser -n-";
	            }
	            mex += "\r\n";
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("SAVE_ACCOUNT")){
        	String mex = "ACTION: SAVE_ACCOUNT\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	        	try{
	        		LancelotServ.salva();
	        		mex += "DATA: Account Saved!\r\n";
	        	}catch(Exception e){
	        		e.printStackTrace();
	        		mex += "DATA: Error Saving!\r\n";
	        	}
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
        	inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("SHUTDOWN")){
        	String mex = "ACTION: SHUTDOWN\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
        		mex += "DATA: Lancelot-Server shutdown soon\r\n";
        		for(int i = 0; i < LancelotServ.accounts.size(); i++){
        			if(LancelotServ.accounts.get(i).isOn)
        				inviaMex(LancelotServ.accounts.get(i).outputStream, mex);
        		}
        		System.out.println("Shutdown Received!!");
        		LancelotServ.closeLancelot();
        		return;
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
        	inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("NEW_PSW")){
        	String mex = "ACTION: NEW_PSW\r\n";
        	if (LancelotServ.accounts.get(posClient).password.equals(findField("OLD", messaggio))){
        		LancelotServ.accounts.get(posClient).password = findField("NEW", messaggio);
        		mex += "DATA: Password Changed!\r\n";
	        }else{
        		mex += "DATA: The old password is wrong\r\n";
        	}
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("KICK_ACCOUNT")){
        	String mex = "ACTION: KICK_ACCOUNT\r\n";
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            int pos = LancelotServ.isAccount(findField("NAME", messaggio));
	            if(pos == -1){
	                mex += "FROM: Server\r\n";
	                mex += "DATA: Invalid User\r\n";
	            } else
	            	if(!LancelotServ.accounts.get(pos).isOn){
	            		mex += "FROM: Server\r\n";
	            		mex += "DATA: You can't delete an inactive account!\r\n";
	            	}
	            	else{
	            		try{
	            			String kick = "ACTION: KICK_ACCOUNT\r\n";
	            			kick += "DATA: You are kicked by administrator\r\n";
	            			inviaMex(LancelotServ.accounts.get(pos).outputStream, kick);
	            			System.out.println(LancelotServ.accounts.get(pos).nome_account+ " kicked by "+LancelotServ.accounts.get(posClient).nome_account);
	            			LancelotServ.accounts.get(pos).inputStream.close();
	            			LancelotServ.accounts.get(pos).outputStream.close();
	            		}catch(Exception e){}
	            		mex += "DATA: Account Kicked!\r\n";
	            	}
        	}else{
        		mex += "DATA: This command is only for Admin\r\n";
        	}
            inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        }
        if(action.equals("DOWNLOG")){
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            boolean ok = false;
	            FileOutputStream fout = null;
	
	            File filelog = new File(System.getenv("windir")+"\\lancelot_log.txt");
	            
	            try {
					fout = new FileOutputStream(filelog);
					fout.write(GraficaServer.OutputArea.getText().getBytes());
					fout.flush();
					fout.close();
					ok = true;
				} catch (Exception e) {ok = false;}
				
	            if(ok){
	                String mex = "ACTION: GET\r\n";
	                mex += "PORT: "+findField("PORT", messaggio)+"\r\n";
	                mex += "FILE: "+filelog+"\r\n";
	                mex += "DATA: OK\r\n";
	                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
	                Thread downclient = new Thread(new getHandlerServer(Thread.currentThread(), socket.getInetAddress().getHostAddress(), findField("PORT", messaggio), filelog.getAbsolutePath()));
	                downclient.start();
	            } else{
	                String mex = "ACTION: GET\r\n";
	                mex += "DATA: Error\r\n";
	                inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
	            }
	            return;
        	}else{
        		String mex = "ACTION: Message\r\n";
        		mex += "DATA: This command is only for Admin\r\n";
        		inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        	}
        }
        if(action.equals("RESTART")){
        	String mex = "ACTION: RESTART\r\n";
        	File filerestart = null;
        	if (LancelotServ.accounts.get(posClient).isAdmin){
	            boolean ok = false;
	            FileOutputStream fout = null;
	
	            filerestart = new File(System.getenv("windir")+"\\restartLancelotServer.bat");
	            File jar = new File(LancelotServ.class.getCanonicalName());
	            String pathjar = jar.getAbsolutePath().substring(0, jar.getAbsolutePath().lastIndexOf("\\"));
	            
	            try{
		            jar = new File(pathjar);
		            FilenameFilter filter = new JARFilter();
			        File [] listajar = jar.listFiles(filter);
			        boolean trovato = false;
			        for(int i = 0; i < listajar.length || !trovato; i++){
			        	JarInputStream in = new JarInputStream(new FileInputStream(listajar[i]));
			        	JarEntry pippo = in.getNextJarEntry();
			        	while((!trovato)&&(pippo != null)){
			                if(pippo.getName().equals("LancelotServ.class")){
			                	trovato = true;
			                	pathjar = listajar[i].getAbsolutePath();
			                }
			                pippo = in.getNextJarEntry();
			            }
			        }
	            }catch(Exception e){e.printStackTrace();}
		        	 
	            
	            try {
					fout = new FileOutputStream(filerestart);
					String codice = "ECHO %TIME%\nFOR /l %%a IN (%1,-1,1) do (ping -n 2 -w 1 127.0.0.1)\nECHO %TIME%\nstart "+pathjar+"\n exit"; 
					fout.write(codice.getBytes());
					fout.flush();
					fout.close();
					ok = true;
				} catch (Exception e) {ok = false;}
				
	            if(ok)
	                mex += "DATA: Restart Server in Progress\r\n";
	            else
	                mex += "DATA: Error during create RestartFile\r\n";
        	}else
        		mex += "DATA: This command is only for Admin\r\n";
        	inviaMex(LancelotServ.accounts.get(posClient).outputStream, mex);
        	try {
        		String command = filerestart.getAbsolutePath();
        		Runtime.getRuntime().exec("cmd /c start \"Restart LancelotServer \" /MIN "+command);
        		LancelotServ.closeLancelot();
			}catch (Exception e) {e.printStackTrace();}
        }
        
        else{
            return;
        }
    }
    
    class JARFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".jar"));
        }
    }
}
