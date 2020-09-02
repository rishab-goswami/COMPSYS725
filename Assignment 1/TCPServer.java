/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross,
 * All Rights Reserved.
 **/

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.jar.Attributes;

class TCPServer { 
    private static String accountName = "";
    private static String password = "";
    private static String userID = "";
    private static boolean loggedIn = false;
    private static String streamType = "B";
    private static String currentPath = System.getProperty("user.dir");
    private static String basePath = System.getProperty("user.dir");
	private static String loginMessage = "Please log in before using this command \n";
	private static Boolean verified = false;
	private static String tempPath;
	private static String tempAccount;
	private static String tempPassword;

    public static void main(String argv[]) throws Exception 
    { 
		String clientSentence;
		String toClient = null;
		String cmd;
		String args;
		String positiveGreeting = "+MIT-localhost SFTP Service" + '\n';
		String negativeGreeting = "-MIT-localhost Out to Lunch" + '\n';
		Boolean endConnection = false;
		//figure out when to send the negative response
		ServerSocket welcomeSocket = new ServerSocket(6789);

		Socket connectionSocket = welcomeSocket.accept();

		BufferedReader inFromClient =
			new BufferedReader(new
				InputStreamReader(connectionSocket.getInputStream()));

		DataOutputStream  outToClient =
			new DataOutputStream(connectionSocket.getOutputStream());

		outToClient.writeBytes(positiveGreeting);

		while(true) {
			//Need to make sure that the command is valid
			clientSentence = inFromClient.readLine();
			if(clientSentence.length() == 4){
				cmd = clientSentence.substring(0, 4);
				args = "end connection";
			} else if(clientSentence.length() >= 4) {
				cmd = clientSentence.substring(0, 4);
				args = clientSentence.substring(5);
			} else {
				cmd = "unrecognised command";
				args = "unrecognised arguments";
			}

			switch (cmd) {
				case "USER":
					toClient = USER(args);
					break;
				case "ACCT":
					toClient = ACCT(args);
					break;
				case "PASS":
					toClient = PASS(args);
					break;
				case "DONE":
					toClient = "+(the message may be charge/accounting info)";
					endConnection = true;
					break;
				case "TYPE":
					toClient = TYPE(args);
					break;
				case "LIST":
					toClient = LIST(args);
					break;
				case "CDIR":
					toClient = CDIR(args);

					if (toClient.charAt(0) == '+') {
						//add done to this
						while (toClient.charAt(0) == '+') {
							outToClient.writeBytes(toClient);
							clientSentence = inFromClient.readLine();
							//might want to broaden this to cover commands that are too short as well
							if (clientSentence.length() > 4) {
								cmd = clientSentence.substring(0, 4);
								args = clientSentence.substring(5);
							} else {
								System.out.println("1");
								cmd = "length too small";
								args = "length too small";
							}

							if(cmd.equals("ACCT")) {
								toClient = ACCT(args);
							} else if (cmd.equals("PASS")) {
								toClient = PASS(args);
							} else if (cmd.equals("length too small")) {
								toClient = "-Command needs to be at least 4 letters to be valid, try changing directory again \n";
							} else {
								toClient = "-Incorrect/invalid command, try changing directory again \n";
							}
						}

						if (toClient.charAt(0) == '!'){
							verified = true;
							tempAccount = "";
							tempPassword = "";
							toClient = CDIR(tempPath);
						} else {
							accountName = tempAccount;
							password = tempPassword;
							tempAccount = "";
							tempPassword = "";
						}
					}
					break;
				case "KILL":
					toClient = KILL(args);
					break;
				case "NAME":
					toClient = NAME(args);
					String oldName = args;

					if (toClient.charAt(0) == '+') {
						outToClient.writeBytes(toClient);
						clientSentence = inFromClient.readLine();

						if (clientSentence.length() > 4) {
							if (clientSentence.substring(0, 4).equals("TOBE")) {
								String newName = clientSentence.substring(5);
								toClient = TOBE(newName, oldName);
							} else {
								toClient = "-File wasn't renamed because the correct TOBE command was not given \n";
							}
						} else {
							toClient = "-File wasn't renamed because the TOBE command was not given \n";
						}
					}
					break;
				case "RETR":
					toClient = RETR(args);

					if(toClient.charAt(0) == '+'){
						
						while(!clientSentence.equals("SEND") && !clientSentence.equals("STOP")) {
							outToClient.writeBytes(toClient);

							clientSentence = inFromClient.readLine();

							if(clientSentence.length() < 4){
								toClient = "-Commands need to be at least 4 letters to be valid";
							} else if (!clientSentence.equals("SEND") && !clientSentence.equals("STOP")){
								toClient = "-Send a SEND or STOP command to continue";
							}
						}

						if(clientSentence.equals("STOP")){
							toClient = "+ok, RETR aborted";
						} else {
							String fileName = currentPath + "/" + args;
							File f = new File(fileName);
							byte[] buffer = new byte[(int) f.length()];

							if(streamType.equals("A")) { //ASCII transfer
								try (BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(f))){
									int p = 0;

									while((p = bufferedStream.read(buffer)) >= 0){
										outToClient.write(buffer, 0, p);
									}

									bufferedStream.close();
									outToClient.flush();
								} catch (Exception e){
									System.out.println("didn't work");
									e.printStackTrace();
								}
							} else {
								try{
									DataOutputStream dos = new DataOutputStream(connectionSocket.getOutputStream());
									FileInputStream fis = new FileInputStream(f);

									while(fis.read(buffer) > 0){
										dos.write(buffer);
									}
									fis.close();
									dos.close();
								} catch (Exception e){
									System.out.println("didn't work");
									e.printStackTrace();
								}
							}

						}
					}
					break;
				case "STOR":
					toClient = STOR(args);
					if(toClient.charAt(0) == '+'){
						outToClient.writeBytes(toClient);
						String argType = args.substring(0,3);
						String fileName = args.substring(4);

						clientSentence = inFromClient.readLine();
						if(clientSentence.length() < 6){
							toClient = "-Command not long enough \n";
							continue;
						}

						cmd = clientSentence.substring(0,4);
						args = clientSentence.substring(5);

						if(cmd.equals("SIZE")){
							int fileSize = Integer.parseInt(args);

							toClient = "+ok, waiting for file \n";
							outToClient.writeBytes(toClient);
							try{
								FileOutputStream fos = new FileOutputStream(currentPath + "/" + fileName, argType.equals("APP"));
								if(streamType.equals("A")){
									BufferedOutputStream bos = new BufferedOutputStream(fos);
									for(int i=0; i < fileSize; i++){
										bos.write(inFromClient.read());
									}
									bos.flush();
									bos.close();
								} else {
									DataInputStream dis = new DataInputStream(connectionSocket.getInputStream());
									byte[] buffer = new byte[fileSize];
									int read = 0;
									int totalRead = 0;
									int remaining = fileSize;

									while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0){
										totalRead += read;
										remaining -= read;
										fos.write(buffer, 0, read);
									}

									fos.close();
									dis.close();
								}
							} catch (Exception e) {
								e.printStackTrace();
								toClient = "-Unable to save file \n";
							}
						} else if (cmd.equals("STOP")){
							toClient = "-STOR aborted \n";
						} else {
							toClient = "-Invalid command, expected either SIZE followed by size of file in digits or the STOP command";
						}
					}

					break;
				default:
					toClient = "-Command not recognised \n";
					break;
			}
			
			outToClient.writeBytes(toClient);

			if(endConnection){
				connectionSocket.close();
				break;
			}
		}
    }

    private static String USER(String inputID){
		//Need to implement saved users
		BufferedReader reader;
		try{
			reader = new BufferedReader(
						new FileReader(System.getProperty("user.dir") + "/User-Data.txt"));

			String line = reader.readLine();
			while(line != null){
				String [] userData = line.split(",");
				String storedID = userData[0];
				String storedName = userData[1];
				String storedPassword = userData[2];
				String verifiedUser = userData[3];

				if(storedID.equals(inputID)){
					if(verifiedUser.equals("V")){
						accountName = storedName;
						password = storedPassword;
						loggedIn = true;
						userID = inputID;
						return "!" + inputID + " logged in \n";
					} else {
						userID = inputID;
						loggedIn = false;
						accountName = "";
						password = "";
						return "+User-id valid, send account and password \n";
					}
				}

				line = reader.readLine();
			}
			reader.close();
			loggedIn = false;
			accountName = "";
			password = "";
			return "-Invalid user-id, try again \n";
		} catch (IOException e){
			e.printStackTrace();
			return "-" + e;
		}
	}

	private static String ACCT(String inputAccount){
		if(userID.equals("")){
			return "-Please use USER command first \n";
		}
    	BufferedReader reader;
		try{
			reader = new BufferedReader(
					new FileReader(System.getProperty("user.dir") + "/User-Data.txt"));

			String line = reader.readLine();
			while(line != null){
				String [] userData = line.split(",");
				String storedID = userData[0];
				String storedName = userData[1];
				String storedPassword = userData[2];

				if(storedName.equals(inputAccount) && userID.equals(storedID)){
					if(password.equals(storedPassword)){
						accountName = storedName;
						loggedIn = true;
						return "!Account valid, logged in \n";
					} else {
						accountName = storedName;
						return "+Account valid, send password \n";
					}
				}

				line = reader.readLine();
			}
			reader.close();
			return "-Invalid account, try again \n";
		} catch (IOException e){
			e.printStackTrace();
			return "-" + e;
		}
	}

	private static String PASS(String inputPassword){
		if(userID.equals("")){
			return "-Please use USER command first \n";
		}
		BufferedReader reader;
		try{
			reader = new BufferedReader(
					new FileReader(System.getProperty("user.dir") + "/User-Data.txt"));

			String line = reader.readLine();
			while(line != null){
				String [] userData = line.split(",");
				String storedID = userData[0];
				String storedName = userData[1];
				String storedPassword = userData[2];

				if(storedPassword.equals(inputPassword) && userID.equals(storedID)){
					if(accountName.equals(storedName)){
						password = inputPassword;
						loggedIn = true;
						return "!Logged in \n";
					} else {
						password = inputPassword;
						return "+Send account \n";
					}
				}

				line = reader.readLine();
			}
			reader.close();
			return "-Wrong password, try again \n";
		} catch (IOException e){
			e.printStackTrace();
			return "-" + e;
		}
	}

	//Ensure that we have gotten the ! before letting people use this.
	private static String TYPE(String type){
    	if(!loggedIn){
    		return loginMessage;
		}
    	//argument needs to be upercase to be counted
    	if(type.equals("A")){
    		streamType = type;
			return "+Using Ascii mode \n";
		} else if (type.equals("B")){
    		streamType = type;
			return "+Using Binary mode \n";
		} else if (type.equals("C")){
    		streamType = type;
			return "+Using Continuous mode \n";
		} else {
    		return "-Type not valid \n";
		}
	}

	//need to differentiate between a null directory path and an invalid directory path
	private static String LIST(String path){
		if(!loggedIn){
			return loginMessage;
		}

    	String [] pathNames;
    	String type = path.substring(0,1);
    	String directory;
		StringBuilder returnMessageMaker;
		String returnMessage;

		if(path.length() > 2) {
			directory = path.substring(2);
		} else {
			directory = currentPath;
		}

		File f = new File(directory);
		pathNames = f.list();

    	if(type.equals("F")){
    		try {
    				returnMessageMaker = new StringBuilder("+" + directory + "\n");
					System.out.println(returnMessageMaker);

					if(!f.exists()){
						return "-Path doesn't exist \n";
					}

					for(String pathName : pathNames) {
						System.out.println(returnMessageMaker);
						returnMessageMaker.append(pathName);
						returnMessageMaker.append("\n");
					}

					returnMessage = returnMessageMaker.toString();
					return returnMessage;
			}catch (Exception e){
    			e.printStackTrace();
    			return "-" + e + "\n";
			}
		} else if (type.equals("V")){
			try {
				returnMessageMaker = new StringBuilder("+" + directory + "\n");
				System.out.println(returnMessageMaker);

				if(!f.exists()){
					return "-Path doesn't exist \n";
				}

				for(String pathName : pathNames) {
					returnMessageMaker.append(pathName);
					returnMessageMaker.append("\n");
				}

				returnMessage = returnMessageMaker.toString();
				return returnMessage;
			}catch (Exception e){
				e.printStackTrace();
				return "-" + e + "\n";
			}
		} else {
    		return "-invalid LIST command please try again \n";
		}
	}
	//Need to decide when to ask for an account and password
	//Might need to check if it is a directory or a file
	//if the path isn't part of the base path (where the code is) then they need to log in again.
	//Need to decide on reasons for why it can't connect to directory including not enough permissions to go into specific file
	private static String CDIR(String path){
		if(!loggedIn){
			return loginMessage;
		}

		File f = new File(path);

		if(f.exists()){
			tempPath = path;

			if(f.isFile()){
				return "-Path specified is a file, please specify a directory";
			}

			if(verified){
				currentPath = tempPath;
				tempPath = "";
				return "!Changed working dir to " + currentPath + "\n";
			} else {
				tempAccount = accountName;
				tempPassword = password;
				accountName = "";
				password = "";
				return "+directory ok, send account/password \n";
			}
		} else {
			return "-Can't connect to directory because: Directory doesn't exist \n";
		}
	}

	//Might need to check if we are allowed to include folder or just files
	private static String KILL(String file){
		if(!loggedIn){
			return loginMessage;
		}

    	File f = new File(currentPath + "/" + file);

    	if (f.delete()) {
			return "+" + file + " deleted \n";
		} else {
			return "-File does not exist \n";
    	}
    }
	//Assumes that the file is in the current directory, therefore doesn't need the full path.
	private static String NAME(String fileName){
		if(!loggedIn){
			return loginMessage;
		}

    	File f = new File(currentPath + "/" + fileName);

    	if(f.exists()){
			int index = fileName.indexOf(".");
			if(index == -1){
				return "-Unable to change file name. No file type specified \n";
			}
    		return "+File exists \n";
		} else {
			return "-Can't find " + fileName + "\n";
		}
	}

	private static String TOBE(String newName, String oldName){
    	File newFile = new File(currentPath + "/" + newName);
    	File oldFile = new File(currentPath + "/" + oldName);

    	if(newFile.exists()){
    		return "-There is already a file with that name, try again \n";
		}

    	int index = newName.indexOf(".");
    	if(index == -1){
    		return "-Unable to change file name. No file type specified \n";
		}

		if(oldFile.renameTo(newFile)){
			return "+change successful \n";
		} else {
			return "-error \n";
		}
	}

	private static String RETR(String fileSpec){
		if(!loggedIn){
			return loginMessage;
		}
		File f = new File(currentPath + "/" +fileSpec);

		if(f.exists()){
			String length = "+" + Long.toString(f.length()) + "\n";

			if(streamType.equals("A") && isBinary(f)){
				return "-File type is Binary and current TYPE is A. Switch to B or C or send a binary file \n";
			} else if ((streamType.equals("B") || streamType.equals("C")) && !isBinary(f)){
				return "-File type is ASCII and current TYPE is B or C. Please switch type to A or send binary file";
			} else {
				return length;
			}
		} else {
			return "-File doesn't exist. Please try again \n";
		}
	}

	private static String STOR(String fileSpec){
		if(!loggedIn){
			return loginMessage;
		}

		if(fileSpec.length() < 8){
			return "-STOR command needs 2 arguments, NEW | OLD | APP and file name";
		}

		File f = new File(currentPath + "/" + fileSpec.substring(4));

		if(fileSpec.substring(0,3).equals("NEW")){
			if(f.isFile()){
				return "-File exists, but system doesn't suppose generations \n";
			}else {
				return "+File does not exist, will create new file \n";
			}
		} else if(fileSpec.substring(0,3).equals("OLD")){
			if(f.isFile()){
				return "+Will write over old file \n";
			}else {
				return "+Will create new file \n";
			}
		}else if(fileSpec.substring(0,3).equals("APP")){
			if(f.isFile()){
				return "+Will append to new file \n";
			}else {
				return "+Will create file \n";
			}
		} else {
			return "-incorrect argument. the first argument needs to be NEW OLD OR APP \n";
		}

	}

	private static Boolean isBinary(File file){
    	FileInputStream fis;

    	try {
    		fis = new FileInputStream(file);
    		int size = fis.available();

    		if(size > 32) size = 32; //only checking the first 32 bytes
			byte[] data = new byte[size];

			fis.read(data);
			fis.close();

			int ascii = 0;
			int binary = 0;

			for(int i =0; i < data.length; i++){
				byte b = data[i];

				if(b < 0x09) {
					return true;
				}

				if( b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D ){
					ascii++;
				}
				else if( b >= 0x20  &&  b <= 0x7E ){
					ascii++;
				}
				else {
					binary++;
				}
			}

			if(binary == 0) {
				return false;
			}

			return (100* binary/(binary + ascii) > 95);
		} catch (FileNotFoundException ex){
		} catch (IOException io){
		}
    	return false;
	}
} 


