import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
// import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class helloworld {
    private static final int port = 8001;
    

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("The server is ready to receive on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                processRequest();


                /* Trying to log everything into the terminal */
                System.out.println(); //

                System.out.println("HTTP/1.1 " + "200 OK");
                System.out.println("Date: " + getCurrentTime());
                System.out.println("Server: MyWebServer");



                System.out.println("(Server) <======= request ======= (Client)");
    // After receiving a request, print out the request details
                String request = "GET /index.html HTTP/1.1"; // this would be captured from the client input
                System.out.println(request);
                System.out.println("Host: localhost: " + port);
                System.out.println("User-Agent: Mozilla");
                System.out.println();
                System.out.println();
                System.out.println();
                // ... other request headers ...

                // After processing the request and generating a response, print out the response details
                System.out.println("(Server) ======= response =======> (Client)");
                String response = "HTTP/1.1 200 OK"; // this would be the status line of the response
                System.out.println(response);
                System.out.println("Date: " + getCurrentTime()); // You would use a method to get the current time in proper format
                System.out.println("Server: MyWebServer");
                //System.out.println("Last-Modified: " + getLastModifiedTime(someFile)); // You would retrieve the last modified time
                System.out.println("Content-Type: text/html");
                //System.out.println("Content-Length: " + contentLength); // You would calculate the content length
                System.out.println("Connection: close"); 



                System.out.println(); //
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processRequest() throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());

            String requestLine = in.readLine();  //Read first line from http request
            if (requestLine == null || requestLine.isEmpty()) return; 

            String[] requestParts = requestLine.split(" ");  //Split request line by spaces
            if (requestParts.length < 3) return;  //Invalid request if < 3 elements found

            String method = requestParts[0];  //Get method
            String resourcePath = requestParts[1].substring(1); // Remove leading '/'

            // Only support GET and HEAD methods or send 501 Not Implimented Message
            if (!method.equals("GET") && !method.equals("HEAD")) {
                sendResponse(out, "501 Not Implemented", null, null, 0);
                return;
            }

            //LOcate the requested resource from the second element of the array
            File resourceFile = new File(resourcePath); //Create file object with extracted path
            //If the file does not exist or is a directory, send 404 Not Found message
            if (!resourceFile.exists() || resourceFile.isDirectory()) {
                sendResponse(out, "404 Not Found", null, null, 0);
                return;
            }

            
            // Additional logic for If-Modified-Since goes here...
            // Reading headers to find If-Modified-Since
            String headerLine;
            String ifModifiedSince = null;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("If-Modified-Since: ")) {
                ifModifiedSince = headerLine.substring("If-Modified-Since: ".length());
                break; // Assuming If-Modified-Since is the only header we're interested in
                }
            }

            String lastModified = getLastModifiedTime(resourceFile); //Retreive last modification time
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            Date lastModifiedDate;
            try {
                lastModifiedDate = dateFormat.parse(lastModified);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            boolean contentChanged = true;

            if (ifModifiedSince != null) {
                try {
                    Date ifModifiedSinceDate = dateFormat.parse(ifModifiedSince);
                    // Check if the content has been modified since the date specified in the header
                    lastModifiedDate = dateFormat.parse(lastModified);
                    contentChanged = lastModifiedDate.after(ifModifiedSinceDate);
                    
                } catch (ParseException e) {
                    e.printStackTrace(); // Log parsing error
                }
            }

            if (!contentChanged) {
                // If the content has not changed, send a 304 Not Modified response
                sendResponse(out, "304 Not Modified", null, null, 0);
            }
            else {
                // Content has changed, or no If-Modified-Since header was provided
                if (method.equals("GET")) {
                    byte[] fileContent = Files.readAllBytes(resourceFile.toPath());
                    sendResponse(out, "200 OK", lastModified, Files.probeContentType(resourceFile.toPath()), fileContent.length);
                    // Write file content to output stream if it's a GET request
                    if (fileContent.length > 0) {
                        clientSocket.getOutputStream().write(fileContent);
                        clientSocket.getOutputStream().flush();
                    }
                } else {
                    sendResponse(out, "200 OK", lastModified, null, 0);
                }
            }
        }

        private String getLastModifiedTime(File file) throws IOException {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            FileTime fileTime = Files.getLastModifiedTime(file.toPath());
            return dateFormat.format(new Date(fileTime.toMillis()));
        }

        private void sendResponse(PrintWriter out, String status, String lastModified, String contentType, long contentLength) {
            out.println("HTTP/1.1 " + status);
            out.println("Date: " + getCurrentTime());
            out.println("Server: MyWebServer");
            if (lastModified != null) out.println("Last-Modified: " + lastModified);
            if (contentType != null) out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + contentLength);
            out.println("Connection: close");
            out.println(); // Blank line between headers and content
            out.flush();


        }

        private String getCurrentTime() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.format(new Date());
        }
    }
}
