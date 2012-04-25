import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.KeyPairInfo;


public class TestKeypairs {
  private static Log LOG = LogFactory.getLog(TestKeypairs.class);

  public static void main(String [] args) throws Exception {
    Jec2 ec2 = new Jec2(System.getenv( "EC2_ACCESS_KEY" ), System.getenv( "EC2_SECRET_KEY" ), false, "192.168.7.7", 8773 );
    ec2.setResourcePrefix( "/services/Eucalyptus" );
    
  
    List<ImageDescription> images = ec2.describeImages( new String[]{} );
/*
    logger.info("Available Images");
    for (ImageDescription img : images) {
      if (img.getImageState().equals("available")) {
        logger.info(img.getImageId()+"\t"+img.getImageLocation()+"\t"+img.getImageOwnerId());
        if (img.getProductCodes() != null) {
          logger.info("          product code : "+img.getProductCodes().get(0));
        }
      }
    }

    List<AvailabilityZone> zones = ec2.describeAvailabilityZones(null);
    for (AvailabilityZone zone : zones) {
      logger.info("zone : "+zone.getName()+" state : "+zone.getState());
    }
    List<AddressInfo> addrs = ec2.describeAddresses(null);
    for (AddressInfo info : addrs) {
      logger.info("address : "+info.getPublicIp()+" instance : "+info.getInstanceId());
    }
*/
//    String publicIp = ec2.allocateAddress();
//    logger.info("Address allocated : "+publicIp);

//    ReservationDescription runInst = ec2.runInstances(new LaunchConfiguration("ami-20b65349", 1, 1));
//    ReservationDescription runInst = ec2.runInstances("ami-36ff1a5f", 1, 1, new ArrayList<String>(), null, "dak-keypair", true, InstanceType.LARGE, "us-east-1c", null, null, null);

/*
    params = new ArrayList<String>();
    List<ReservationDescription> instances = ec2.describeInstances(params);
    logger.info("Instances");
    String instanceId = "";
    for (ReservationDescription res : instances) {
      logger.info(res.getOwner()+"\t"+res.getReservationId());
      if (res.getInstances() != null) {
        for (Instance inst : res.getInstances()) {
          logger.info("\t"+inst.getImageId()+"\t"+inst.getDnsName()+"\t"+inst.getState()+"\t"+inst.getKeyName()+"\t"+inst.getInstanceType().getTypeId());
          instanceId = inst.getInstanceId();
        }
      }
    }

//    ec2.associateAddress(runInst.getInstances().get(0).getInstanceId(), publicIp);

//    addrs = ec2.describeAddresses(null);
//    for (AddressInfo info : addrs) {
//      logger.info("address : "+info.getPublicIp()+" instance : "+info.getInstanceId());
//    }
//    ec2.disassociateAddress(publicIp);
//    ec2.releaseAddress(publicIp);

//    ec2.terminateInstances(new String [] {runInst.getInstances().get(0).getInstanceId()});

    // confirm product instance
/*
    ReservationDescription res = ec2.runInstances("ami-45997c2c", 1, 1, new ArrayList<String>(), null, "dak-keypair");
    ProductInstanceInfo pinfo = ec2.confirmProductInstance(res.getInstances().get(0).getInstanceId(), "BA7154BF");
    if (pinfo == null) {
      logger.info("no relationship here");
    }
    else {
      logger.info("relationship confirmed. owner = "+pinfo.getOwnerId());
    }
*/

    // test console output
/*
    ConsoleOutput consOutput = ec2.getConsoleOutput(instanceId);
    logger.info("Console Output:");
    logger.info(consOutput.getOutput());
*/

    // test keypair methods
/*
*/
    testKeypairs( ec2 );

    // test security group methods
/*
    List<GroupDescription> info = ec2.describeSecurityGroups(new String [] {});
    logger.info("SecurityGroup list");
    for (GroupDescription i : info) {
      logger.info("group : "+i.getName()+", "+i.getDescription()+", "+i.getOwner());
    }
    ec2.createSecurityGroup("test-group", "My test security group");
    info = ec2.describeSecurityGroups(new String [] {});
    logger.info("SecurityGroup list");
    for (GroupDescription i : info) {
      logger.info("group : "+i.getName()+", "+i.getDescription());
    }
    ec2.authorizeSecurityGroupIngress("default", "tcp", 1000, 1001, "0.0.0.0/0");
    ec2.revokeSecurityGroupIngress("default", "tcp", 1000, 1001, "0.0.0.0/0");
    ec2.authorizeSecurityGroupIngress("default", "tcp", 1000, 1001, "0.0.0.0/0");
    ec2.revokeSecurityGroupIngress("default", "tcp", 1000, 1001, "0.0.0.0/0");

    ec2.authorizeSecurityGroupIngress("default", "test-group", "291944132575");
    ec2.revokeSecurityGroupIngress("default", "test-group", "291944132575");

    ec2.deleteSecurityGroup("test-group");
    info = ec2.describeSecurityGroups(new String [] {});
    logger.info("GroupDescription list");
    for (GroupDescription i : info) {
      logger.info("group : "+i.getName()+", "+i.getDescription());
    }

    // test image attribute methods
    DescribeImageAttributeResult res = ec2.describeImageAttribute("ami-45997c2c", ImageAttributeType.launchPermission);
    Iterator<ImageListAttributeItem> iter = res.getImageListAttribute().getImageListAttributeItems().iterator();
    logger.info("image attrs");
    while (iter.hasNext()) {
      ImageListAttributeItem item = iter.next();
      logger.info("image : "+res.getImageId()+", "+item.getType()+"="+item.getValue());
    }
/*
    LaunchPermissionAttribute attr = new LaunchPermissionAttribute();
    attr.getImageListAttributeItems().add(new ImageListAttributeItem(ImageListAttributeItemType.userId, "291944132575"));
    ec2.modifyImageAttribute("ami-11816478", attr, ImageListAttributeOperationType.add);
    res = ec2.describeImageAttribute("ami-11816478", ImageAttributeType.launchPermission);
    iter = res.getImageListAttribute().getImageListAttributeItems().iterator();
    logger.info("image attrs");
    while (iter.hasNext()) {
      ImageListAttributeItem item = iter.next();
      logger.info("image : "+res.getImageId()+", "+item.getValue());
    }
    ec2.resetImageAttribute("ami-11816478", ImageAttributeType.launchPermission);
    res = ec2.describeImageAttribute("ami-11816478", ImageAttributeType.launchPermission);
    iter = res.getImageListAttribute().getImageListAttributeItems().iterator();
    logger.info("image attrs");
    while (iter.hasNext()) {
      ImageListAttributeItem item = iter.next();
      logger.info("image : "+res.getImageId()+", "+item.getValue());
    }
*/
    // test image attribute methods for product codes
/*
    DescribeImageAttributeResult res = ec2.describeImageAttribute("ami-45997c2c", ImageAttributeType.productCodes);
    Iterator<ImageListAttributeItem> iter = res.getImageListAttribute().getImageListAttributeItems().iterator();
    logger.info("image attrs");
    while (iter.hasNext()) {
      ImageListAttributeItem item = iter.next();
      logger.info("image : "+res.getImageId()+", "+item.getValue());
    }
    ProductCodesAttribute attr = new ProductCodesAttribute();
    attr.getImageListAttributeItems().add(new ImageListAttributeItem(ImageListAttributeItemType.productCode, "BA7154BF"));
    res = ec2.describeImageAttribute("ami-45997c2c", ImageAttributeType.productCodes);
    iter = res.getImageListAttribute().getImageListAttributeItems().iterator();
    logger.info("image attrs");
    while (iter.hasNext()) {
      ImageListAttributeItem item = iter.next();
      logger.info("image : "+res.getImageId()+", "+item.getValue());
    }
    List<RegionInfo> rInfo = ec2.describeRegions(null);
    for (RegionInfo r : rInfo) {
      logger.info("region : "+r.getName()+" url : "+r.getUrl());
    }
*/
  }

  public static void log( String format ) {
    LOG.info( String.format( "============================= %-20.20s =============================", format) );
  }

  public static void item( String... format ) {
    StringBuffer sb = new StringBuffer().append("= ");
    for( String s : format ) sb.append( "%-20s " );
    LOG.info( String.format( sb.toString( ), format ) );
  }

  private static void testKeypairs( Jec2 ec2 ) throws EC2Exception {
    List<KeyPairInfo> info = ec2.describeKeyPairs(new String [] {});
    log("keypair list");
    for (KeyPairInfo i : info) {
      item("KEYPAIR", i.getKeyName(),i.getKeyFingerprint());
    }
    String keypairName = "test-keypair" + System.nanoTime( );
    log("create keypair: " + keypairName );
    ec2.createKeyPair(keypairName);
    info = ec2.describeKeyPairs(new String [] {});
    for (KeyPairInfo i : info) {
      item("KEYPAIR", i.getKeyName(),i.getKeyFingerprint());
    }
    log("create dupe keypair: " + keypairName );
    try {
      ec2.createKeyPair(keypairName);
    } catch ( Exception e1 ) {
      LOG.info( e1, e1 );
    }
    info = ec2.describeKeyPairs(new String [] {});
    for (KeyPairInfo i : info) {
      item("KEYPAIR", i.getKeyName(),i.getKeyFingerprint());
    }
    log("delete keypair: " + keypairName );
    ec2.deleteKeyPair(keypairName);
    info = ec2.describeKeyPairs(new String [] {});
    for (KeyPairInfo i : info) {
      item("KEYPAIR", i.getKeyName(),i.getKeyFingerprint());
    }
    log("delete non-existant keypair: " + keypairName );
    try {
      ec2.deleteKeyPair(keypairName);
    } catch ( Throwable e ) {
      LOG.info( e );
    }
    info = ec2.describeKeyPairs(new String [] {});
    for (KeyPairInfo i : info) {
      item("KEYPAIR", i.getKeyName(),i.getKeyFingerprint());
    }
  }

}
