package com.project.x_ray.model;

import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.ndarray.buffer.FloatDataBuffer;
import org.tensorflow.types.TFloat32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class XrayAnalyzer {

    public String analyzeXray(MultipartFile file) {
        try {
            // Read and preprocess image
            BufferedImage img = ImageIO.read(file.getInputStream());

            int targetWidth = 224;
            int targetHeight = 224;

            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            resizedImage.getGraphics().drawImage(img, 0, 0, targetWidth, targetHeight, null);

            // Prepare float array
            float[][][][] inputArray = new float[1][targetHeight][targetWidth][3]; // (batch_size, height, width, channels)

            for (int y = 0; y < targetHeight; y++) {
                for (int x = 0; x < targetWidth; x++) {
                    int rgb = resizedImage.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    inputArray[0][y][x][0] = r / 255.0f;
                    inputArray[0][y][x][1] = g / 255.0f;
                    inputArray[0][y][x][2] = b / 255.0f;
                }
            }

            // Create tensor from array
            TFloat32 inputTensor = TFloat32.tensorOf(StdArrays.ndCopyOf(inputArray));

            // Load model
            try (SavedModelBundle model = SavedModelBundle.load("path_to_your_saved_model", "serve")) {
                Session session = model.session();

                Tensor output = session.runner()
                        .feed("your_input_tensor_name", inputTensor)
                        .fetch("your_output_tensor_name")
                        .run()
                        .getFirst();

                // Read output
                float[][] outputArray = new float[1][1]; // Adjust shape based on your model
                ((TFloat32) output).read((FloatDataBuffer) StdArrays.ndCopyOf(outputArray));

                return "Detection result: " + outputArray[0][0];
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading image.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during model inference.";
        }
    }
}
