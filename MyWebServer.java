import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.nio.file.Path;

//code made by Connor Persaud and Manyaka Anjorin

public class MyWebServer {
    private static ServerSocket serverSocket;
    private static int port;

    public static void main(String[] args) throws IOException {
        port = 8888;

        serverSocket = new ServerSocket(port);
        System.out.println("The server is ready to receive on port " + port + "\n");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                // ###### Fill in Start ######
                String line = null;
                String response = null;
                boolean ok = true;
                String conType = "";
                String dateMod = "";
                ArrayList<ArrayList<String>> httpLineLst= new ArrayList<ArrayList<String>>();
                response = "HTTP/1.1 200 OK";
                SimpleDateFormat form = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                Date curDate = new Date();
                String curDateString = form.format(curDate);
                String host = "MyWebServer";
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                int count = 0;
                while( (line=socketInput.readLine()) != null && !line.equals("")) {
                    ArrayList<String> placeHolder= new ArrayList<String>();
                    System.out.println(line);
                    httpLineLst.add(placeHolder);
                    httpLineLst.get(count).addAll(Arrays.asList(line.split(" ")));  //add each line to ArrayList to call elements later
                    count++;
                }

                
                String command = httpLineLst.get(0).get(0); //retreive method
                String url = httpLineLst.get(0).get(1).substring(1,httpLineLst.get(0).get(1).length());//retreive url
                File f = new File(url);     //retreive file
                if (!command.equals("GET") && !command.equals("HEAD")){
                        response = "HTTP/1.1 501 Not Implemented";
                        ok = false;

                }else{
                    
                    if (!f.exists()){
                        response = "HTTP/1.1 404 Not Found";
                        ok = false;

                    }else{              //format date to check if has been modified since
                        Path file = Path.of(url);
                        conType = Files.probeContentType(file);
                        FileTime lastModi = Files.getLastModifiedTime(file);
                        Date d1= new Date (lastModi.toMillis());
                        dateMod = form.format(d1);
                        Date date1 = form.parse(dateMod);
                        try {       //catch if it doesnt contain "If-Modified-Since:"
                        String lastModTrue = httpLineLst.get(7).get(0);
                        String lastMod = httpLineLst.get(7).get(1)+" "+httpLineLst.get(7).get(2);
                        Date date2 = form.parse(lastMod);
                        if(lastModTrue.equals("If-Modified-Since:")){

                            if(date1.equals(date2) || date2.after(date1)){
                                response = "HTTP/1.1 304 Not Modified";

                                ok = false;
                            }
                        }
                    } catch (Exception e) {}
                    }
                }
            
                    
                
                System.out.println("\n");
                PrintWriter socketOutput = new PrintWriter(clientSocket.getOutputStream(), true);

                socketOutput.println(response);     //start socket response
                if(ok == true && command.equals("GET")){    //Get HTTP/1.1 200 OK to socket
                    socketOutput.println("content-length: " + f.length());
                    socketOutput.println("content-type: "+conType);
                    socketOutput.println("Date: "+curDateString);
                    socketOutput.println("Last-Modified: "+dateMod);
                    socketOutput.println("Server: "+host+ "\n");

                    StringBuilder fileAsString = new StringBuilder();
                    FileReader fR = null;
                    try {                          // reads file and prints it to response data
                        fR = new FileReader(f);
                        int fileLine;
                        char buf[] = new char[4096];
                        while ((fileLine = fR.read(buf)) != -1) {
                            fileAsString.append(buf, 0, fileLine);
                        }
                    } catch (Exception e) {
                        System.out.println("Error");
                    } finally {
                        fR.close();
                    }
                    
                    String result = fileAsString.toString();
                    socketOutput.println(result);
                    
                }else if(ok == true && command.equals("HEAD")){     // HEAD HTTP/1.1 200 OK to socket
                    socketOutput.println("content-length: 0");
                    socketOutput.println("Date: "+curDateString);
                    socketOutput.println("Last-Modified: "+dateMod);
                    socketOutput.println("Server: "+host);
                }else{
                    socketOutput.println("content-length: 0");      //HTTP/1.1 Not 200 OK to socket
                    socketOutput.println("Date: "+curDateString);
                    socketOutput.println("Server: "+host);
                }
                
                socketOutput.flush();
                
                
                System.out.println(response);                           //start system response
                System.out.println("Date: "+curDateString);
                System.out.println("Server: "+host);

                if(ok == true){                                     //HTTP/1.1 200 OK
                    System.out.println("Last-Modified: "+dateMod);
                }
                if(command.equals("GET")&& ok == true){     //Get HTTP/1.1 200 OK
                    System.out.println("Content-Type: "+conType);
                }
                if(ok == true ){                                                //HTTP/1.1 200 OK to system
                    System.out.println("Content-Length: : "+f.length());
                }else{                                                  //HTTP/1.1 Not 200 OK to system
                    System.out.println("Content-Length: : 0");
                }
                System.out.println();

                // ###### Fill in End ######

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
//If-Modified-Since: 01/31/2025 02:57:03
//http://localhost:8888/index.html