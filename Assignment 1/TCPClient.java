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

        BufferedReader inFromUser =
	        new BufferedReader(new InputStreamReader(System.in));

        Socket clientSocket = new Socket( "localhost", 6789);

        DataOutputStream outToServer =
	        new DataOutputStream(clientSocket.getOutputStream());


	    BufferedReader inFromServer =
	        new BufferedReader(new
		        InputStreamReader(clientSocket.getInputStream()));

	    response = inFromServer.readLine();
	    if(response.substring(0,1).equals("+")){
	        System.out.println("FROM SERVER: " + response);
        } else if(response.substring(0,1).equals("-")){
	        //Server is not available
        } else {
	        //Server is not responding correctly
        }

	    while(!sentence.equals("DONE")){
	        System.out.println("Command:");
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');

            response = response(inFromServer);
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
