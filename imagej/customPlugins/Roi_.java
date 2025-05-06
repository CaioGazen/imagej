import ij.*;
import ij.plugin.PlugIn;
import ij.plugin.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.frame.RoiManager;

import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.io.*;

public class Roi_ implements PlugIn {

  public void run(String arg) {

    GenericDialog gd = new GenericDialog("Choose output an input folders");
    gd.addDirectoryField("Input Folder", "imagensArvores/arvoresSrc");
    gd.addDirectoryField("Output Folder", "imagensArvores/arvoresOut");
    gd.showDialog();

    if (gd.wasCanceled()) {
      return;
    }

    String inputFolder = gd.getNextString();
    String outputFolder = gd.getNextString();

    System.out.println("Input Folder: " + inputFolder);

    for (File file : new File(inputFolder).listFiles()) {
      ImagePlus ip = new ImagePlus(file.getAbsolutePath());
      ImagePlus ipDup = ip.duplicate();
      IJ.run(ip, "8-bit", "");

      IJ.setAutoThreshold(ip, "Default");
      IJ.run(ip, "Convert to Mask", "black");
      IJ.run(ip, "Dilate", "");
      IJ.run(ip, "Dilate", "");
      IJ.run(ip, "Erode", "");
      IJ.run(ip, "Fill Holes", "");

      IJ.run(ip, "Analyze Particles...", "size=1000-Infinity  circularity=0.00-1.00 add");

      RoiManager rm = RoiManager.getInstance();

      ipDup.cropAndSave(rm.getRoisAsArray(), outputFolder, "png");

      rm.reset();
    }

    // IJ.run(ip, "8-bit", "ip");
    // IJ.setAutoThreshold(ip, "Default");
    // IJ.run(ip, "Enhance Contrast...", "saturated=0.35 normalize equalize");
    // IJ.run(ip, "Make Binary", "ip");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Fill Holes", "ip");
    // IJ.run(ip, "Analyze Particles...", "size=0-Infinity add");

    // IJ.run(ip, "8-bit", "");
    //
    // IJ.setAutoThreshold(ip, "Default");
    // IJ.run(ip, "Convert to Mask", "black");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Dilate", "ip");
    // IJ.run(ip, "Fill Holes", "ip");
    // IJ.run(ip, "Watershed", "ip");
    //
    // IJ.run(ip, "Analyze Particles...", "size=500-Infinity circularity=0.00-1.00
    // add ip");
    //
    // ip.show();
  }
}
