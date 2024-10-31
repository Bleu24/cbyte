package com.cbyte.classes;

import java.io.*;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.cbyte.PrimaryController; // Import the PrimaryController

public class Interpreter {
    private static String OUTPUT_FILE_PATH = "output.txt"; // File to store messages
    private static final String[] reservedWords = {"int", "double", "cin", "cout"};
    private static final String[] symbols = {"=", ">>", "<<", ";", "+"};
    private static final StringBuilder messageBuffer = new StringBuilder(); // Buffer to collect messages

    // Function to write all collected messages to a file
    private static void writeMessagesToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH, false))) { // Overwrite file
            writer.write(messageBuffer.toString());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    // Modified printToTerminal to collect messages in the buffer
    private static void bufferMessage(String message) {
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

    // Improved syntax checker using simplified regular expressions
    private static boolean checkSyntax(String code) {
        bufferMessage("\nStarting syntax check for code:\n" + code + "\n");

        // Regular expressions for different constructs
        String variableDeclarationRegex = "\\b(int|double)\\b\\s+\\w+\\s*";
        String assignmentRegex = "\\w+\\s*=\\s*\\d+\\s*";
        String inputRegex = "cin\\s*>>\\s*\\w+\\s*";
        String outputRegex = "cout\\s*<<\\s*(\\w+|\".*\"|\\w+\\s*([+\\-*/]\\s*\\w+)+)\\s*";

        // Split the code into individual statements
        String[] statements = code.split(";");
        for (String statement : statements) {
            statement = statement.trim();
            if (statement.isEmpty()) continue;

            bufferMessage("\nChecking statement: '" + statement + "'\n");

            // Check each statement against all valid patterns
            if (!statement.matches(variableDeclarationRegex) &&
                    !statement.matches(assignmentRegex) &&
                    !statement.matches(inputRegex) &&
                    !statement.matches(outputRegex)) {
                bufferMessage("Syntax error found in statement: '" + statement + "'\n");
                return false;
            }
        }

        bufferMessage("Syntax check passed for all statements.\n");
        return true;
    }

    // Translates C++ code to x86 NASM assembly language for Windows and saves it in a file
    private static String translateToAssembly(String code) {
        StringBuilder assemblyCode = new StringBuilder();
        Map<String, String> variableRegisters = new HashMap<>();
        int registerIndex = 0;
        String[] registers = {"rcx", "rdx", "r8", "r9"}; // Use x64 registers for Windows calling convention
        StringBuilder dataSection = new StringBuilder("section .data\n");
        StringBuilder bssSection = new StringBuilder("section .bss\n");
        int stringCounter = 0;

        assemblyCode.append("section .text\n");
        assemblyCode.append("\tglobal main\n");
        assemblyCode.append("\textern printf\n");
        assemblyCode.append("main:\n");

        String[] lines = code.split(";");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("int") || line.startsWith("double")) {
                // Handle variable declaration
                String varName = line.split(" ")[1].replace(";", "").trim();
                if (!variableRegisters.containsKey(varName)) {
                    if (registerIndex >= registers.length) {
                        return "; Error: Too many variables declared. Not enough registers available.\n";
                    }
                    variableRegisters.put(varName, registers[registerIndex++]);
                    assemblyCode.append("\t; Declare ").append(varName).append("\n");
                }
            } else if (line.contains("=")) {
                // Handle variable initialization
                String[] parts = line.split("=");
                String varName = parts[0].trim();
                String value = parts[1].replace(";", "").trim();
                String register = variableRegisters.get(varName);

                if (register == null) {
                    return "; Error: Variable '" + varName + "' is not declared.\n";
                }

                assemblyCode.append("\tmov ").append(register).append(", ").append(value).append("\n");
            } else if (line.startsWith("cin>>")) {
                // Handle input (Windows-specific, simplified)
                String varName = line.substring(5).replace(";", "").trim();
                String register = variableRegisters.get(varName);

                if (register == null) {
                    return "; Error: Variable '" + varName + "' is not declared.\n";
                }

                assemblyCode.append("\t; Input ").append(varName).append("\n");
                assemblyCode.append("\t; Input handling in Windows requires complex API calls, simplified for now\n");
            } else if (line.startsWith("cout<<")) {
                // Handle output
                String varName = line.substring(6).replace(";", "").trim();
                if (varName.startsWith("\"") && varName.endsWith("\"")) {
                    // Handle string output
                    String label = "str" + stringCounter++;
                    dataSection.append(label).append(": db ").append(varName).append(", 0\n");
                    assemblyCode.append("\t; Output ").append(varName).append("\n");
                    assemblyCode.append("\tmov rcx, ").append(label).append(" ; First argument to printf\n");
                    assemblyCode.append("\tcall printf\n");
                } else {
                    // Handle variable output
                    String register = variableRegisters.get(varName);
                    if (register == null) {
                        return "; Error: Variable '" + varName + "' is not declared.\n";
                    }

                    assemblyCode.append("\t; Output ").append(varName).append("\n");
                    assemblyCode.append("\tmov rcx, ").append(register).append(" ; First argument to printf\n");
                    assemblyCode.append("\tcall printf\n");
                }
            }
        }

        assemblyCode.append("\tret\n");

        // Combine sections
        String finalAssemblyCode = dataSection.toString() + bssSection.toString() + assemblyCode.toString();

        // Write to file "assembly.asm"
        try (FileWriter writer = new FileWriter(PrimaryController.getFileName() + ".asm")) {
            writer.write(finalAssemblyCode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bufferMessage("\nAssembly code successfully created! Output -> " + PrimaryController.getFileName() + ".asm");
        return finalAssemblyCode;
    }

    public static String interpretFile(File file) {
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
            return "Could not open the source file " + file.getAbsolutePath() + "\n";
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
        if (noSpacesCode.isEmpty()) {
            bufferMessage("ERROR: No content after removing spaces in " + file.getAbsolutePath() + "\n");
            writeMessagesToFile(); // Write messages to file
            PrimaryController.printToTerminal(); // Display messages
            return "ERROR: No content after removing spaces in " + file.getAbsolutePath() + "\n";
        }

        if (!checkSyntax(noSpacesCode)) {
            bufferMessage("ERROR IN " + file.getAbsolutePath() + "\n");
            writeMessagesToFile(); // Write messages to file
            PrimaryController.printToTerminal(); // Display messages
            return "ERROR IN " + file.getAbsolutePath() + "\n";
        }

        String assemblyCode = translateToAssembly(noSpacesCode);
        writeMessagesToFile(); // Write messages to file
        PrimaryController.printToTerminal(); // Display messages
        return assemblyCode;
    }

}
