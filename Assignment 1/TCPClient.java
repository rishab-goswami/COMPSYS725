/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*; 
class TCPClient { 
    
    public static void main(String argv[]) throws Exception
    {
        String sentence = "";
        String response;
        String type = "B";
        String fileName = "";
        int fileSize = 0;

        BufferedReader inFromUser =
	        new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket( "localhost", 6789);

        DataOutputStream outToServer =
	        new DataOutputStream(clientSocket.getOutputStream());


	    BufferedReader inFromServer =
	        new BufferedReader(new
		        InputStreamReader(clientSocket.getInputStream()));

	    response = inFromServer.readLine();
	    if(response.charAt(0) == '+'){
	        System.out.println("FROM SERVER: " + response);
        } else if(response.charAt(0) == '-'){
	        //Server is not available
        } else {
	        //Server is not responding correctly
        }

	    while(!sentence.equals("DONE")){
	        System.out.println("Command:");
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');

            response = response(inFromServer);

            if(sentence.contains("TYPE") && (response.charAt(0) == '+')){
                type = response.substring(7,8);
            } else if(sentence.contains("RETR") && (response.charAt(0) == '+')){
                fileName = sentence.substring(5);
                fileSize = Integer.parseInt(response.substring(1));
            } else if (sentence.contains("SEND") && (response.charAt(0) == '+')){
                try {
                    FileOutputStream fos = new FileOutputStream(fileName, false);
                    if(type.equals("A")){
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        for(int i=0; i < fileSize; i++){
                            bos.write(inFromServer.read());
                        }
                        bos.flush();
                        bos.close();
                    } else {
                        DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
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
                } catch(Exception e){
                    e.printStackTrace();
                }
            } else if (sentence.substring(0,4).equals("STOR") && (response.charAt(0) == '+')){
                fileName = sentence.split(" ")[2];
                File file = new File(fileName);
                if(!file.isFile()){
                    outToServer.writeBytes("File not found \n");
                    System.out.println("File not found on the server side, command stopped");
                    continue;
                }
                sentence= "SIZE " + Long.toString(file.length()) + "\n";
                fileSize = (int) file.length();
                outToServer.writeBytes(sentence);
                response(inFromServer);
                if(response.charAt(0) == '+'){
                    byte[] bytes = new byte[fileSize];
                    FileInputStream fis = new FileInputStream(file);
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    try {
                        if(type.equals("A")) {
                            BufferedInputStream bis = new BufferedInputStream(fis);

                            int p = 0;

                            while ((p = bis.read(bytes)) >= 0) {
                                outToServer.write(bytes, 0, p);
                            }
                            bis.close();
                            outToServer.flush();
                        } else {
                            while(fis.read(bytes) > 0){
                                dos.write(bytes);
                            }
                            fis.close();
                            dos.close();

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("FROM SERVER: " + response);
	    }
	    clientSocket.close();
	
    }

    private static String response(BufferedReader inFromServer) throws IOException{
        StringBuilder respondedText;
        String response;

        respondedText = new StringBuilder(inFromServer.readLine());

        while(inFromServer.ready()){
            respondedText.append(inFromServer.readLine());
            respondedText.append("\n");
        }
        response = respondedText.toString();
        return response;
    }

} 

