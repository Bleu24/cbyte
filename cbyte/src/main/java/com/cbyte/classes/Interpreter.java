package com.cbyte.classes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.cbyte.PrimaryController; // Import the PrimaryController

public class Interpreter {
    private static String OUTPUT_FILE_PATH = "output.txt"; // File to store messages
    private static final String[] reservedWords = {"int", "double", "cin", "cout"};
    private static final String[] symbols = {"=", ">>", "<<", ";", "+"};
    private static final StringBuilder messageBuffer = new StringBuilder(); // Buffer to collect messages

    // Function to write all collected messages to a file
    public static void writeMessagesToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, false))) { // Overwrite file
            writer.write(messageBuffer.toString());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    // Modified printToTerminal to collect messages in the buffer
    public static void bufferMessage(String message) {
        messageBuffer.append(message).append("\n");
    }

    private static boolean isReservedWord(String word) {
        boolean result = Arrays.asList(reservedWords).contains(word);
        bufferMessage("Checking if '" + word + "' is a reserved word: " + result + "\n"); // Use bufferMessage
        return result;
    }

    private static boolean isSymbol(String word) {
        boolean result = Arrays.asList(symbols).contains(word);
        bufferMessage("Checking if '" + word + "' is a symbol: " + result + "\n");
        return result;
    }

    private static String removeSpaces(String sourceCode) {
        String cleanedCode = sourceCode.replaceAll("\\s+", " ").trim();
        cleanedCode = cleanedCode.replaceAll("\\s*>>\\s*", ">>");
        cleanedCode = cleanedCode.replaceAll("\\s*<<\\s*", "<<");
        cleanedCode = cleanedCode.replaceAll("\\s*=\\s*", "=");
        cleanedCode = cleanedCode.replaceAll("\\s*;\\s*", ";");
        cleanedCode = cleanedCode.replaceAll("\\s*\\+\\s*", "+");

        bufferMessage("\nSource code after removing spaces:\n" + cleanedCode + "\n");
        return cleanedCode;
    }

    // Enhanced syntax checker with added support for double values and arithmetic expressions
    private static Set<String> checkSyntax(String code) {
        bufferMessage("\nStarting syntax check for code:\n" + code + "\n");

        // Regular expressions for various C++ constructs
        String variableDeclarationRegex = "\\b(int|double)\\b\\s+\\w+\\s*";
        String variableInitializationRegex = "\\b(int|double)\\b\\s+\\w+\\s*=\\s*\\d+(\\.\\d+)?\\s*";
        // Updated assignment regex to handle arithmetic expressions and double values
        String assignmentRegex = "\\w+\\s*=\\s*\\w+(\\s*\\+\\s*\\d+(\\.\\d+)?)?\\s*|\\w+\\s*=\\s*\\d+(\\.\\d+)?\\s*";
        String inputRegex = "cin(\\s*>>\\s*\\w+)+\\s*";
        String outputRegex = "cout(\\s*<<\\s*(\".*?\"|\\w+|\\d+|\\s*\\w+\\s*))*\\s*";

        Set<String> declaredVariables = new HashSet<>();
        Map<String, String> variableTypes = new HashMap<>(); // Added to track variable types

        // Split the code into individual statements using semicolons as delimiters
        String[] statements = code.split(";");
        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) continue;

            bufferMessage("\nChecking statement: '" + statement + "'\n");

            // Check for variable declaration without initialization
            if (statement.matches(variableDeclarationRegex)) {
                String[] parts = statement.split("\\s+");
                String varType = parts[0].trim();
                String variableName = parts[1].trim();
                declaredVariables.add(variableName);
                variableTypes.put(variableName, varType); // Add the variable type
            }
            // Check for variable declaration with initialization
            else if (statement.matches(variableInitializationRegex)) {
                String[] parts = statement.split("\\s+");
                String varType = parts[0].trim();
                String variableName = parts[1].split("=")[0].trim();
                declaredVariables.add(variableName);
                variableTypes.put(variableName, varType); // Add the variable type
            }
            // Check for assignment or arithmetic expressions
            else if (statement.matches(assignmentRegex)) {
                String variableName = statement.split("=")[0].trim();
                if (!declaredVariables.contains(variableName)) {
                    bufferMessage("Syntax error: Variable '" + variableName + "' is not declared before assignment.\n");
                    return null;
                }
            }
            // Check for input operation
            else if (statement.matches(inputRegex)) {
                String[] variables = statement.split(">>");
                for (int i = 1; i < variables.length; i++) {
                    String variableName = variables[i].trim();
                    if (!declaredVariables.contains(variableName)) {
                        bufferMessage("Syntax error: Variable '" + variableName + "' used in input is not declared.\n");
                        return null;
                    }
                }
            }
            // Check for output operation
            else if (statement.matches(outputRegex)) {
                String[] parts = statement.split("<<");
                for (String part : parts) {
                    part = part.trim();
                    if (part.equals("cout") || (part.startsWith("\"") && part.endsWith("\"")) || part.matches("\\d+")) {
                        continue;
                    }
                    if (!part.isEmpty() && !declaredVariables.contains(part)) {
                        bufferMessage("Syntax error: Variable '" + part + "' used in output is not declared.\n");
                        return null;
                    }
                }
            }
            // If the statement does not match any valid construct, it's a syntax error
            else {
                bufferMessage("Syntax error found in statement: '" + statement + "'\n");
                return null;
            }
        }

        bufferMessage("Syntax check passed for all statements.\n");
        return declaredVariables;
    }

    // Function to translate C++ source code to assembly
    public static String translateToAssembly(String code) {
        try {
            // Construct valid C++ content by adding necessary headers and a main function
            String cppContent = "#include <iostream>\nusing namespace std;\nint main() {\n"
                    + code
                    + "\nreturn 0;\n}";

            // Use PrimaryController.getFileName() for the file name
            String baseFileName = PrimaryController.getFileName();
            File tempCppFile = new File(baseFileName + ".cpp");

            // Write the C++ content to a file
            Files.write(tempCppFile.toPath(), cppContent.getBytes());

            // Compile the C++ file into an executable using g++
            Process process = new ProcessBuilder(
                    "g++", tempCppFile.getName(), "-o", baseFileName + ".exe", "-m64"
            ).inheritIO().start();
            process.waitFor();

            bufferMessage("Compiling the C++ file into an executable using g++...\n");
            PrimaryController.printToTerminal();

            // Output success message
            System.out.println("\nExecutable successfully created! Output -> " + baseFileName + ".exe");
            bufferMessage("Executable successfully created! Output -> " + baseFileName + ".exe\n");
            PrimaryController.printToTerminal();
            return cppContent;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean interpretFile(File file) {
        messageBuffer.setLength(0); // Clear the buffer for a new file
        bufferMessage("\nReading file: " + file.getAbsolutePath() + "\n");

        StringBuilder sourceCode = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceCode.append(line).append("\n");
            }
        } catch (IOException e) {
            bufferMessage("Could not open the source file " + file.getAbsolutePath() + "\n");
            writeMessagesToFile(); // Write messages to file
            PrimaryController.printToTerminal(); // Display messages
            return false; // Return false if the file could not be opened
        }

        // Check for reserved words and symbols
        String[] words = sourceCode.toString().split("\\s+");
        for (String word : words) {
            if (isReservedWord(word)) {
                bufferMessage("Found reserved word: " + word + "\n");
            }
            if (isSymbol(word)) {
                bufferMessage("Found symbol: " + word + "\n");
            }
        }

        String noSpacesCode = removeSpaces(sourceCode.toString());
        PrimaryController.setSourceCode(noSpacesCode); // Sets the noSpacesCode as the source code
        if (noSpacesCode.isEmpty()) {
            bufferMessage("ERROR: No content after removing spaces in " + file.getAbsolutePath() + "\n");
            writeMessagesToFile(); // Write messages to file
            PrimaryController.printToTerminal(); // Display messages
            return false; // Return false if the content is empty after removing spaces
        }

        // Check syntax and stop if there is an error
        Set<String> variableList = checkSyntax(noSpacesCode);

        if(variableList ==null) {
            bufferMessage("ERROR IN " + file.getAbsolutePath() + "\n");
            writeMessagesToFile(); // Write messages to file
            PrimaryController.printToTerminal(); // Display messages
            return false;
        }

        // If all checks passed
        bufferMessage("No errors in file. Attempting to translate to AT&T assembly code...\n");
        writeMessagesToFile(); // Write messages to file
        PrimaryController.printToTerminal(); // Display messages
        return true; // Return true if no errors were found
    }

}
