package rpc;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
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
			String clientName = "";
            if(args.length < 4){
                System.out.println("To run:\n" +
                "\tjava packageName.MonCompile packageName.file packageName.clientFile xxx.xxx.xxx.xxx yyyy\n" +
                "\tFirst parameter -> Java file to distribute\n" +
                "\tSecond parameter -> Java client file to distribute\n" +
				"\tThird parameter -> IP Adress on which to send data\n" +
                "\tFourth parameter -> Port on which to send data");
                System.exit(1);
            } else {
				if(!args[0].contains(".")){
					System.out.println("The file must contain a package name.");
				} else {
					classPackName = args[0];
				}
				if(!args[1].contains(".")){
					System.out.println("The file must contain a package name.");
				} else {
					clientName = args[1];
				}
                socketIP = args[2].toString();
                socketPort = args[3].toString();
            }
            Class inputClass = Class.forName(classPackName); // Contient l'objet Class du paramètre donné en argument
			Class clientClass = Class.forName(clientName);
            String packName = inputClass.getPackageName(); // Nom du package de la classe donné par args[0]
			String clientPackName = clientClass.getPackageName();
            String className = classPackName.replace(packName+".", ""); // Nom de la classe
            List<String> fieldsList = new ArrayList<String>(); // Liste pour les attributs de la classe
            List<String> fieldsNameList = new ArrayList<String>(); // Liste pour le nom des attributs de la classe
            List<String> constructorList = new ArrayList<String>(); // Liste pour les constructeurs de la classe
            List<String> methodList = new ArrayList<String>(); // Liste pour les méthodes de la classe
            List<String> methodParameterList = new ArrayList<String>(); // Liste pour les paramètres des méthodes
            
            // Fichier source du code donné en argument
            Path filePath = Paths.get("").toAbsolutePath();
            File source = new File(filePath.toString()+"\\"+className+".java");
			File clientSource = new File(filePath.toString()+"\\Client.java");

            // Lieu de sortie des fichiers crée par le compilateur
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
			String targetClientLine = null;
			int startLine = 0;
			int endLine = 0;
			String[] clientTempArray = null;
            
            try {
                FileReader fr = new FileReader(source);
                BufferedReader br = new BufferedReader(fr);

				Scanner clientScanner = new Scanner(clientSource);

                while ((targetLine = br.readLine()) != null){
                    if(!targetLine.startsWith("//")){ // && !targetLine.isEmpty()){ // Pour trier les commentaires
                        targetLines.add(targetLine);
                    }
                }

				clientScanner.useDelimiter("\\A");
				targetClientLine = clientScanner.next();
				clientTempArray = targetClientLine.split("\n");
				int i = 0;
				
				while(i < clientTempArray.length){
					if(clientTempArray[i].contains("main")){
						startLine = i+1;
						break;
					}
					i++;
				}

				endLine = startLine;

				while(endLine < clientTempArray.length){
					if(clientTempArray[i].contains("}")){
						endLine = i-1;
						break;
					}
					i++;
				}

                fr.close();
                br.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

			String[] clientMainContents = new String[endLine-startLine+1];

			for(int j = startLine; j <= endLine; j++){
				clientMainContents[j-startLine] = clientTempArray[j];
			}

            // Suppression de la dernière ligne du code qui est simplement la dernière accolade
            // Cela permet de pouvoir rajouter la fonction main nécessaire à faire tourner le code
            targetLines.remove(targetLines.size() - 1);

			List<String> constructorParameters = new ArrayList<String>();

			Constructor[] cs = inputClass.getDeclaredConstructors();
			for(Constructor c : cs){
				Type[] ts = c.getGenericParameterTypes();
				List<Type> ts2 = Arrays.asList(ts);
				for(Type t : ts2){
					constructorParameters.add(t.getTypeName());
				}
			}

            // targetLines contient le fichier Matlab.java sans la dernière accolade
            // Il faut maintenant écrire la fonction main qui sera implémentée
            char targetVariableName = className.toLowerCase().charAt(0);
            targetLines.add("\n\tpublic static void main(String[] args) throws Exception {\n");
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

			for(int i = 0; i < clientMainContents.length; i++){
				if(clientMainContents[i].contains(className)){
					targetClientLines.add("\t\tdos.writeUTF(\"constructor\");\n");
					
					String temp = "";
					for(String type : constructorParameters){
						temp = "\t\tdos.write"+type.substring(0, 1).toUpperCase() + type.substring(1)+"(";
					}

					Pattern pattern = Pattern.compile("\\((.*)\\)");
					Matcher m = pattern.matcher(clientMainContents[i]);
					String result = null;
					String[] parametersToSend = null;
					while(m.find()){
						result = m.group(1);
						result.replace(", ", ",");
						parametersToSend = result.split(",");
					}
					for(int j = 0; j < parametersToSend.length; j++){
						temp = temp + parametersToSend[j] + ");";
					}
					targetClientLines.add(temp);
				}
			}

			targetClientLines.add("\t}\n}\n");

            try {
                FileWriter fw = new FileWriter(target);
				FileWriter cfw = new FileWriter(targetClient);

                BufferedWriter bw = new BufferedWriter(fw);
				BufferedWriter cbw = new BufferedWriter(cfw);

                for(String line : targetLines){
                    if(!line.endsWith("\n")){
                        bw.write(line+"\n");
                    } else {
                        bw.write(line);
                    }
                }

				for(String line : targetClientLines){
					if(!line.endsWith("\n")){
						cbw.write(line+"\n");
					} else {
						cbw.write(line);
					}
				}

                bw.flush();
				cbw.flush();

                bw.close();
				cbw.close();

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