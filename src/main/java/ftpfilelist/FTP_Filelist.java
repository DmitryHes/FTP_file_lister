package ftpfilelist;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.io.FileUtils;


public class FTP_Filelist
{
	static boolean fullsubdir = false;
	static boolean isOut = false;
    static File outF;
    static FileWriter writeFile;
	
	public static void main(String[] args)
	{
	    boolean activeMode = false, anonimous = false, error = false;;
	    String outFile = null;
	    String username = null;
	    String userpass = null;
	    String subdir = null;
	    long fileCount = 0;
        	    
	    String server = null;//"ftp.mozilla.org";
		int port = 0;
		
		if (args.length == 0)
		{
			helpPrintOut();
			System.exit(1);
		}
		int i;
		int paramCount = 0;
		for (i=0; i<args.length; i++)
		{
			if (paramCount == 0)
			{
				if (args[i].equals("-a"))
				{
					activeMode = true;
				}
				else if (args[i].equals("-A"))
				{
					anonimous = true;
		            username = "anonymous";
		            userpass = System.getProperty("user.name")+"@mail.adr";
		 		}
				else if (args[i].equals("-f"))
				{
					fullsubdir = true;
				}
				else if (args[i].equals("-o") || args[i].equals("-out"))
				{
					isOut = true;
					paramCount = 1;
				}
				else
				{
					break;
				}
			}
			else
			{
				outFile = args[i];
				--paramCount;
			}
		}
		i--;
		while(i++ < args.length)
		{
			if (server == null)
			{
				server = args[i];
			}
			else if (username == null)
			{
				username = args[i];
			}
			else if (userpass == null)
			{
				userpass = args[i];
			}
			
		}
		if (server == null)
		{
			helpPrintOut();
			System.exit(1);
		}
		
	    String parts[] = server.split(":");
	    if (parts.length == 2)
	    {
	    	server=parts[0];
	        port=Integer.parseInt(parts[1]);
	    }
		
		
		FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
		FTPClient ftp = new FTPClient();
		ftp.configure(conf);
        try
        {
            int reply;
            if (port > 0) {
                ftp.connect(server, port);
            } else {
                ftp.connect(server);
            }
            System.out.println("Connected to " + server + " on " + (port>0 ? port : ftp.getDefaultPort()));
            ftp.setDataTimeout (60000); // Set the transfer timeout to 60 seconds
            ftp.setConnectTimeout (60000); //connection timeout is 60 seconds
            
            reply = ftp.getReplyCode();
            System.out.println(reply);
            
            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                System.exit(1);
            }
            
            System.out.print("login...");
            boolean login = ftp.login(username, userpass);
            if (login)
            {
            	System.out.println("OK");
            	
	            if (activeMode)//Switching  mode
	            {
	            	ftp.enterLocalActiveMode();
	            }
	            else
	            {
	            	ftp.enterLocalPassiveMode();
	            }
	            if (isOut)
	            {
		            try
		            {
		                outF = new File(outFile);
		                writeFile = new FileWriter(outF);
		            } 
		            catch (IOException e)
		            {
		                e.printStackTrace();
		            }
		            fileCount += ftpFileLists(ftp, "");
	                if(writeFile != null) 
	                {
	                    try
	                    {
	                        writeFile.close();
	                    } 
	                    catch (IOException e)
	                    {
	                        e.printStackTrace();
	                    }
	                }
	            }
	            else
	            {
	            	fileCount += ftpFileLists(ftp, "");
	            }
		        System.out.println("Total files: " + fileCount);
            }
            else
            {
            	System.out.println("FAIL");
            }
 
        }
        catch (IOException conn)
        {
            if (ftp.isConnected())
            {
                try
                {
                    ftp.disconnect();
                }
                catch (IOException disc)
                {
                 }
            }
            System.err.println("Could not connect to server.");
            System.exit(1);
        }
        try
        {
            //ftp.noop(); // check that control connection is working OK
            ftp.logout();
            System.out.println("Logout.");
        }
        catch (FTPConnectionClosedException e)
        {
            error = true;
            System.err.println("Server closed connection.");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            error = true;
            e.printStackTrace();
        }
        finally
        {
            if (ftp.isConnected())
            {
                try
                {
                    ftp.disconnect();
                    System.out.println("Disconnected from server.");
                }
                catch (IOException f)
                {
                    // do nothing
                }
            }
        }

        System.exit(error ? 1 : 0);
		

	}//~End of main

	static private long ftpFileLists(FTPClient ftp, String sub) throws IOException
	{
        FTPFile[] files = ftp.listFiles(sub);
        //System.out.println(files.length);
        int count = 0;
        for (FTPFile ftpFile : files)
        {
        	if (ftpFile.getType() == FTPFile.FILE_TYPE)
            {
                System.out.println((sub.isEmpty() ? sub : sub + "/" ) + ftpFile.getName()
                        + "   (" + FileUtils.byteCountToDisplaySize(ftpFile.getSize()) + ")");
                if (isOut)
                {
	                if(writeFile != null) 
	                {
	                    try
	                    {
	                        writeFile.write((sub.isEmpty() ? sub : sub + "/" ) + ftpFile.getName()
	                                + "   (" + FileUtils.byteCountToDisplaySize(ftpFile.getSize()) + ")\n");
	                    } 
	                    catch (IOException e)
	                    {
	                        e.printStackTrace();
	                    }
	                }

                }
                count++;
            }
        	if ((fullsubdir) && (ftpFile.getType() == FTPFile.DIRECTORY_TYPE))
        	{
        		count += ftpFileLists(ftp, (sub.isEmpty() ? sub : sub + "/" ) + ftpFile.getName());
        	}
        }
        return count;
	}
	
	private static void helpPrintOut()
	{
		System.out.println("Lists files specified ftp-server\n" + 
				"FTP_Filelist [-options] <FTP host> <username> <userpass>\n" + 
				"Ommited username and userpass is equal anonimous connect\n" + //To be released late
				"Options is:\n" + //To be released late
				"\t-a \t- switch to active mode (passive by default)\n" + //
				"\t-A \t- anonimous connect \n" + //
				"\t-f \t- list files in all subdirectories \n" + //
				"\t-o path or -out path \t- specify output file\n" + //specify output file
				"\t-? or -help \t- print this help message\n"); //help message
						
		
	}
	//~End of HelpPrintOut
	
}
