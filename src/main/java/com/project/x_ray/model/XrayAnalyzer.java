package com.project.x_ray.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.StdArrays;
import org.tensorflow.types.TFloat32;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Component
public class XrayAnalyzer {

    // Injecting the model path and tensor names from application.properties
    @Value("${xray.model.path}")
    private String modelPath;

    @Value("${xray.model.input-name}")
    private String inputTensorName;

    @Value("${xray.model.output-name}")
    private String outputTensorName;

    public String analyzeXray(MultipartFile file) {
        try {
            // Read and preprocess image
            BufferedImage img = ImageIO.read(file.getInputStream());

            int targetWidth = 224;
            int targetHeight = 224;

            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            resizedImage.getGraphics().drawImage(img, 0, 0, targetWidth, targetHeight, null);
            resizedImage.getGraphics().dispose();  // Ensure Graphics object is disposed

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

            // Load the TensorFlow model using path and tensor names from properties
            try (SavedModelBundle model = SavedModelBundle.load(modelPath, "serve")) {
                Session session = model.session();

                // Run inference
                Tensor output = session.runner()
                        .feed(inputTensorName, inputTensor)  // Feed the input tensor
                        .fetch(outputTensorName)            // Fetch the output tensor
                        .run()
                        .get(0); // Get the result from the first output tensor

                // Convert Tensor to float array
                float[][] outputArray = new float[1][1]; // Adjust the shape based on your model's output shape
                output.copyTo(outputArray); // Using copyTo to extract the values

                // Return the detection result
                return "Detection result: " + outputArray[0][0];  // Display the first element from the output
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
