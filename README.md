# JavaSST Compiler

Generates a Java-Class file for a given JavaSST Program.

## Usage
```
JavaSSTCompiler inputFile [generateDotGraph]
```
The `inputFile` must be a valid path to a File containing JavaSST source code.
If the second parameter `generateDotGraph` is set to `true`, an additional DOT file is created which contains a Graph describing the AST and symbol table. 

## Use the class file
```
java -noverify JavaClassFileUsingCompiled Class
```
The `-noverify` flag is used to circumvent the StackMap usage in higher Java versions.
