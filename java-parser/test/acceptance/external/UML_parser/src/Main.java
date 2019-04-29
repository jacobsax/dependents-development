import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class Main
{

	private static PrintWriter pw;
	private static Map<String, CompilationUnit> classesAndCu = new HashMap<String, CompilationUnit>();
	private static StringBuilder plantUmlSource;
	private static StringBuilder writeDependency = new StringBuilder();
	private static StringBuilder writeClassHierarchy = new StringBuilder();
	private static String[][] dependencyMultiplicityTable;
	private static Vector<String> allMultipleDependencies = new Vector<String>();
	private static Vector<String> classes = new Vector<String>();
	
	private static StringBuilder writeUsesDependency = new StringBuilder();
	private static boolean[][] dependencyUsesTable;

	public static void main(String[] args)
	{
		// deal with args for command line interface
		// String srcFolder = args[0];
		// String outputFolder = args[1];

		// temp, hard code file location:
		String srcFolder = args[0];
		
	//	String srcFolder = "C:/Software/202/cmpe202-master/umlparser/uml-parser-test-2";

		allMultipleDependencies.add("[]");
		allMultipleDependencies.add("Collection");

		System.out.println(srcFolder);
		File mainFolder = new File(srcFolder);
		if (!mainFolder.exists())
		{
			// error
		}
		if (!mainFolder.isDirectory())
		{
			mainFolder = mainFolder.getParentFile();
		}

		try
		{
			// testUML();
			plantUmlSource = new StringBuilder();
			plantUmlSource.append("@startuml \n");
			// plantUmlSource.append("!pragma graphviz_dot jdot \n");
			plantUmlSource.append("skinparam classAttributeIconSize 0 \n");
			getFiles(mainFolder);
			createDependencyTable();
			createDependencyUsesTable();
			createStringForPlantUML();

			plantUmlSource.append("@enduml");

			System.out.println(plantUmlSource);

			SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
			FileOutputStream output = new FileOutputStream(new File(args[1]));
			reader.generateImage(output, new FileFormatOption(FileFormat.PNG, false));

		}
		catch (FileNotFoundException e)
		{			
			e.printStackTrace();
		}
		catch (IOException e2)
		{

		}

		// ClassOrInterfaceDeclaration classA =
		// compilationUnit.getClassByName("A");

	}

	private static void createDependencyUsesTable()
	{
		
		dependencyUsesTable = new boolean[classesAndCu.size()][classesAndCu.size()];
	}

	private static void createDependencyTable()
	{
		String[] tempArry = new String[classesAndCu.size()];
		classesAndCu.keySet().toArray(tempArry);
		// adding to array for index.
		classes.addAll(Arrays.asList(tempArry));

		dependencyMultiplicityTable = new String[classesAndCu.size()][classesAndCu.size()];

	}

	private static void createStringForPlantUML()
	{
		Set<Entry<String, CompilationUnit>> entrySet = classesAndCu.entrySet();

		for (Entry<String, CompilationUnit> entry : entrySet)
		{

			if (isInterface(entry.getKey()))
			{
				plantUmlSource.append("interface "   );
				plantUmlSource.append(entry.getKey());
				plantUmlSource.append( " <<" + "interface" + ">>" );
			}
			else
			{
				plantUmlSource.append("class ");
				plantUmlSource.append(entry.getKey());
			}
		//	plantUmlSource.append(entry.getKey());
			plantUmlSource.append("{ ");
			CompilationUnit cu = entry.getValue();
			writeMembers(entry.getKey(), cu);
			plantUmlSource.append("\n ");
			plantUmlSource.append("} ");
			plantUmlSource.append("\n ");

		}
		writeClassDependency();
		writeClassHierarchy();
		writeUsesDependency();

	}

	private static void writeUsesDependency()
	{
		
		
		
		for (int i = 0; i < classes.size(); i++)
		{
			for (int j = 0; j < classes.size(); j++)
			{
				if(dependencyUsesTable[i][j]==true)
				{
					String className = classes.get(i);
					String typeName = classes.get(j);
					
					writeUsesDependency.append("\n");
					
					writeUsesDependency
					.append(className + " " + "..> " + " " + typeName +" : uses" );
					writeUsesDependency.append("\n");

				}
				

			}

		}
		plantUmlSource.append(writeUsesDependency.toString());
		
	
		
	}

	public static boolean isInterface(String className)
	{
		CompilationUnit compilationUnit = classesAndCu.get(className);
		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit
				.getNodesByType(ClassOrInterfaceDeclaration.class).get(0);
		return classOrInterfaceDeclaration.isInterface();

	}

	private static void writeClassDependency()
	{
		for (int i = 0; i < classes.size(); i++)
		{
			for (int j = i + 1; j < classes.size(); j++)
			{
				String dependencyNumber1 = GetDependencyNumber(i, j);
				String dependencyNumber2 = GetDependencyNumber(j, i);
				if (dependencyNumber1.equalsIgnoreCase("") && dependencyNumber2.equalsIgnoreCase(""))
				{
					continue;
				}

				writeDependency.append(classes.get(i) + " " + GetDependencyNumber(j, i) + " ---- "
						+ GetDependencyNumber(i, j) + " " + classes.get(j));
				writeDependency.append(" \n ");

			}

		}
		plantUmlSource.append(writeDependency);
	}

	private static String getArrow(int i, int j)
	{

		String classA = classes.get(i);
		String classB = classes.get(i);
		CompilationUnit compilationUnit = classesAndCu.get(classB);
		List<ClassOrInterfaceDeclaration> nodesByType = compilationUnit
				.getNodesByType(ClassOrInterfaceDeclaration.class);
		Iterator<ClassOrInterfaceDeclaration> iterator = nodesByType.iterator();
		if (iterator.next().isInterface())
		{
			// return
		}
		else
		{

		}

		return null;
	}

	private static String GetDependencyNumber(int i, int j)
	{
		String retString = dependencyMultiplicityTable[i][j];
		if (retString == null)
		{
			retString = "";
		}
		else
		{
			retString = "\"" + retString + "\"";
		}

		return retString;

	}

	// private static class MethodChangerVisitor extends
	// VoidVisitorAdapter<Void>
	// {
	// @Override
	// public void visit(MethodDeclaration n, Void arg)
	// {
	// // change the name of the method to upper case
	// n.getDeclarationAsString();
	// // n.setName(n.getNameAsString().toUpperCase());
	//
	// // add a new parameter to the method
	// n.addParameter("int", "value");gu
	// }
	// }

	private static void findMethod(CompilationUnit cu)
	{
		NodeList<TypeDeclaration<?>> types = cu.getTypes();
		for (TypeDeclaration<?> type : types)
		{
			// Go through all fields, methods, etc. in this type
			NodeList<BodyDeclaration<?>> members = type.getMembers();
			for (BodyDeclaration<?> member : members)
			{

				if (member instanceof MethodDeclaration)
				{
					MethodDeclaration method = (MethodDeclaration) member;
					System.out.println(method);

				}
			}
		}

	}

	private static void writeMembers(String className, CompilationUnit cu)
	{
		// Go through all the types in the file
		NodeList<TypeDeclaration<?>> types = cu.getTypes();
		for (TypeDeclaration<?> type : types)
		{
			// Go through all fields, methods, etc. in this type
			NodeList<BodyDeclaration<?>> members = type.getMembers();
			for (BodyDeclaration<?> member : members)
			{
				if(member instanceof ConstructorDeclaration)
				{
					ConstructorDeclaration constructor = (ConstructorDeclaration) member;

					
					EnumSet<Modifier> modifiers = constructor.getModifiers();
					AccessSpecifier accessSpecifier = Modifier.getAccessSpecifier(modifiers);
					if (accessSpecifier.equals(AccessSpecifier.PUBLIC))
					{
						plantUmlSource.append("\n");
						plantUmlSource.append("+" + constructor.getName() + "(");
						NodeList<Parameter> parameters = constructor.getParameters();
						ListIterator<Parameter> listIterator = parameters.listIterator(0);

						while (listIterator.hasNext())
						{
							Parameter parameter = listIterator.next();
							plantUmlSource.append(parameter.getName());

							if (listIterator.hasNext() == true)
							{
								plantUmlSource.append(",");
							}
						}

						plantUmlSource.append(")");

					}

					plantUmlSource.append("\n");

				
				}

				if (member instanceof MethodDeclaration)
				{
					MethodDeclaration method = (MethodDeclaration) member;
			
					
					buildUsesDependency(className,method);					
					
					if(isOverriden(className,method))
					{
						continue;
					}
					
					EnumSet<Modifier> modifiers = method.getModifiers();
					AccessSpecifier accessSpecifier = Modifier.getAccessSpecifier(modifiers);
					if (accessSpecifier.equals(AccessSpecifier.PUBLIC))
					{
						plantUmlSource.append("\n");
						plantUmlSource.append("+" + method.getName() + "(");
						NodeList<Parameter> parameters = method.getParameters();
						ListIterator<Parameter> listIterator = parameters.listIterator(0);

						while (listIterator.hasNext())
						{
							Parameter parameter = listIterator.next();
							plantUmlSource.append(parameter.getName());

							if (listIterator.hasNext() == true)
							{
								plantUmlSource.append(",");
							}
						}

						plantUmlSource.append(")");

					}

					plantUmlSource.append("\n");

				}

				if (member instanceof FieldDeclaration)
				{
					FieldDeclaration field = (FieldDeclaration) member;
					// field.ge
					EnumSet<Modifier> modifiers = field.getModifiers();
					AccessSpecifier accessSpecifier = Modifier.getAccessSpecifier(modifiers);
					// Iterator<Modifier> iterator = modifiers.iterator();
					// while(iterator.hasNext())
					// {
					// Modifier next = iterator.next();
					// next.getAccessSpecifier(modifiers)
					// }
					System.out.println(field.getClass());
					field.getNodeLists();
					field.getChildNodes();
					NodeList<VariableDeclarator> variables = field.getVariables();

					for (VariableDeclarator variable : variables)
					{
						String classNameInField = variable.getType().toString();

						variable.getChildNodes();
						// try
						// {
						// System.out.println(" \n array or not :" +
						// variable.getChildNodes().get(1).getChildNodes().get(0));
						// System.out.println(" \n class names :" +
						// variable.getChildNodes().get(1).getChildNodes().get(1));
						// }
						// catch(Exception e)
						// {
						// System.out.println(" \n class Name from Catch " +
						// classNameInField );
						// }

						// System.out.println(" \n Field Names " +
						// classNameInField );

						variable.getNodeLists();

						String parsedClassName = parseClassNameFromString(classNameInField);
						if (classesAndCu.containsKey(parsedClassName))
						{

							insertDependencyNumber(className, parsedClassName,
									parseMultiplicityFromString(classNameInField));
							// dependency[getIndexOfClass(class)]
							// classes.

							// writeDependency.append(className + " ---- "
							// +classNameInField);
							// writeDependency.append(" \n ");
						}
						else
						{
							if (accessSpecifier.equals(AccessSpecifier.PUBLIC))
							{
								plantUmlSource.append(" \n ");
								plantUmlSource.append(" + " + field);
								plantUmlSource.append(" \n ");
							}

							else if (accessSpecifier.equals(AccessSpecifier.PRIVATE))
							{
								plantUmlSource.append(" \n ");
								plantUmlSource.append(" - " + field);
								plantUmlSource.append(" \n ");
							}

						}
					}

				}

			}
		}

	}
	
	
	private static void buildUsesDependency(String className,MethodDeclaration method)
	{
		NodeList<Parameter> parametersForUsesDependency = method.getParameters();
		
		for(int z=0; z<parametersForUsesDependency.size();z++)
		{
			Parameter parameter = parametersForUsesDependency.get(z);
			String TypeName = parameter.getType().toString();
			if(classes.contains(TypeName))
			{
				int i =classes.indexOf(className);
				int j = classes.indexOf(TypeName);
				
				dependencyUsesTable[i][j] = true;
				
//				writeUsesDependency.append("\n");
//				
//				writeUsesDependency
//				.append(className + " " + "..> " + " " + TypeName +" : uses" );
//				writeUsesDependency.append("\n");
			}
				
		}
		
		
	}

	private static boolean isOverriden(String className, MethodDeclaration method)
	{
		boolean retBool = false;
		
		
		CompilationUnit cu = classesAndCu.get(className);

		List<ClassOrInterfaceDeclaration> nodesByType = cu.getNodesByType(ClassOrInterfaceDeclaration.class);
		Iterator<ClassOrInterfaceDeclaration> iterator = nodesByType.iterator();
		ClassOrInterfaceDeclaration currentClass = iterator.next();
		NodeList<ClassOrInterfaceType> extendedTypes = currentClass.getExtendedTypes();

		for (int j = 0; j < extendedTypes.size(); j++)
		{
			ClassOrInterfaceType extendedTypeClass = extendedTypes.get(j);
			List<MethodDeclaration> methods = extendedTypeClass.getNodesByType(MethodDeclaration.class);
			for(int k = 0 ; k <methods.size(); k++ )
			{
				if(methods.get(k).getName().toString().equalsIgnoreCase(method.getName().toString()))
				{
					return true;
				}
			}
			

		}

		NodeList<ClassOrInterfaceType> implementedTypes = currentClass.getImplementedTypes();
		for (int j = 0; j < implementedTypes.size(); j++)
		{
			ClassOrInterfaceType implementedTypeClass = implementedTypes.get(j);
			
			
			
			
			
			List<MethodDeclaration> methods = classesAndCu.get(implementedTypeClass.getNameAsString()).getNodesByType(MethodDeclaration.class);
			for(int k = 0 ; k <methods.size(); k++ )
			{
				if(methods.get(k).getName().toString().equalsIgnoreCase(method.getName().toString()))
				{
					return true;
				}
			}
		

		}
		return retBool;
	}
		
		
		
	

	public static void getFiles(File folder) throws FileNotFoundException
	{

		File[] allFiles = folder.listFiles();
		for (int i = 0; i < allFiles.length; i++)
		{
			if (allFiles[i].isFile() && allFiles[i].getName().toLowerCase().endsWith(".java"))
			{

				System.out.println("File " + allFiles[i].getName());
				CompilationUnit cu = JavaParser.parse(allFiles[i]); // remove
																	// throws
																	// and add
																	// try catch
				NodeList<TypeDeclaration<?>> types = cu.getTypes();

				cu.getNodesByType(FieldDeclaration.class).stream()
						.filter(f -> f.getModifiers().contains(AccessSpecifier.PUBLIC))
						.forEach(f -> System.out.println("Check field at line " + f.getBegin().get().line));

				classesAndCu.put(allFiles[i].getName().substring(0, allFiles[i].getName().length() - 5), cu);

			}
			else if (allFiles[i].isDirectory())
			{
				getFiles(allFiles[i]);
			}
		}
	}

	public static void testUML() throws IOException
	{
		StringBuilder plantUmlSource = new StringBuilder();
		plantUmlSource.append("@startuml\n");
		plantUmlSource.append("Alice -> Bob: Authentication Request\n");
		plantUmlSource.append("Bob --> Alice: Authentication Response\n");
		plantUmlSource.append("@enduml");
		SourceStringReader reader = new SourceStringReader(plantUmlSource.toString());
		FileOutputStream output = new FileOutputStream(new File("C:\\Software\\202\\test\\test.png"));
		reader.generateImage(output, new FileFormatOption(FileFormat.PNG, false));
	}

	public static void writeClassHierarchy()
	{
		for (int i = 0; i < classes.size(); i++)
		{
			CompilationUnit cu = classesAndCu.get((classes.get(i)));

			List<ClassOrInterfaceDeclaration> nodesByType = cu.getNodesByType(ClassOrInterfaceDeclaration.class);
			Iterator<ClassOrInterfaceDeclaration> iterator = nodesByType.iterator();
			ClassOrInterfaceDeclaration currentClass = iterator.next();
			NodeList<ClassOrInterfaceType> extendedTypes = currentClass.getExtendedTypes();

			for (int j = 0; j < extendedTypes.size(); j++)
			{
				ClassOrInterfaceType extendedTypeClass = extendedTypes.get(j);
				writeClassHierarchy
						.append(classes.get(i) + " " + " ----|> " + " " + extendedTypeClass.getNameAsString());
				writeClassHierarchy.append(" \n ");

			}

			NodeList<ClassOrInterfaceType> implementedTypes = currentClass.getImplementedTypes();
			for (int j = 0; j < implementedTypes.size(); j++)
			{
				ClassOrInterfaceType implementedTypeClass = implementedTypes.get(j);
				writeClassHierarchy
						.append(classes.get(i) + " " + "..> " + " " + implementedTypeClass.getNameAsString());
				writeClassHierarchy.append(" \n ");

			}
		}

		plantUmlSource.append(writeClassHierarchy.toString());
	}

	public static void insertDependencyNumber(String classNameIn, String classNameMember, String dependentFactor)
	{

		int indexRow = classes.indexOf(classNameIn);
		int indexColumn = classes.indexOf(classNameMember);

		dependencyMultiplicityTable[indexRow][indexColumn] = dependentFactor;

	}

	public static String parseClassNameFromString(String classString)
	{

		String retString = classString;

		if (classString.contains("[]"))
		{
			retString = classString.substring(0, classString.length() - 2);
		}
		else if (classString.contains("Collection"))
		{
			String tempString;
			tempString = classString.substring(10);
			retString = tempString.substring(1, tempString.length() - 1);
		}

		return retString;
	}

	public static String parseMultiplicityFromString(String classString)
	{

		String retString = "1";

		if (classString.contains("[]"))
		{
			retString = "*";
		}
		else if (classString.contains("Collection"))
		{
			retString = "0..*";
		}
		return retString;

	}
}
