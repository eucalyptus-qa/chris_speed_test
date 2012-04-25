import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSchException;

public class SshClientTest {
  private static Logger LOG = Logger.getLogger( SshUtil.class );
  
  private String key;
  private String command = "ip addr show dev eth0";
  private String name = "testingstuff";

  @Before public void readKey() throws Exception {
    File sshKey = new File(System.getProperty( "user.home" ) + File.separator + ".ssh" + File.separator + "id_rsa" );
    if( !sshKey.exists( ) ) {
      sshKey = new File(System.getProperty( "user.home" ) + File.separator + ".ssh" + File.separator + "id_dsa" );
    }
    Scanner in = new Scanner(sshKey);
    key = in.nextLine( );
    while( in.hasNextLine( ) ) key += "\n"+in.nextLine( );    
  }
  
  @Test
  public void good( ) throws Exception {
    SshUtil.exec( name, key, "chris", command );
  }

  @Test(expected=JSchException.class)
  public void badKey( ) throws Exception {
     SshUtil.exec( name, key, "gibson", command );
  }
  
  @Test(expected=JSchException.class)
  public void invalidKey( ) throws Exception {
     SshUtil.exec( name, key, "gibson", command );
  }

  @Test(expected=ConnectException.class)
  public void noSshdOnHost( ) throws Exception {
     SshUtil.exec( name, key, "192.168.7.153"/*dan's phone*/, command );
  }
  
  @Test(expected=NoRouteToHostException.class)
  public void hostDown( ) throws Exception {
     SshUtil.exec( name, key, "192.168.7.251", command );
  }
  
  @Test(expected=NoRouteToHostException.class)
  public void invalidRoute( ) throws Exception {
     SshUtil.exec( name, key, "224.66.66.66", command );
  }
  
  @Test(expected=ConnectException.class)
  public void nullRouted( ) throws Exception {
     SshUtil.exec( name, key, "google.com", command );
  }
}
