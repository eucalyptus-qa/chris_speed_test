import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Logger;

import com.xerox.amazonws.ec2.Jec2;

public abstract class TestContext {
  public static String[] EMPTY = {}; 
  private boolean managed = true;
  private Jec2 myClient;
  private UUID name;
  
  public TestContext( ) throws Exception {
    String url = System.getenv( "EC2_URL" );
    URL eucaUrl = new URL( url );
    myClient = new Jec2( System.getenv( "EC2_ACCESS_KEY" ), System.getenv( "EC2_SECRET_KEY" ), false, eucaUrl.getHost( ), eucaUrl.getPort( ) );
    myClient.setResourcePrefix( "/services/Eucalyptus" );
    myClient.setMaxConnections( 16000 );
    myClient.setMaxRetries( 10 );
    MultiThreadedHttpConnectionManager connMgr = new MultiThreadedHttpConnectionManager();
    HttpConnectionManagerParams connParams = connMgr.getParams();
    connParams.setMaxTotalConnections(16000);
    connParams.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, 16000);
    connParams.setConnectionTimeout(0);
    connParams.setSoTimeout(0);
//    connParams.setReceiveBufferSize( 1024 );
//    connParams.setSendBufferSize( 1024 );
    connParams.setTcpNoDelay( true );
    connMgr.setParams(connParams);
    HttpClient hc = new HttpClient( connMgr );
    myClient.setHttpClient( hc );
    this.name = UUID.randomUUID( );
  }

  public Jec2 client() {
    return this.getClient( );
  }

  public String unique( String arg ) {
    return String.format( "%s-%s", arg, this.name.toString( ) );
  }
  public String unique( Object arg ) {
    return String.format( "%s.%s", arg.toString( ), this.name.toString( ) ).replaceAll("-","");
  }
  public boolean isManaged( ) {
    return this.managed;
  }

  public void setManaged( boolean managed ) {
    this.managed = managed;
  }

  public Jec2 getClient( ) {
    return this.myClient;
  }

  public void setClient( Jec2 client ) {
    this.myClient = client;
  }
  
  public UUID getName( ) {
    return this.name;
  }

  public void setName( UUID name ) {
    this.name = name;
  }

  private static String SEP = ":";
  public void header( Object... args ) {
    StringBuffer sb = new StringBuffer( ).append( SEP ).append( this ).append( SEP ).append( System.currentTimeMillis( ) ).append( SEP );
    for( Object s : args ) sb.append( s.toString( ) ).append( SEP );
    LOG.info( sb.toString( ) );
  }
private static Logger LOG = Logger.getLogger( TestContext.class );
  public String toString() {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[5];
    return new StringBuffer().append( this.getName( ).toString( ) )
      .append( ":" ).append( ste.getFileName( ) )
      .append( ":" ).append( ste.getMethodName( ) )
      .append( ":" ).append( ste.getLineNumber( ) ).toString( );
  }
  
  
}
