package webserver;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SimpleHttpServer {
        
        private int listenPort;
        private int statisticsPort;
        private String accessPath; 
        private String errorPath; 
        private String rootDir;
        private boolean runPHP;
        private List<String> denyAccess;
	private HttpServer httpServer;
        private HttpServer httpServerStats;
        private FileWriter outputStream = null;
        private PrintWriter logfile,errofile;
        
        private static final Date dateTime = Calendar.getInstance().getTime();
        private int connections;
        private int badRequest;
        private int notFound;
        private int methodNotAllowed;
        private int internalServerError;
        private long startTime;
        private static final int hours = Calendar.getInstance().get(Calendar.HOUR);
        private static final int minutes  = Calendar.getInstance().get(Calendar.MINUTE);
        private static final int seconds = Calendar.getInstance().get(Calendar.SECOND);
        private double averageServiceTime;
	
        private static final int HTTP_OK_STATUS = 200;
        private static final int HTTP_BAD_REQUEST_STATUS = 400;
        private static final int HTTP_NOT_FOUND_STATUS = 404;
        private static final int HTTP_METHOD_NA_STATUS = 405;
        private static final int HTTP_INTERNAL_SE_STATUS = 500;
	
        
        private static final String GAP = " - ";
        private static final String ARROW = " ->";

        private static final String[][] mime={
            {".avi",    "video/x-msvideo"},
            {".bmp",    "image/bmp"},
            {".gif",    "image/gif"},
            {".doc",    "application/msword"},
            {".jpg",    "image/jpeg"},
            {".htm",    "text/html"},
            {".html",   "text/html"},
            {".mp3",    "audio/mpeg"},
            {".mpg",   "video/mpeg"},
            {".mpg4",   "video/mpeg"},
            {".pdf",    "application/pdf"},
            {".png",    "image/png"},
            {".ppt",    "application/vnd.ms-powerpoint"},
            {".tiff",   "image/tiff"},
            {".txt",    "text/plain"},
            {".xls",    "application/vnd.ms-excel"}
        };
        
	public SimpleHttpServer( String context,String settings)  {
            this.connections = 0;
            this.badRequest = 0;
            this.notFound = 0;
            this.methodNotAllowed = 0;
            this.internalServerError = 0;
            this.averageServiceTime = 0;
            this.startTime = System.currentTimeMillis();
            DOMParser parser  = new DOMParser();
            try {
                parser.parse(settings);
            } catch (SAXException ex) {
                System.out.println("Error "+ex.getMessage());
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println("Error "+ex.getMessage());
                ex.printStackTrace();
            }
            Document document = parser.getDocument();
            parseXML(document);
            try {
                errofile = new PrintWriter(new FileOutputStream(errorPath, true));
            } catch (FileNotFoundException ex) {
                System.out.println("File "+errorPath+" can't be found. Server stops.");
            }
            try {
                logfile = new PrintWriter(new FileOutputStream(accessPath, true));
            } catch (FileNotFoundException ex) {
                System.out.println("File "+accessPath+" can't be found. Server stops.");
            }
            
            try {
			//Create HttpServer which is listening on the given port 
			httpServer = HttpServer.create(new InetSocketAddress(listenPort), 0);
			httpServerStats = HttpServer.create(new InetSocketAddress(statisticsPort), 0);
                        //Create a new context for the given context and handler
			httpServer.createContext(context,new GetHandler());
                        httpServerStats.createContext(context,new Stats());
			//Create a default executor
			httpServer.setExecutor(null);
                        httpServerStats.setExecutor(null);
		} catch (IOException e) {
			e.printStackTrace();
                        System.exit(1);
		}

	}

	/**
	 * Start.
	 */
	public void start() {
		this.httpServer.start();
                this.httpServerStats.start();
	}
        
         public final void parseXML(Document doc) { 
            Node helpNode;
            Element rootN = doc.getDocumentElement();
            NodeList childs;
            String nameNode;
            childs = rootN.getChildNodes();

                for (int i = 0; i < childs.getLength(); i++) {
                    helpNode = childs.item(i);
                    nameNode = helpNode.getNodeName();;
                    switch (nameNode) {
                        case "listen":  
                                        try{
                                            listenPort = Integer.parseInt(helpNode.getAttributes().item(0).getNodeValue());
                                            break;
                                        } catch(NumberFormatException ex) {
                                            System.out.println("Error "+ex.getMessage());
                                            ex.printStackTrace();
                                        }

                        case "statistics":  
                                            try {
                                                statisticsPort = Integer.parseInt(helpNode.getAttributes().item(0).getNodeValue());
                                                break;
                                            }catch(NumberFormatException ex) {
                                                System.out.println("Error "+ex.getMessage());
                                                ex.printStackTrace();
                                            }

                        case  "log":    accessPath = helpNode.getChildNodes().item(1).getAttributes().item(0).getNodeValue();
                                        errorPath = helpNode.getChildNodes().item(3).getAttributes().item(0).getNodeValue();
                                        break;

                        case "documentroot":   rootDir = helpNode.getAttributes().item(0).getNodeValue();
                                                break;

                        case "runphp": runPHP = helpNode.getTextContent().equals("yes");
                                        break;

                        case "denyaccess": NodeList ips;
                        ips = helpNode.getChildNodes();
                        //ELEGXO IPS > 0
                        denyAccess=new ArrayList<>();
                        for (int j = 0; j < ips.getLength(); j++) {
                            denyAccess.add(ips.item(j).getTextContent());
                        }
                        break;

                        default:    break;
                    }
                }    
        }
        
        public int getPort() {
                return listenPort;
        }
    
    public class GetHandler implements HttpHandler {    
        public void handle(HttpExchange t) throws IOException  {
            try {
                int HTTP_Status=HTTP_OK_STATUS;
                String uri = URLDecoder.decode(t.getRequestURI().toString());
                String method=t.getRequestMethod();
                StringBuilder response = new StringBuilder();
                long responseTime = System.currentTimeMillis();

                if (!method.equalsIgnoreCase("GET")) {
                    //Set the response header status and length
                    connections++;
                    methodNotAllowed++;
                    WriteLogs(t,uri,HTTP_METHOD_NA_STATUS,logfile);
                    t.sendResponseHeaders(HTTP_METHOD_NA_STATUS,-1);
                    return;
                }

                //check if exists index.html file
                if (uri.endsWith("/")) {
                    File index=new File(rootDir+uri+"index.html");
                    if (index.exists()) {
                        WriteLogs(t,uri,HTTP_OK_STATUS,logfile);
                        connections++;
                        servResource(t,"text/html",rootDir+uri+"index.html");
                        calculateServiceTime(responseTime);
                        return;
                    }
                    index=new File(rootDir+uri+"index.htm");
                    if (index.exists()) {
                        WriteLogs(t,uri,HTTP_OK_STATUS,logfile);
                        connections++;
                        servResource(t,"text/html",rootDir+uri+"index.htm");
                        calculateServiceTime(responseTime);
                        return;
                    }
                }

                String mimeType=getMimeType(uri.toLowerCase());
                if (mimeType!=null ) {
                    File index=new File(rootDir+uri);
                        if (!index.exists()) {
                            response.append("<h1>Server error: 404 Not Found</h1>");            
                            response.append("<h1>File "+uri +" was not found on the server</h1>");
                            HTTP_Status=HTTP_NOT_FOUND_STATUS;
                            connections++;
                            notFound++;
                            WriteLogs(t,uri,HTTP_NOT_FOUND_STATUS,logfile);
                            calculateServiceTime(responseTime);
                            response.append("</body>\n</html>\n");  
                            calculateServiceTime(responseTime);
                            t.sendResponseHeaders(HTTP_Status, response.toString().getBytes().length);
                            //Write the response string
                            OutputStream os = t.getResponseBody();
                            os.write(response.toString().getBytes());
                            os.close();                            
                        } else {                    
                            WriteLogs(t,uri,HTTP_OK_STATUS,logfile);
                            connections++;
                            servResource(t,mimeType,rootDir+uri);
                            calculateServiceTime(responseTime);
                        }
                        return;
                }
                /* display list of directory */
                SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                response.append("<html>\n");
                response.append("<head>\n");
                response.append("<link rel=\"shortcut icon\" href=\"data:image/x-icon;,\" type=\"image/x-icon\">"); 
                response.append("</head>\n");
                response.append("<body>");

                File index=new File(rootDir+uri);
                if (!index.exists()  ) {
                    response.append("<h1>Server error: 404 Not Found</h1>");            
                    response.append("<h1>File "+uri +" was not found on the server</h1>");
                    HTTP_Status=HTTP_NOT_FOUND_STATUS;
                    notFound++;
                }  else    
                if (index.isDirectory()) {
                    Path dir = Paths.get(rootDir+uri);
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                            response.append("<h1>CE325 HTTP  &nbsp;&nbsp;&nbsp;      Server</h1>\n");        
                            response.append("Current folder: " + uri);
                            response.append("<table> <tr>  <th>Filename</th> <th>Size</th> <th>Last Modified</th></tr>\n"); 
                            if (! (uri.equals("/"))) {
                                response.append("<tr><td class=\"link\"><a href=\"..\" >Parent Directory</a></td><td></td><td></td></tr>");
                            }
                            for (Path file: stream) {
                                File f=file.toFile();
                                String fname=file.getFileName().toString();
                                if (file.toFile().isDirectory()) fname=fname+"/";
                                response.append("<tr>");
                                response.append("<td><a href=\""+fname+"\">"+fname+"</a></td>");
                                response.append("<td>"+f.length()+"</td>");  
                                response.append("<td>"+sdf.format(f.lastModified())+"</td>");
                                response.append("</tr>\n");
                            }
                            response.append("</table>");
                        } catch (IOException | DirectoryIteratorException x) {
                            // IOException can never be thrown by the iteration.
                            // In this snippet, it can only be thrown by newDirectoryStream.
                            response.append("<h1>Server error: 500 Internal Server Error</h1>");              
                            HTTP_Status=HTTP_INTERNAL_SE_STATUS;
                            internalServerError++;
                        }
                } else {
                    response.append("<h1>Server error: 400 Bad Request</h1>");            
                    response.append("<h1>This type of file "+uri +" is not served on the server</h1>");
                    HTTP_Status=HTTP_BAD_REQUEST_STATUS;
                    badRequest++;
                }
                WriteLogs(t,uri,HTTP_Status,logfile);
                connections++;
                calculateServiceTime(responseTime);
                response.append("\nServer Statistics on Port :" + statisticsPort + "</body>\n</html>\n");  
                t.sendResponseHeaders(HTTP_Status, response.toString().getBytes().length);
                //Write the response string
                OutputStream os = t.getResponseBody();
                os.write(response.toString().getBytes());
                os.close();
                System.out.println("************ Response to Send ******************");                
            }
            catch (RuntimeException ex) {
                WriteErrors(t,ex, errofile);
            }
            catch (Error ex) {
                WriteErrors(t,ex, errofile);
            }
        }
        
        public void servResource(HttpExchange t, String type, String filename) throws FileNotFoundException, IOException {
            // add the required response header
            Headers h = t.getResponseHeaders();
            h.add("Content-Type", type);
            // read file
            File file = new File (filename);
            byte [] bytearray  = new byte [(int)file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(bytearray, 0, bytearray.length);

            // ok, we are ready to send the response.
            t.sendResponseHeaders(200, file.length());
            OutputStream os = t.getResponseBody();
            os.write(bytearray,0,bytearray.length);
            os.close();
        }
        
        public void WriteLogs(HttpExchange t,String uri,int status,PrintWriter file) {

            InetAddress address = t.getRemoteAddress().getAddress();
            StringBuilder logs = new StringBuilder();
            Date dateTime = Calendar.getInstance().getTime();   
            Collection<List<String>> agent =  t.getRequestHeaders().values();

            logs.append(address);
            logs.append(GAP);
            logs.append("[");
            logs.append(dateTime.toString());
            logs.append("] ");
            logs.append(t.getRequestMethod());
            logs.append(" ");
            logs.append(uri);
            logs.append(ARROW + " ");
            logs.append(status);
            switch(status) {
                case HTTP_OK_STATUS: logs.append(" OK ");
                                     break;
                case HTTP_BAD_REQUEST_STATUS: logs.append(" Bad Request ");
                                     break;
                case HTTP_NOT_FOUND_STATUS: logs.append(" Not Found ");
                                     break;                
                case HTTP_METHOD_NA_STATUS: logs.append(" Method Not Allowed ");
                                     break;                
                case HTTP_INTERNAL_SE_STATUS: logs.append(" Internal Server Error ");
                                     break;                
            }
            logs.append("\"User-Agent: ");
                int j=0;
                for (List<String> name : agent) {
                    if( j == 5) {
                        logs.append(name.toString());
                        logs.append("\"");
                    }
                    j++;
                }

            file.println(logs);
            file.flush();
            System.out.println(logs);
        }
        
        public void WriteErrors(HttpExchange t,Throwable ex,PrintWriter file) throws IOException {

            InetAddress address = t.getRemoteAddress().getAddress();
            StringBuilder errors = new StringBuilder();
            Date dateTime = Calendar.getInstance().getTime();   

            errors.append(address);
            errors.append(GAP);
            errors.append("[");
            errors.append(dateTime.toString());
            errors.append("] ");
            errors.append(t.getRequestHeaders().values());
            errors.append(" Details:\"");
            file.println(errors);
            ex.printStackTrace(file);
            file.flush();
        }
        
        private void calculateServiceTime(long startTime) {
        
            double prevSum = averageServiceTime * (connections -1);
            long servTime = System.currentTimeMillis() - startTime;
            averageServiceTime = (prevSum + servTime) / connections; 
        }
        
        String getMimeType(String uri) {
            for (String[] mime1 : mime) {
                if (uri.endsWith(mime1[0])) {
                    return mime1[1];
                }        
            }
            return null;
        }
    }
        
    class Stats implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
          String response = toHTMLString();
          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();
        }
        
        public String toHTMLString(){
            String temp = "<html><head><style> .size, .date {padding: 0 30px} h1.header {color: red; vertical-align: middle;}</style>";
            temp = temp + "<title>CE325 HTTP Server Statistics</title>";
            temp = temp + "<h1 class=\"header\">CE325 HTTP Server Statistics</h1></head><body><table><tr><th>Category</th><th>Statistics</th><tr><tr><td>Started At:</td><td>";
            temp = temp + dateTime.toString();
            temp = temp + "</td></tr><tr><td>Running for: </td><td>";
            long millis = System.currentTimeMillis() - startTime;
            long secondsM = (millis / 1000) % 60;
            long minutesM = (millis / (1000 * 60)) % 60;
            long hoursM = (millis / (1000 * 60 * 60)) % 24;
            long days = (long) (millis / (1000*60*60*24));
            temp = temp + days + " days, " + hoursM + " hours, " + minutesM + "min, " + secondsM + "sec";
            temp = temp + "</td></tr><tr><td>All Serviced Requests</td><td>:</td><td>" + connections;
            temp = temp + "</td></tr><tr><td>HTTP 400 Requests</td><td>:</td><td>" + badRequest;
            temp = temp + "</tr><tr><td>HTTP 404 Requests</td><td>:</td><td>" + notFound;
            temp = temp + "</tr><tr><td>HTTP 405 Requests</td><td>:</td><td>" + methodNotAllowed;
            temp = temp + "</tr><tr><td>HTTP 405 Requests</td><td>:</td><td>" + internalServerError;
            temp = temp + "</tr><tr><td>Average Service Time (in msec)</td><td>:</td><td>" + averageServiceTime + "</td></tr></table></body></html></tr>";

            return temp;
        }
    }
}


