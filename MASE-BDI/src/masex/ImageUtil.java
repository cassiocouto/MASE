package masex;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public abstract class ImageUtil {

	public static void setHeightAndWidth(String nomeArquivo) {
		// ImagePlus image = IJ.openImage(nomeArquivo);
		// ImageProcessor improc = image.getProcessor();
		BufferedImage improc;
		try {
			improc = ImageIO.read(new File(nomeArquivo));
			int width = improc.getWidth();
			int height = improc.getHeight();
			BeliefDB.setHeight(height);
			BeliefDB.setWidth(width);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static int[][] filtragem_classes_iniciais(String nomeArquivo) {

		try {
			BufferedImage buffer = ImageIO.read(new File(nomeArquivo));
			int width = buffer.getWidth();
			int height = buffer.getHeight();
			int[][] result = new int[width][height];
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					Color c = new Color(buffer.getRGB(i, j));
					int red = c.getRed();
					int green = c.getGreen();
					int blue = c.getBlue();
					if (red == 0 && green == 100 && blue == 0) {
						result[i][j] = 1500;
					} else if (red == 255 && green == 255 && blue == 50) {
						result[i][j] = 500;
					} else {
						result[i][j] = -1;
					}
				}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int[][] filtragem_adicional_layers(String[] nomesArquivos) {
		int width = BeliefDB.getWidth();
		int height = BeliefDB.getHeight();

		int[][] result = new int[width][height];

		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				result[i][j] = 0;
			}
		}
		try {
			for (String nomeArquivo : nomesArquivos) {
				// ImagePlus image = IJ.openImage(nomeArquivo);
				BufferedImage image = ImageIO.read(new File(nomeArquivo));
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						Color c = new Color(image.getRGB(i, j));
						int pixel[] = new int[] { c.getRed(), c.getBlue(), c.getGreen() };

						if (pixel[0] > 10 && pixel[1] > 10 && pixel[2] > 10) {
							result[i][j] += 0;
						} else
							result[i][j] += 1;
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static int[][] filtragem_relevo(String nomeArquivo) {
		// ImagePlus image = IJ.openImage(nomeArquivo);
		// ImageProcessor improc = image.getProcessor();
		try {
			BufferedImage improc = ImageIO.read(new File(nomeArquivo));
			int width = improc.getWidth();
			int height = improc.getHeight();
			BeliefDB.setHeight(height);
			BeliefDB.setWidth(width);
			int[][] result = new int[width][height];
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					Color c = new Color(improc.getRGB(i, j));
					int pixel[] = new int[] { c.getRed(), c.getBlue(), c.getGreen() };

					if (pixel[0] == 0) {
						result[i][j] = 0;
					} else if (pixel[0] == 52) {
						result[i][j] = -1;
					} else if (pixel[0] == 78) {
						result[i][j] = 0;
					} else if (pixel[0] == 104) {
						result[i][j] = +1;
					} else if (pixel[0] == 130) {
						result[i][j] = 0;
					} else {
						result[i][j] = 0;
					}

				}
				return result;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int[][] filtragem_solo(String nomeArquivo) {
		// ImagePlus image = IJ.openImage(nomeArquivo);
		// ImageProcessor improc = image.getProcessor();
		try {
			BufferedImage improc = ImageIO.read(new File(nomeArquivo));

			int width = improc.getWidth();
			int height = improc.getHeight();
			BeliefDB.setHeight(height);
			BeliefDB.setWidth(width);
			int[][] result = new int[width][height];
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					Color c = new Color(improc.getRGB(i, j));
					int pixel[] = new int[] { c.getRed(), c.getBlue(), c.getGreen() };

					if (pixel[0] == 210 || pixel[0] == 38) {
						result[i][j] = -1;
					} else if (pixel[0] == 112) {
						result[i][j] = 0;
					} else if (pixel[0] == 74 || pixel[0] == 53 || pixel[0] == 250 || pixel[0] == 154) {
						result[i][j] = 1;
					} else if (pixel[0] == 176 || pixel[0] == 133 || pixel[0] == 91 || pixel[0] == 232) {
						result[i][j] = 1;
					} else if (pixel[0] == 189) {
						result[i][j] = 1;
					} else if (pixel[0] == 45) {
						result[i][j] = 0;
					} else if (pixel[0] == 104 || pixel[0] == 84 || pixel[0] == 141) {
						result[i][j] = 0;
					} else if (pixel[0] == 201 || pixel[0] == 98) {
						result[i][j] = 0;
					} else if (pixel[0] == 79) {
						result[i][j] = 0;
					} else if (pixel[0] == 216) {
						result[i][j] = 1;
					} else {
						result[i][j] = 0;
					}

				}

			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * //refazer este método! public static int[][] filtragem_gaussiana(String
	 * nomeArquivo) { // ImagePlus image = IJ.openImage(nomeArquivo); //
	 * ImageProcessor improc = image.getProcessor(); BufferedImage improc =
	 * ImageIO.read(new File(nomeArquivo));
	 * 
	 * int width = improc.getWidth(); int height = improc.getHeight(); int[][]
	 * result = new int[width][height]; image =
	 * converter_para_gray_im(nomeArquivo); improc = image.getProcessor();
	 * improc.threshold(50); float sigma = 20 * (width / 2750f); new
	 * GaussianBlur().blurGaussian(improc, sigma, sigma, 0.02);
	 * inverter_imagem(image); ImagePlus imageOrig = IJ.openImage(nomeArquivo);
	 * 
	 * for (int j = 0; j < height; j++) { for (int i = 0; i < width; i++) {
	 * 
	 * int valor[] = image.getPixel(i, j); int valorOriginal[] =
	 * imageOrig.getPixel(i, j); if (valorOriginal[0] < 240) { valor[0] = -1; }
	 * 
	 * result[i][j] = (short) valor[0]; } } // image.show(); image.changes =
	 * false; image.close(); return result; }
	 */
	public static int[][] filtragem_gaussiana(String nomeArquivo) {
		return null;
	}

	public static int[][] filtragem_layers_basica(String[] nomesArquivos, int width, int height) {
		int[][] result = new int[width][height];
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				result[i][j] = 0;
			}
		}
		try {
			for (String nomeArquivo : nomesArquivos) {
				BufferedImage buffer = ImageIO.read(new File(nomeArquivo));

				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						Color c = new Color(buffer.getRGB(i, j));
						int red = c.getRed();
						int valorFinal = red;

						if (red < 240) {
							valorFinal = 1;
						}

						result[i][j] = (short) valorFinal;
					}
				}

			}
			return result;
		} catch (IOException i) {
			i.printStackTrace();
		}
		return null;
	}

	/*
	 * public static ImagePlus converter_para_gray_im(String nomeArquivo) {
	 * ImagePlus image = IJ.openImage(nomeArquivo); ImageProcessor improc =
	 * image.getProcessor(); int width = improc.getWidth(); int height =
	 * improc.getHeight();
	 * 
	 * ImagePlus a = IJ.createImage("a", "8-bit", width, height, 1);
	 * ImageProcessor i1 = a.getProcessor();
	 * i1.setPixels(converter_para_gray(nomeArquivo));
	 * 
	 * return a; }
	 * 
	 * public static byte[] converter_para_gray(String nomeArquivo) { ImagePlus
	 * image = IJ.openImage(nomeArquivo); ImageProcessor improc =
	 * image.getProcessor(); int width = improc.getWidth(); int height =
	 * improc.getHeight(); byte[] resposta = new byte[width * height]; for (int
	 * j = 0; j < height; j++) { for (int i = 0; i < width; i++) {
	 * 
	 * int rgb[] = image.getPixel(i, j); byte gray = (byte) (0.299 * rgb[0] +
	 * 0.587 * rgb[1] + 0.114 * rgb[2]); resposta[(j * width) + i] = gray;
	 * 
	 * } } // ImagePlus a = IJ.createImage("a", "8-bit", width, height, 1); //
	 * ImageProcessor i1 = a.getProcessor(); // i1.setPixels(resposta); //
	 * a.show();
	 * 
	 * return resposta; }
	 * 
	 * public static void inverter_imagem(ImagePlus image) { ImageProcessor
	 * improc = image.getProcessor(); int width = improc.getWidth(); int height
	 * = improc.getHeight(); byte[] resposta = new byte[width * height]; for
	 * (int j = 0; j < height; j++) { for (int i = 0; i < width; i++) {
	 * 
	 * int rgb[] = image.getPixel(i, j); resposta[(j * width) + i] = (byte) (255
	 * - rgb[0]);
	 * 
	 * } } improc.setPixels(resposta); }
	 */
	public static int[][] filtragem_pdot(String nomeArquivo) {
		// ImagePlus image = IJ.openImage(nomeArquivo);
		// ImageProcessor improc = image.getProcessor();
		try {
			BufferedImage improc = ImageIO.read(new File(nomeArquivo));
			int width = improc.getWidth();
			int height = improc.getHeight();
			int[][] result = new int[width][height];
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					Color c = new Color(improc.getRGB(i, j));
					int rgb[] = new int[] { c.getRed(), c.getBlue(), c.getGreen() };
					short valor = 0;
					if (rgb[0] > rgb[1] && rgb[0] > rgb[2]) {
						valor = 1; // proibido
					} else if (rgb[2] > rgb[1] && rgb[2] > rgb[0]) {
						valor = 2; // incentivado
					} else if (rgb[1] > rgb[2] && rgb[1] > rgb[0]) {
						valor = 3; // normal
					}
					result[i][j] = valor;

				}
			}
			// improc.setPixels(resposta2);
			// image.show();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String gerarImagem(int width, int height, int[][] map, int[][] usedPositions, int agentQty,
			int percentageOfGroupAgents, boolean debug, long simulationTime) {
		Starter.print("gerando imagem");

		// ImagePlus image = IJ.createImage("resultado.bmp", "RGB", width,
		// height, 8);
		// ImageProcessor improc = image.getProcessor();
		try {
			BufferedImage improc = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					if (debug && usedPositions[i][j] != 0) {
						improc.setRGB(i, j, 0x0000ff);

					} else if (map[i][j] == 1500) {
						improc.setRGB(i, j, 0x006400);
					} else if (map[i][j] < 1500 && map[i][j] >= 0) {
						improc.setRGB(i, j, 0xffff32);
					} else {
						improc.setRGB(i, j, 0xffffff);
					}

				}
			}

			String path = "";
			String imname = "resultadoMASEX " + "" + agentQty + "porcentagem" + percentageOfGroupAgents
			// + "tempo"+ simulationTime
					+ ".bmp";
			path = BeliefDB.savePath + imname;
			RenderedImage a = improc;
			ImageIO.write(a, "bmp", new File(path));
			Starter.print("imagem gerada");
			return path;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String gerarImagem(int width, int height, int[][] map, int[][] usedPositions, int agentQty,
			int percentageOfGroupAgents, boolean debug, String nomeImagem) {
		Starter.print("gerando imagem");
		try {
			BufferedImage improc = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					if (debug && usedPositions[i][j] != 0) {
						improc.setRGB(i, j, 0x0000ff);
					} else if (map[i][j] == 1500) {
						improc.setRGB(i, j, 0x006400);
					} else if (map[i][j] < 1500 && map[i][j] >= 0) {
						improc.setRGB(i, j, 0xffff32);
					} else {
						improc.setRGB(i, j, 0xffffff);
					}

				}
			}

			String path = "";
			
			path = BeliefDB.savePath + nomeImagem+".bmp";
			RenderedImage a = improc;
			ImageIO.write(a, "bmp", new File(path));
			Starter.print("imagem gerada");
			return path;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
