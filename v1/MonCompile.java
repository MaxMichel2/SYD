package rpc;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class MonCompile {
    // Listes des variables associes à chaque type de I/O Stream
    private static List<String> dataIOStreamType = new ArrayList<String>(
        Arrays.asList("byte", "char", "short", "int", "long", "float", "double", "boolean", "void", "String")
    );
    private static List<String> fileIOStreamTypes = new ArrayList<String>(
        Arrays.asList("File")
    );
    private static List<String> byteArrayIOStreamTypes = new ArrayList<String>(
        Arrays.asList("byte[]")
    );
    private static String socketPort;
    private static String socketIP;
    public static void main(String[] args){
        try{
            // Récupère le nom du fichier qui nous intéresse et ajoute "rpc." s'il n'y est pas déjà
            // On pars du principe que pour créer le compilateur, on sait dans quel package est la classe
            String classPackName = "";
            if(args.length < 3){
                System.out.println("To run:\n" +
                "\tjava rpc.MonCompile file.java xxx.xxx.xxx.xxx yyyy\n" +
                "\tFirst parameter -> Java file to distribute\n" +
                "\tSecond parameter -> IP Adress on which to send data\n" +
                "\tThird parameter -> Port on which to send data");
                System.exit(1);
            } else {
                if(args[0].startsWith("rpc.")){
                    classPackName = args[0];
                }else{
                    classPackName = "rpc."+args[0];
                }
                socketIP = args[1].toString();
                socketPort = args[2].toString();
            }
            Class inputClass = Class.forName(classPackName); // Contient l'objet Class du paramètre donné en argument
            String packName = inputClass.getPackageName(); // Nom du package de la classe donné par args[0]
            String className = classPackName.replace(packName+".", ""); // Nom de la classe
            List<String> fieldsList = new ArrayList<String>(); // Liste pour les attributs de la classe
            List<String> fieldsNameList = new ArrayList<String>(); // Liste pour le nom des attributs de la classe
            List<String> constructorList = new ArrayList<String>(); // Liste pour les constructeurs de la classe
            List<String> methodList = new ArrayList<String>(); // Liste pour les méthodes de la classe
            List<String> methodParameterList = new ArrayList<String>(); // Liste pour les paramètres des méthodes
            
            // Fichier source du code donné en argument
            Path filePath = Paths.get("").toAbsolutePath();
            File source = new File(filePath.toString()+"\\"+className+".java");

            // Lieu de sortie des dichiers crée par le compilateur
            Path parent = Paths.get("").toAbsolutePath().getParent();
            File target =  new File(parent.toString()+"\\v3\\"+className+".java");
            File targetClient = new File(parent.toString()+"\\v3\\"+className+"Client.java");

            // Remplissage du code client initial
            List<String> targetClientLines = new ArrayList<String>();
            targetClientLines.add("package "+packName+";\n");
            targetClientLines.add("public class "+className+"Client {\n");
            targetClientLines.add("\tpublic static void main(String[] args) throws Exception{");
            targetClientLines.add("\t\tjava.net.Socket s = new java.net.Socket("+socketIP+", "+socketPort+");\n");

            // Liste pour la copie du code de args[0]
            List<String> targetLines = new ArrayList<String>();
            String targetLine = null;
            
            try {
                FileReader fr = new FileReader(source);
                BufferedReader br = new BufferedReader(fr);

                while ((targetLine = br.readLine()) != null){
                    if(!targetLine.startsWith("//")){ // && !targetLine.isEmpty()){ // Pour trier les commentaires
                        targetLines.add(targetLine);
                    }
                }

                fr.close();
                br.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Suppression de la dernière ligne du code qui est simplement la dernière accolade
            // Cela permet de pouvoir rajouter la fonction main nécessaire à faire tourner le code
            targetLines.remove(targetLines.size() - 1);

            /*
            Field[] fs = inputClass.getDeclaredFields();
            for(Field f : fs){
                String fld = f.toString().replaceAll(classPackName+".", "");
                fieldsList.add(fld);
                String[] fldString = fld.split(" ");
                fieldsNameList.add(fldString[fldString.length - 1]);
            }

            Constructor[] cs = inputClass.getDeclaredConstructors();
            for(Constructor c : cs){
                String constructorString = c.toGenericString();
                constructorString = constructorString.replaceAll(packName+".", ""); // Remplace "rpc." par ""
                Type[] ts = c.getGenericParameterTypes();
                List<Type> ts2 = Arrays.asList(ts);
                for(Type t : ts2){
                    // Unicité du nom de la variable dans le constructeur
                    constructorString = constructorString.replaceAll(t.getTypeName(), t.getTypeName()+" "+t.getTypeName().charAt(0)+ts2.indexOf(t));
                }
                constructorList.add(constructorString);
            }
            */

            // targetLines contient le fichier Matlab.java sans la dernière accolade
            // Il faut maintenant écrire la fonction main qui sera implémentée
            char targetVariableName = className.toLowerCase().charAt(0);
            targetLines.add("\tpublic static void main(String[] args) throws Exception {\n");
            targetLines.add("\t\t"+className+" "+targetVariableName+" = null;\n");
            targetLines.add("\t\tjava.net.ServerSocket sos = new java.net.ServerSocket("+socketPort+");\n");
            targetLines.add("\t\tjava.net.Socket s = sos.accept();\n");

            Method[] ms = inputClass.getDeclaredMethods();
            for(Method m : ms){
                String returnType = m.getReturnType().getSimpleName();

                IOStream(targetClientLines, targetLines, returnType, true);

                String methodString = m.toGenericString();
                methodString = methodString.replaceAll(classPackName+".", ""); // Remplace "rpc.Matlab." par ""
                methodString = methodString.replaceAll(packName+".", ""); //  "rpc." par ""
                Type[] ts = m.getGenericParameterTypes();
                List<Type> ts2 = Arrays.asList(ts);
                for(Type t : ts2){
                    IOStream(targetClientLines, targetLines, t.getTypeName(), false);
                    methodParameterList.add(t.getTypeName());
                    methodString = methodString.replaceAll(t.getTypeName(), t.getTypeName()+" "+t.getTypeName().charAt(0)+ts2.indexOf(t));
                }
                methodList.add(methodString);
            }

            targetLines.add("\t}\n}\n");

            try {
                FileWriter fw = new FileWriter(target);
                BufferedWriter bw = new BufferedWriter(fw);

                for(String line : targetLines){
                    if(!line.endsWith("\n")){
                        bw.write(line+"\n");
                    } else {
                        bw.write(line);
                    }
                }
                bw.flush();
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Files.copy(source.toPath(), target.toPath());


        } catch (Exception e) {
            e.printStackTrace();
        }     
    }

    public static void IOStream(List<String> tCL, List<String> tL, String val, boolean returnType) {
        if(returnType){ 
            boolean dataDone = false;
            boolean fileDone = false;
            boolean byteArrayDone = false;
            boolean objectDone = false;
            String targetOutputStream = "";
            if(dataIOStreamType.contains(val) && !dataDone){
                dataDone = true;
                tL.add("\t\tjava.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream());\n");
                tCL.add("\t\tjava.io.DataInputStream dis = new java.io.DataInputStream(s.getInputStream());\n");
            } else if (fileIOStreamTypes.contains(val) && !fileDone){
                fileDone = true;
                tL.add("\t\tjava.io.FileOutputStream fos = new java.io.FileOutputStream(s.getOutputStream());\n");
                tCL.add("\t\tjava.io.FileInputStream fis = new java.io.FileInputStream(s.getInputStream());\n");
            } else if (byteArrayIOStreamTypes.contains(val) && !byteArrayDone){
                byteArrayDone = true;
                tL.add("\t\tjava.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(s.getOutputStream());\n");
                tCL.add("\t\tjava.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(s.getInputStream());\n");
            } else if (!objectDone){
                objectDone = true;
                tL.add("\t\tjava.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(s.getOutputStream());\n");
                tCL.add("\t\tjava.io.ObjectInputStream ois = new java.io.ObjectInputStream(s.getInputStream());\n");
            }
        } else { 
            boolean dataDone = false;
            boolean fileDone = false;
            boolean byteArrayDone = false;
            boolean objectDone = false;
            String targetOutputStream = "";
            if(dataIOStreamType.contains(val) && !dataDone){
                dataDone = true;
                tCL.add("\t\tjava.io.DataOutputStream dos = new java.io.DataOutputStream(s.getOutputStream());\n");
                tL.add("\t\tjava.io.DataInputStream dis = new java.io.DataInputStream(s.getInputStream());\n");
            } else if (fileIOStreamTypes.contains(val) && !fileDone){
                fileDone = true;
                tCL.add("\t\tjava.io.FileOutputStream fos = new java.io.FileOutputStream(s.getOutputStream());\n");
                tL.add("\t\tjava.io.FileInputStream fis = new java.io.FileInputStream(s.getInputStream());\n");
            } else if (byteArrayIOStreamTypes.contains(val) && !byteArrayDone){
                byteArrayDone = true;
                tCL.add("\t\tjava.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(s.getOutputStream());\n");
                tL.add("\t\tjava.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(s.getInputStream());\n");
            } else if (!objectDone){
                objectDone = true;
                tCL.add("\t\tjava.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(s.getOutputStream());\n");
                tL.add("\t\tjava.io.ObjectInputStream ois = new java.io.ObjectInputStream(s.getInputStream());\n");
            }
        }
    }
}