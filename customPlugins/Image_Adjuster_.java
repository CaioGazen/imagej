import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.AWTEvent;
import ij.plugin.PlugIn;

public class Image_Adjuster_ implements PlugIn, DialogListener {

    private ImagePlus imp;
    private ImageProcessor originalIp;
    private double brilho, contraste, solarizacao, dessaturacao;
    private int width, height;

    public void run(String arg) {
        imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }

        originalIp = imp.getProcessor().duplicate(); // Cópia da imagem original
        brilho = 0;
        contraste = 1;
        solarizacao = 0;
        dessaturacao = 0;
        width = originalIp.getWidth();
        height = originalIp.getHeight();

        GenericDialog gd = new GenericDialog("Ajustes de Imagem");
        gd.addSlider("Brilho", -255, 255, 0, 1);
        gd.addSlider("Contraste", -255, 255, 0, 0.2);
        gd.addSlider("Solarização", 0, 255, 255, 1);
        gd.addSlider("Dessaturação", 0, 1, 1, 0.05);

        gd.addDialogListener(this);
        gd.showDialog();
        

        if (gd.wasCanceled()) {
            imp.setProcessor(originalIp); // Restaura a imagem original
            imp.updateAndDraw();
            return;
        }

        brilho = gd.getNextNumber();
        contraste = gd.getNextNumber();
        solarizacao = gd.getNextNumber();
        dessaturacao = gd.getNextNumber();

        // Aplica as alterações definitivas (se o usuário clicar em "OK")
        ajustarBrilho(originalIp, brilho);
        ajustarContraste(originalIp, contraste);
        solarizar(originalIp, solarizacao);
        dessaturar(originalIp, dessaturacao);
        imp.setProcessor(originalIp);
        imp.updateAndDraw();
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        brilho = gd.getNextNumber();
        contraste = gd.getNextNumber();
        solarizacao = gd.getNextNumber();
        dessaturacao = gd.getNextNumber();

        ImageProcessor ip = originalIp.duplicate(); // Duplica para pré-visualização
        ajustarBrilho(ip, brilho);
        ajustarContraste(ip, contraste);
        solarizar(ip, solarizacao);
        dessaturar(ip, dessaturacao);

        imp.setProcessor(ip);
        imp.updateAndDraw();
        return true;
    }

    public int limit(int n){
      if(n < 0) return 0;
      if(n > 255) return 255;
      return n;
    }

    public void ajustarBrilho(ImageProcessor processor, double brilho) {

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                rgb[0] = limit(rgb[0] += brilho);
                rgb[1] = limit(rgb[1] += brilho);
                rgb[2] = limit(rgb[2] += brilho);
                processor.putPixel(x, y, rgb);
            }
        }
    }

    public void ajustarContraste(ImageProcessor processor, double contraste) {
        double fator = (259 * (contraste + 255))/(255 * (259 - contraste));
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                rgb[0] = limit((int)Math.round((fator * (rgb[0] - 128)) + 128));
                rgb[1] = limit((int)Math.round((fator * (rgb[1] - 128)) + 128));
                rgb[2] = limit((int)Math.round((fator * (rgb[2] - 128)) + 128));
                processor.putPixel(x, y, rgb);
            }
        }
    }

    public void solarizar(ImageProcessor processor, double nivel) {
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                rgb[0] = (rgb[0] > nivel) ? 255 - rgb[0] : rgb[0];
                rgb[1] = (rgb[1] > nivel) ? 255 - rgb[1] : rgb[1];
                rgb[2] = (rgb[2] > nivel) ? 255 - rgb[2] : rgb[2];
                processor.putPixel(x, y, rgb);
            }
        }
    }

    public int luminance(int[] rgb){
        double Yd = rgb[0] * 0.2125 + rgb[1] * 0.7154 + rgb[2] * 0.0721;
        int Y = (int)Math.round(Yd);
        return Y;
    }

    public void dessaturar(ImageProcessor processor, double fator) {
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                int Y = luminance(rgb);
                rgb[0] = limit((int)Math.round(Y + (fator * (rgb[0] - Y))));
                rgb[1] = limit((int)Math.round(Y + (fator * (rgb[1] - Y))));
                rgb[2] = limit((int)Math.round(Y + (fator * (rgb[2] - Y))));
                processor.putPixel(x, y, rgb);
            }
        }
    }
}
