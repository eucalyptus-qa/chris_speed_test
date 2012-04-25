import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SshUtil {
  private static Logger LOG = Logger.getLogger( SshUtil.class );
  public static String exec( String keyName, String key, String host, String command ) throws Exception {
    JSch jsch = new JSch( );
    LOG.debug( String.format( "Trying SSH with: \nkey %s \nprivatekey %s\nhost %s\ncommand %s\n", keyName, key, host, command ));
    jsch.addIdentity( keyName, key.getBytes( ), null, null );
    Session session = jsch.getSession( "root", host, 22 );
    session.setConfig( "StrictHostKeyChecking", "no" );
    try {
      session.connect( 60000 );
      session.setTimeout( 60000 );
      Channel channel = session.openChannel( "exec" );
      try {
        ChannelExec exec = ( ( ChannelExec ) channel );
        exec.setCommand( command );
        InputStream in = channel.getInputStream( );
        channel.connect( 3 * 1000 );
        StringBuffer out = new StringBuffer( );
        byte[] tmp = new byte[1024];
        int len = 0;
        while ( ( len = in.read( tmp, 0, 1024 ) ) >= 0 ) {
          out.append( Charset.defaultCharset( ).decode( ByteBuffer.wrap( tmp, 0, len ) ) );
          if ( channel.isClosed( ) ) break;
        }
        return out.toString( );
      } finally {
        channel.disconnect( );
      }
    } catch ( Exception e ) {
      Exception ex = e;
      try {
        if ( e.getMessage( ) != null ) {
          if ( e.getMessage( ).matches( ".*java.net.*" ) ) {
            ex = ( IOException ) Class.forName( e.getMessage( ).replaceAll(".*(java\\.net)","java.net").replaceAll(":.*","") ).newInstance( );
            ex.initCause( e );
          } else if ( e.getMessage( ).matches( "timeout.*" ) ) {
            ex = new ConnectException( e.getMessage( ) );
          }
        }
      } catch ( Throwable e1 ) {
        ex = new RuntimeException( e1 );
      }
      throw ex;
    } finally {
      session.disconnect( );
    }
  }
    
}
