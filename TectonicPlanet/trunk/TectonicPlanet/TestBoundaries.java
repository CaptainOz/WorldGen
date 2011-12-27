package TectonicPlanet;

// Standard Java imports
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.awt.*;
import java.math.*;
import javax.vecmath.*;
import java.util.*;



public class TestBoundaries {
  public static void main(String[] args) {
	  TestBoundaries tb=new TestBoundaries();
		tb.init();
	}
	
	public void init() {
	  double[] lat={-10,-10,10,10};
	  double[] lon={10,-10,-10,10};
		
		ByteBuffer byteBuf=ByteBuffer.allocate(1000);
		byteBuf.order(ByteOrder.LITTLE_ENDIAN);
		
		
		try {
		  FileChannel pkgChan=new FileOutputStream(new File("Terrain5/pathlist.pkg")).getChannel();
		  FileChannel idxChan=new FileOutputStream(new File("Terrain5/pathlist.idx")).getChannel();
			
			byteBuf.putInt(1);	// Number of loops
			byteBuf.put((byte)4);	// Bytes in the following string
			byteBuf.put(new String("Test").getBytes());	// Loop name
			byteBuf.putDouble(-179);	// West
			byteBuf.putDouble(-89);	// South
			byteBuf.putDouble(179);	// East
			byteBuf.putDouble(89);	// North
			byteBuf.putLong(0);	// Offset of group in pkg file
			System.out.println("Pos="+byteBuf.position()+" limit="+byteBuf.limit());
			byteBuf.flip();
			System.out.println("Pos="+byteBuf.position()+" limit="+byteBuf.limit());
			idxChan.write(byteBuf);
			
			idxChan.close();
			
			byteBuf.clear();
			System.out.println("Pos="+byteBuf.position()+" limit="+byteBuf.limit());
			byteBuf.putInt(5);	// Number of entries in this loop
			byteBuf.put((byte)3);	// Number of numbers in entry (lat,lon,alt)
			
			for (int i=0; i<=4; i++) {
				byteBuf.putDouble(lat[i%4]);	// Lat
				byteBuf.putDouble(lon[i%4]);	// Lon
				byteBuf.putShort((short)12);	// Alt
			}
			
			System.out.println("Pos="+byteBuf.position()+" limit="+byteBuf.limit());
			byteBuf.flip();
			System.out.println("Pos="+byteBuf.position()+" limit="+byteBuf.limit());
			pkgChan.write(byteBuf);
			pkgChan.close();
		} catch(Exception e) {
		  System.out.println(e);
			e.printStackTrace();
		}
	}
}