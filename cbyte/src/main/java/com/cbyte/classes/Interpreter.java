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

    // Updated translator for x86-64 NASM assembly language - can read inputs
    public static String translateToAssembly(String code) {
        // Perform syntax checking and get the set of declared variables
        Set<String> declaredVariables = checkSyntax(code);
        if (declaredVariables == null) {
            return "; Error: Syntax check failed. See messages for details.\n";
        }

        StringBuilder assemblyCode = new StringBuilder();
        Map<String, String> variableTypes = new HashMap<>();
        Map<String, String> doubleValues = new HashMap<>(); // To store double constants
        int stringCounter = 0;
        int doubleCounter = 0;

        // Sections for data, bss, and text
        StringBuilder dataSection = new StringBuilder("section .data\n");
        StringBuilder bssSection = new StringBuilder("section .bss\n"); // Uninitialized data

        // Adding format specifiers with newline for better readability
        dataSection.append("formatInt: db \"%d\", 10, 0\n");      // "%d\n"
        dataSection.append("formatDouble: db \"%lf\", 10, 0\n");  // "%lf\n"
        dataSection.append("inputInt: db \"%d\", 0\n");           // "%d"
        dataSection.append("inputDouble: db \"%lf\", 0\n");       // "%lf"

        assemblyCode.append("section .text\n");
        assemblyCode.append("\textern printf\n");
        assemblyCode.append("\textern scanf\n"); // Include scanf for input
        assemblyCode.append("\tglobal main\n");
        assemblyCode.append("main:\n");

        // Prologue: Set up stack frame
        assemblyCode.append("\tpush rbp\n");
        assemblyCode.append("\tmov rbp, rsp\n");

        // Split code into individual lines
        String[] lines = code.split(";");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("int") || line.startsWith("double")) {
                // Handle variable declaration and initialization
                String[] parts = line.split("\\s+");
                String varType = parts[0].trim();
                String varName = parts[1].trim().replace(";", "");
                String value = "0";

                // Check for initialization
                if (varName.contains("=")) {
                    String[] varParts = varName.split("=");
                    varName = varParts[0].trim();
                    value = varParts[1].trim();
                }

                // Register the variable type
                variableTypes.put(varName, varType);

                if (varType.equals("int")) {
                    bssSection.append(varName).append(": resd 1\n"); // Reserve space for a 32-bit integer
                    if (!value.equals("0")) { // Handle initialization
                        assemblyCode.append("\tmov dword [rel ").append(varName).append("], ").append(value).append("\n");
                    }
                } else if (varType.equals("double")) {
                    bssSection.append(varName).append(": resq 1\n"); // Reserve space for a 64-bit double
                    if (!value.equals("0")) {
                        // Store the double value in the data section
                        String doubleLabel = "doubleValue" + doubleCounter++;
                        dataSection.append(doubleLabel).append(": dq ").append(value).append("\n");

                        // Load the double value and store it in the variable
                        assemblyCode.append("\tmovsd xmm0, [rel ").append(doubleLabel).append("]\n");
                        assemblyCode.append("\tmovsd [rel ").append(varName).append("], xmm0\n");
                    }
                }
            } else if (line.contains("=")) {
                // Handle variable assignment
                String[] parts = line.split("=");
                String varName = parts[0].trim();
                String expression = parts[1].trim();

                if (!declaredVariables.contains(varName)) {
                    return "; Error: Variable '" + varName + "' is not declared.\n";
                }

                String varType = variableTypes.get(varName);

                // Handle assignment with arithmetic operations
                if (varType.equals("int")) {
                    if (expression.matches("\\d+\\s*\\+\\s*\\d+")) {
                        // Both operands are integers
                        String[] exprParts = expression.split("\\+");
                        int operand1 = Integer.parseInt(exprParts[0].trim());
                        int operand2 = Integer.parseInt(exprParts[1].trim());
                        int result = operand1 + operand2;
                        assemblyCode.append("\tmov dword [rel ").append(varName).append("], ").append(result).append("\n");
                    } else if (expression.matches("\\w+\\s*\\+\\s*\\d+")) {
                        // Integer arithmetic: var + immediate
                        String[] exprParts = expression.split("\\+");
                        String baseVar = exprParts[0].trim();
                        String operand = exprParts[1].trim();
                        assemblyCode.append("\tmov eax, [rel ").append(baseVar).append("]\n");
                        assemblyCode.append("\tadd eax, ").append(operand).append("\n");
                        assemblyCode.append("\tmov [rel ").append(varName).append("], eax\n");
                    } else if (expression.matches("\\d+\\s*\\+\\s*\\w+")) {
                        // Integer arithmetic: immediate + var
                        String[] exprParts = expression.split("\\+");
                        String operand = exprParts[0].trim();
                        String baseVar = exprParts[1].trim();
                        assemblyCode.append("\tmov eax, ").append(operand).append("\n");
                        assemblyCode.append("\tadd eax, [rel ").append(baseVar).append("]\n");
                        assemblyCode.append("\tmov [rel ").append(varName).append("], eax\n");
                    } else if (expression.matches("\\w+\\s*\\+\\s*\\w+")) {
                        // Integer arithmetic: var + var
                        String[] exprParts = expression.split("\\+");
                        String var1 = exprParts[0].trim();
                        String var2 = exprParts[1].trim();
                        assemblyCode.append("\tmov eax, [rel ").append(var1).append("]\n");
                        assemblyCode.append("\tadd eax, [rel ").append(var2).append("]\n");
                        assemblyCode.append("\tmov [rel ").append(varName).append("], eax\n");
                    } else {
                        // Simple integer assignment
                        assemblyCode.append("\tmov dword [rel ").append(varName).append("], ").append(expression).append("\n");
                    }
                } else if (varType.equals("double")) {
                    if (expression.matches("\\d+(\\.\\d+)?\\s*\\+\\s*\\d+(\\.\\d+)?")) {
                        // Both operands are doubles (constants)
                        String[] exprParts = expression.split("\\+");
                        double operand1 = Double.parseDouble(exprParts[0].trim());
                        double operand2 = Double.parseDouble(exprParts[1].trim());
                        double result = operand1 + operand2;
                        String doubleLabel = "doubleValue" + doubleCounter++;
                        dataSection.append(doubleLabel).append(": dq ").append(result).append("\n");
                        assemblyCode.append("\tmovsd xmm0, [rel ").append(doubleLabel).append("]\n");
                        assemblyCode.append("\tmovsd [rel ").append(varName).append("], xmm0\n");
                    } else if (expression.matches("\\w+\\s*\\+\\s*\\w+") || expression.matches("\\w+\\s*\\+\\s*\\d+(\\.\\d+)?") || expression.matches("\\d+(\\.\\d+)?\\s*\\+\\s*\\w+")) {
                        // Double arithmetic (addition): var + var or var + immediate or immediate + var
                        String[] exprParts = expression.split("\\+");
                        String operand1 = exprParts[0].trim();
                        String operand2 = exprParts[1].trim();

                        // Load first operand
                        if (variableTypes.containsKey(operand1)) {
                            assemblyCode.append("\tmovsd xmm0, [rel ").append(operand1).append("]\n");
                        } else {
                            // Immediate double value
                            String doubleLabel1 = "doubleValue" + doubleCounter++;
                            dataSection.append(doubleLabel1).append(": dq ").append(operand1).append("\n");
                            assemblyCode.append("\tmovsd xmm0, [rel ").append(doubleLabel1).append("]\n");
                        }

                        // Load second operand
                        if (variableTypes.containsKey(operand2)) {
                            assemblyCode.append("\tmovsd xmm1, [rel ").append(operand2).append("]\n");
                        } else {
                            // Immediate double value
                            String doubleLabel2 = "doubleValue" + doubleCounter++;
                            dataSection.append(doubleLabel2).append(": dq ").append(operand2).append("\n");
                            assemblyCode.append("\tmovsd xmm1, [rel ").append(doubleLabel2).append("]\n");
                        }

                        assemblyCode.append("\taddsd xmm0, xmm1\n");
                        assemblyCode.append("\tmovsd [rel ").append(varName).append("], xmm0\n");
                    } else {
                        // Simple double assignment
                        String doubleLabel = "doubleValue" + doubleCounter++;
                        dataSection.append(doubleLabel).append(": dq ").append(expression).append("\n");
                        assemblyCode.append("\tmovsd xmm0, [rel ").append(doubleLabel).append("]\n");
                        assemblyCode.append("\tmovsd [rel ").append(varName).append("], xmm0\n");
                    }
                }
            } else if (line.startsWith("cin>>")) {
                // Handle input using scanf
                String varName = line.substring(5).trim();
                if (!declaredVariables.contains(varName)) {
                    return "; Error: Variable '" + varName + "' is not declared.\n";
                }
                String varType = variableTypes.get(varName);
                if (varType.equals("int")) {
                    // Setup for scanf
                    assemblyCode.append("\tlea rcx, [rel inputInt]\n");      // First argument: format string
                    assemblyCode.append("\tlea rdx, [rel ").append(varName).append("]\n"); // Second argument: address of variable
                    assemblyCode.append("\txor rax, rax\n");            // Clear RAX for variadic functions
                    // Prepare stack (shadow space)
                    assemblyCode.append("\tsub rsp, 40\n"); // 32 bytes shadow space + 8 bytes alignment
                    // Push arguments onto the stack (right to left)
                    assemblyCode.append("\tmov [rsp+32], rdx\n"); // Second argument
                    assemblyCode.append("\tmov [rsp+24], rcx\n"); // First argument
                    assemblyCode.append("\tcall scanf\n");
                    // Clean up the stack
                    assemblyCode.append("\tadd rsp, 40\n");
                } else if (varType.equals("double")) {
                    // Setup for scanf
                    assemblyCode.append("\tlea rcx, [rel inputDouble]\n");   // First argument: format string
                    assemblyCode.append("\tlea rdx, [rel ").append(varName).append("]\n"); // Second argument: address of variable
                    assemblyCode.append("\txor rax, rax\n");            // Clear RAX for variadic functions
                    // Prepare stack (shadow space)
                    assemblyCode.append("\tsub rsp, 40\n"); // 32 bytes shadow space + 8 bytes alignment
                    // Push arguments onto the stack (right to left)
                    assemblyCode.append("\tmov [rsp+32], rdx\n"); // Second argument
                    assemblyCode.append("\tmov [rsp+24], rcx\n"); // First argument
                    assemblyCode.append("\tcall scanf\n");
                    // Clean up the stack
                    assemblyCode.append("\tadd rsp, 40\n");
                }
            } else if (line.startsWith("cout<<")) {
                // Handle output
                String[] parts = line.substring(6).split("<<");
                for (String part : parts) {
                    part = part.trim();
                    if (part.isEmpty()) continue;

                    if (part.startsWith("\"") && part.endsWith("\"")) {
                        // Handle string output
                        String label = "str" + stringCounter++;
                        dataSection.append(label).append(": db ").append(part).append(", 0\n");
                        // Prepare stack (shadow space)
                        assemblyCode.append("\tsub rsp, 40\n"); // 32 bytes shadow space + 8 bytes alignment
                        // Push arguments onto the stack (right to left)
                        assemblyCode.append("\tlea rcx, [rel ").append(label).append("]\n");
                        assemblyCode.append("\tmov [rsp+32], rcx\n");
                        assemblyCode.append("\tcall printf\n");
                        // Clean up the stack
                        assemblyCode.append("\tadd rsp, 40\n");
                    } else if (variableTypes.containsKey(part)) {
                        String varType = variableTypes.get(part);
                        if (varType.equals("int")) {
                            // Prepare stack (shadow space)
                            assemblyCode.append("\tsub rsp, 40\n"); // 32 bytes shadow space + 8 bytes alignment
                            // Push arguments onto the stack (right to left)
                            assemblyCode.append("\tmov eax, [rel ").append(part).append("]\n");
                            assemblyCode.append("\tmov [rsp+32], rax\n"); // Argument value
                            assemblyCode.append("\tlea rcx, [rel formatInt]\n");
                            assemblyCode.append("\tmov [rsp+24], rcx\n"); // Format string
                            assemblyCode.append("\tcall printf\n");
                            // Clean up the stack
                            assemblyCode.append("\tadd rsp, 40\n");
                        } else if (varType.equals("double")) {
                            // Prepare stack (shadow space)
                            assemblyCode.append("\tsub rsp, 48\n"); // 32 bytes shadow space + 16 bytes for double alignment
                            // Push arguments onto the stack (right to left)
                            assemblyCode.append("\tmovsd xmm0, [rel ").append(part).append("]\n");
                            assemblyCode.append("\tmovsd [rsp+32], xmm0\n"); // Argument value
                            assemblyCode.append("\tlea rcx, [rel formatDouble]\n");
                            assemblyCode.append("\tmov [rsp+24], rcx\n"); // Format string
                            assemblyCode.append("\tcall printf\n");
                            // Clean up the stack
                            assemblyCode.append("\tadd rsp, 48\n");
                        }
                    } else {
                        return "; Error: Variable '" + part + "' used in output is not declared.\n";
                    }
                }
            }
        }

        // Epilogue: Restore stack frame and return
        assemblyCode.append("\tmov rsp, rbp\n");
        assemblyCode.append("\tpop rbp\n");
        assemblyCode.append("\tmov eax, 0\n"); // Return 0 from main
        assemblyCode.append("\tret\n");

        // Combine sections
        String finalAssemblyCode = dataSection.toString() + bssSection.toString() + assemblyCode.toString();

        // Write the assembly code to a file
        try (FileWriter writer = new FileWriter(PrimaryController.getFileName() + ".asm")) {
            writer.write(finalAssemblyCode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bufferMessage("\nAssembly code successfully created! Output -> " + PrimaryController.getFileName() + ".asm");
        return finalAssemblyCode;
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
        bufferMessage("No errors in file. Attempting to translate to x86 NASM assembly code...\n");
        writeMessagesToFile(); // Write messages to file
        PrimaryController.printToTerminal(); // Display messages
        return true; // Return true if no errors were found
    }

}
