package webserver;
import java.io.IOException;
import org.xml.sax.SAXException;

public class HttpServerTest {

	private static final String CONTEXT = "/";

	public static void main(String[] args) throws SAXException, IOException {

                // Create a new SimpleHttpServer
		SimpleHttpServer simpleHttpServer = new SimpleHttpServer(CONTEXT,args[0]);
		// Start the server
		simpleHttpServer.start();
                System.out.println("Server is started and listening on port "+ simpleHttpServer.getPort());

        }
}
