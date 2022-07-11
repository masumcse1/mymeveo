package org.meveo.service.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.meveo.service.custom.CustomEntityTemplateService;
import org.meveo.service.script.maven.MavenClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compile a String or other {@link CharSequence}, returning a Java
 * {@link Class} instance that may be instantiated. This class is a Facade
 * around {@link JavaCompiler} for a narrower use case, but a bit easier to use.
 * <p>
 * To compile a String containing source for a Java class which implements
 * MyInterface:
 * 
 * <pre>
 * ClassLoader classLoader = MyClass.class.getClassLoader(); // optional; null is also OK 
 * List&lt;Diagnostic&gt; diagnostics = new ArrayList&lt;Diagnostic&gt;(); // optional; null is also OK
 * JavaStringCompiler&lt;Object&gt; compiler = new JavaStringCompiler&lt;MyInterface&gt;(classLoader,
 *       null);
 * try {
 *    Class&lt;MyInterface&gt; newClass = compiler.compile(&quot;com.mypackage.NewClass&quot;,
 *          stringContaininSourceForNewClass, diagnostics, MyInterface);
 *    MyInterface instance = newClass.newInstance();
 *    instance.someOperation(someArgs);
 * } catch (JavaStringCompilerException e) {
 *    handle(e);
 * } catch (IllegalAccessException e) {
 *    handle(e);
 * }
 * </pre>
 * 
 * The source can be in a String, {@link StringBuffer}, or your own class which
 * implements {@link CharSequence}. If you implement your own, it must be
 * thread safe (preferably, immutable.)
 * 
 * @author <a href="mailto:David.Biesack@sas.com">David J. Biesack</a>
 * @param <T> Type that the compiled script should have
 */
public class CharSequenceCompiler<T> {
   // Compiler requires source files with a ".java" extension:
   static final String JAVA_EXTENSION = ".java";
   
   static final Logger LOGGER = LoggerFactory.getLogger(CharSequenceCompiler.class);

   private static final JavaFileManager fileManager;

   // The compiler instance that this facade uses.
   private static final JavaCompiler compiler;

   // The compiler options (such as "-target" "1.5").
   private final List<String> options;

   // The FileManager which will store source and class "files".
   private final FileManagerImpl javaFileManager;
   
   private final ClassLoaderImpl classLoader;
   
   private static URLClassLoader urlClassLoader;
   
   static {
	   compiler = ToolProvider.getSystemJavaCompiler();
	   if (compiler == null) {
		   throw new IllegalStateException("Cannot find the system Java compiler. Check that your class path includes tools.jar");
	   }
	   fileManager = compiler.getStandardFileManager(null, null, null);
	   
      try {
    	  String classPath = ScriptInstanceService.CLASSPATH_REFERENCE.get();
    	  List<URL> urlList = Arrays.stream(classPath.split(File.pathSeparator))
    			  .map(path -> {
					try {
						return new File(path).toURI().toURL();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
				}).collect(Collectors.toList());
    	  urlList.add(CustomEntityTemplateService.getClassesDir(null).toURI().toURL());
    	  
	      URL[] urls = urlList.toArray(URL[]::new);
	      
	      ClassLoaderImpl classLoaderImpl = new ClassLoaderImpl(CharSequenceCompiler.class.getClassLoader());
		  urlClassLoader = new URLClassLoader(urls, classLoaderImpl);
      } catch (MalformedURLException e) {
    	  throw new RuntimeException(e);
      }
   }
   
   public static ClassLoader getUrlClassLoader() {
	   return urlClassLoader;
   }
   
   public static <T> Class<T> getCompiledClass(final String qualifiedName) throws ClassNotFoundException {
	   return (Class<T>) urlClassLoader.loadClass(qualifiedName);
   }

   /**
    * Construct a new instance which delegates to the named class loader.
    * 
    * @param loader
    *           the application ClassLoader. The compiler will look through to
    *           this // class loader for dependent classes
    * @param options
    *           The compiler options (such as "-target" "1.5"). See the usage
    *           for javac
    * @throws IllegalStateException
    *            if the Java compiler cannot be loaded.
    */
   public CharSequenceCompiler(ClassLoader loader, Iterable<String> options) {
      classLoader = new ClassLoaderImpl(loader);
      
      // create our FileManager which chains to the default file manager and our ClassLoader
	  javaFileManager = new FileManagerImpl(fileManager, classLoader);
      this.options = new ArrayList<String>();
      if (options != null) { // make a save copy of input options
         for (String option : options) {
            this.options.add(option);
         }
      }
   }

   /**
    * Compile Java source in &lt;var&gt;javaSource&lt;/name&gt; and return the resulting
    * class.
    * &lt;p&gt;
    * Thread safety: this method is thread safe if the &lt;var&gt;javaSource&lt;/var&gt;
    * and &lt;var&gt;diagnosticsList&lt;/var&gt; are isolated to this thread.
    * 
    * @param qualifiedClassName
    *           The fully qualified class name.
    * @param javaSource
    *           Complete java source, including a package statement and a class,
    *           interface, or annotation declaration.
    * @param diagnosticsList
    *           Any diagnostics generated by compiling the source are added to
    *           this collector.
    * @param types
    *           zero or more Class objects representing classes or interfaces
    *           that the resulting class must be assignable (castable) to.
    * @return a Class which is generated by compiling the source
    * @throws CharSequenceCompilerException
    *            if the source cannot be compiled - for example, if it contains
    *            syntax or semantic errors or if dependent classes cannot be
    *            found.
    * @throws ClassCastException
    *            if the generated class is not assignable to all the optional
    *            &lt;var&gt;types&lt;/var&gt;.
    */
   public synchronized Class<T> compile(
		 final String sourcePath,
		 final String qualifiedClassName,
         final CharSequence javaSource,
         final DiagnosticCollector<JavaFileObject> diagnosticsList,
         final boolean isTestCompile,
         final Class<?>... types) throws CharSequenceCompilerException,
         ClassCastException {

	  JavaFileObjectImpl compilationUnit = new JavaFileObjectImpl(qualifiedClassName, javaSource);
      Class<T> newClass = compile(sourcePath, List.of(compilationUnit), diagnosticsList, isTestCompile)
    		  .get(qualifiedClassName);
      return castable(newClass, types);
   }

	/**
	 * Compile a java class.
	 * <br>
	 * Thread safety: this method is thread safe if the &lt;var&gt;classes&lt;/var&gt; and &lt;var&gt;diagnosticsList&lt;/var&gt; are isolated to this
	 * thread.
	 * 
	 * @param qualifiedClassName Name of the class to compile
	 * @param content            Content of the class
	 * 
	 * @param diagnosticsList    Any diagnostics generated by compiling the source are added to this list.
	 * @return the compiled class
	 * @throws CharSequenceCompilerException if the source cannot be compiled
	 */
   @SuppressWarnings("unchecked")
   public synchronized Map<String, Class<T>> compile(
		   final String sourcePath,
		   List<JavaFileObjectImpl> compilationUnits,
		   final DiagnosticCollector<JavaFileObject> diagnosticsList,
		   final boolean isTestCompile)  throws CharSequenceCompilerException {
	   
	   Map<String, Class<T>> results = new HashMap<>();
	   if (compilationUnits == null || compilationUnits.isEmpty()) {
		   return results;
	   }
	   
	   Set<String> classNames = compilationUnits.stream().map(obj -> obj.getClassName()).collect(Collectors.toSet());

	   File classesDirectory = CustomEntityTemplateService.getClassesDir(null);
	   File outputDir;
		try {
			outputDir = !isTestCompile ? classesDirectory : Files.createTempDirectory("test-compile").toFile();
		} catch (IOException e1) {
			throw new RuntimeException("Cannot create temp directory", e1);
		}

	   if (!classesDirectory.exists()) {
		   classesDirectory.mkdirs();
	   }

	   // Set source directory
	   options.add("-sourcepath");
	   options.add(sourcePath);
	   
	   // Set output directory
	   options.add("-d");
	   options.add(outputDir.getAbsolutePath());
	   
	   // Get a CompliationTask from the compiler and compile the sources
	   final CompilationTask task = compiler.getTask(
			   null, 
			   javaFileManager, //XXX: Use a file manager to improve perfs ?
			   diagnosticsList,
			   options,
			   null, 
			   compilationUnits
		   );

	   final Boolean result = task.call();

	   if (result == null || !result.booleanValue()) {
		   throw new CharSequenceCompilerException("Compilation failed.", classNames, diagnosticsList);
	   }

	   try {
		   URL[] urls = { outputDir.toURI().toURL() };

		   if(isTestCompile) {
			   // Use a temporary classLoader if compilation is test
			   try (var tmpClassLoader = new URLClassLoader(urls, classLoader)) {
				   for (var unit : compilationUnits) {
					   results.put(unit.getClassName(), (Class<T>) tmpClassLoader.loadClass(unit.getClassName()));
				   }
			   }
		   } else {
			   urlClassLoader.close(); // Close previous UrlClassLoader
			   urlClassLoader = new URLClassLoader(urls, this.getClassLoader()); // Re-instantiate a new one
			   for (var unit : compilationUnits){
				   results.put(unit.getClassName(), (Class<T>) urlClassLoader.loadClass(unit.getClassName()));
			   }
		   }
		   
	   } catch (Exception e) {
		   throw new CharSequenceCompilerException(classNames, e, diagnosticsList);
	   }
	   
	   return results;
   }

   /**
    * Load a class that was generated by this instance or accessible from its
    * parent class loader. Use this method if you need access to additional
    * classes compiled by
    * {@link #compile(String, CharSequence, DiagnosticCollector, Class...) compile()},
    * for example if the primary class contained nested classes or additional
    * non-public classes.
    * 
    * @param qualifiedClassName
    *           the name of the compiled class you wish to load
    * @return a Class instance named by &lt;var&gt;qualifiedClassName&lt;/var&gt;
    * @throws ClassNotFoundException
    *            if no such class is found.
    */
   @SuppressWarnings("unchecked")
   public Class<T> loadClass(final String qualifiedClassName)
         throws ClassNotFoundException {
      return (Class<T>) classLoader.loadClass(qualifiedClassName);
   }
   
   /**
    * Check that the &lt;var&gt;newClass&lt;/var&gt; is a subtype of all the type
    * parameters and throw a ClassCastException if not.
    * 
    * @param types
    *           zero of more classes or interfaces that the &lt;var&gt;newClass&lt;/var&gt;
    *           must be castable to.
    * @return &lt;var&gt;newClass&lt;/var&gt; if it is castable to all the types
    * @throws ClassCastException
    *            if &lt;var&gt;newClass&lt;/var&gt; is not castable to all the types.
    */
   private Class<T> castable(Class<T> newClass, Class<?>... types)
         throws ClassCastException {
      for (Class<?> type : types)
         if (!type.isAssignableFrom(newClass)) {
            throw new ClassCastException(type.getName());
         }
      return newClass;
   }

   /**
    * COnverts a String to a URI.
    * 
    * @param name
    *           a file name
    * @return a URI
    */
   static URI toURI(String name) {
      try {
         return new URI(name);
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @return This compiler's class loader.
    */
   public ClassLoader getClassLoader() {
      return javaFileManager.getClassLoader();
   }
}

/**
 * A JavaFileManager which manages Java source and classes. This FileManager
 * delegates to the JavaFileManager and the ClassLoaderImpl provided in the
 * constructor. The sources are all in memory CharSequence instances and the
 * classes are all in memory byte arrays.
 */
final class FileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {
   // the delegating class loader (passed to the constructor)
   private final ClassLoaderImpl classLoader;

   // Internal map of filename URIs to JavaFileObjects.
   private final Map<URI, JavaFileObject> fileObjects = new HashMap<URI, JavaFileObject>();

   /**
    * Construct a new FileManager which forwards to the &lt;var&gt;fileManager&lt;/var&gt;
    * for source and to the &lt;var&gt;classLoader&lt;/var&gt; for classes
    * 
    * @param fileManager
    *           another FileManager that this instance delegates to for
    *           additional source.
    * @param classLoader
    *           a ClassLoader which contains dependent classes that the compiled
    *           classes will require when compiling them.
    */
   public FileManagerImpl(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
      super(fileManager);
      this.classLoader = classLoader;
   }
   
   /**
    * @return the class loader which this file manager delegates to
    */
   public ClassLoader getClassLoader() {
	   return classLoader;
   }

   /**
    * For a given file &lt;var&gt;location&lt;/var&gt;, return a FileObject from which the
    * compiler can obtain source or byte code.
    * 
    * @param location
    *           an abstract file location
    * @param packageName
    *           the package name for the file
    * @param relativeName
    *           the file's relative name
    * @return a FileObject from this or the delegated FileManager
    * @see ForwardingJavaFileManager#getFileForInput(Location,
    *      String, String)
    */
   @Override
   public FileObject getFileForInput(Location location, String packageName,
         String relativeName) throws IOException {
      FileObject o = fileObjects.get(uri(location, packageName, relativeName));
      if (o != null)
         return o;
      return super.getFileForInput(location, packageName, relativeName);
   }

   /**
    * Store a file that may be retrieved later with
    * {@link #getFileForInput(Location, String, String)}
    * 
    * @param location
    *           the file location
    * @param packageName
    *           the Java class' package name
    * @param relativeName
    *           the relative name
    * @param file
    *           the file object to store for later retrieval
    */
   public void putFileForInput(StandardLocation location, String packageName,
         String relativeName, JavaFileObject file) {
      fileObjects.put(uri(location, packageName, relativeName), file);
   }
   
   /**
    * Convert a location and class name to a URI
    */
   private URI uri(Location location, String packageName, String relativeName) {
      return CharSequenceCompiler.toURI(location.getName() + '/' + packageName + '/'
            + relativeName);
   }

   @Override
   public ClassLoader getClassLoader(Location location) {
      return classLoader;
   }

   @Override
   public String inferBinaryName(Location loc, JavaFileObject file) {
      String result;
      // For our JavaFileImpl instances, return the file's name, else simply run the default implementation
      if (file instanceof JavaFileObjectImpl)
         result = file.getName();
      else
         result = super.inferBinaryName(loc, file);
      return result;
   }

   @Override
   public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
      Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);
      ArrayList<JavaFileObject> files = new ArrayList<JavaFileObject>();
      
      if (location == StandardLocation.CLASS_PATH && kinds.contains(Kind.CLASS)) {
         for (JavaFileObject file : fileObjects.values()) {
            if (file.getKind() == Kind.CLASS && file.getName().startsWith(packageName))
               files.add(file);
         }
         files.addAll(classLoader.files());
         
      } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(Kind.SOURCE)) {
         for (JavaFileObject file : fileObjects.values()) {
            if (file.getKind() == Kind.SOURCE && file.getName().startsWith(packageName))
               files.add(file);
         }
      }
      
      for (JavaFileObject file : result) {
         files.add(file);
      }
      
      return files;
   }
}

/**
 * A JavaFileObject which contains either the source text or the compiler
 * generated class. This class is used in two cases.
 * <ol>
 * <li>This instance uses it to store the source which is passed to the
 * compiler. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, CharSequence)}
 * constructor.
 * <li>The Java compiler also creates instances (indirectly through the
 * FileManagerImplFileManager) when it wants to create a JavaFileObject for the
 * .class output. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, Kind)}
 * constructor.
 * </ol>
 * This class does not attempt to reuse instances (there does not seem to be a
 * need, as it would require adding a Map for the purpose, and this would also
 * prevent garbage collection of class byte code.)
 */
final class JavaFileObjectImpl extends SimpleJavaFileObject {
   // If kind == CLASS, this stores byte code from openOutputStream
   private ByteArrayOutputStream byteCode;

   // if kind == SOURCE, this contains the source text
   private final CharSequence source;
   
   private final String className;

   /**
    * Construct a new instance which stores source
    * 
    * @param baseName
    *           the base name
    * @param source
    *           the source code
    */
   JavaFileObjectImpl(final String className, final CharSequence source) {
      super(CharSequenceCompiler.toURI(getBaseName(className) + CharSequenceCompiler.JAVA_EXTENSION), Kind.SOURCE);
      this.source = source;
      this.className = className;
   }
   
   private static String getBaseName(String qualifiedClassName) {
	   int dotPos = qualifiedClassName.lastIndexOf('.');
	   return dotPos == -1 ? qualifiedClassName : qualifiedClassName.substring(dotPos + 1);
   }

   /**
    * Construct a new instance
    * 
    * @param name
    *           the file name
    * @param kind
    *           the kind of file
    */
//   JavaFileObjectImpl(final String name, final Kind kind) {
//      super(CharSequenceCompiler.toURI(name), kind);
//      source = null;
//      this.className = name;
//   }

   /**
    * Return the source code content
    * 
    * @see SimpleJavaFileObject#getCharContent(boolean)
    */
   @Override
   public CharSequence getCharContent(final boolean ignoreEncodingErrors)
         throws UnsupportedOperationException {
      if (source == null)
         throw new UnsupportedOperationException("getCharContent()");
      return source;
   }

   /**
    * @return the {@link #className}
    */
   public String getClassName() {
	   return className;
   }

   /**
    * Return an input stream for reading the byte code
    * 
    * @see SimpleJavaFileObject#openInputStream()
    */
   @Override
   public InputStream openInputStream() {
      return new ByteArrayInputStream(getByteCode());
   }

   /**
    * Return an output stream for writing the bytecode
    * 
    * @see SimpleJavaFileObject#openOutputStream()
    */
   @Override
   public OutputStream openOutputStream() {
      byteCode = new ByteArrayOutputStream();
      return byteCode;
   }

   /**
    * @return the byte code generated by the compiler
    */
   byte[] getByteCode() {
      return byteCode.toByteArray();
   }
}

/**
 * A custom ClassLoader which maps class names to JavaFileObjectImpl instances.
 */
final class ClassLoaderImpl extends ClassLoader {
   private final Map<String, JavaFileObject> classes = new HashMap<String, JavaFileObject>();

   ClassLoaderImpl(final ClassLoader parentClassLoader) {
      super(parentClassLoader);
   }

   /**
    * @return An collection of JavaFileObject instances for the classes in the
    *         class loader.
    */
   Collection<JavaFileObject> files() {
      return Collections.unmodifiableCollection(classes.values());
   }

	@Override
	protected Class<?> findClass(final String qualifiedClassName) throws ClassNotFoundException {
		JavaFileObject file = classes.get(qualifiedClassName);
		
		try {
			if (file != null) {
				byte[] bytes = ((JavaFileObjectImpl) file).getByteCode();
				return defineClass(qualifiedClassName, bytes, 0, bytes.length);
			} 
		} catch (NoClassDefFoundError nf) {
			try (URLClassLoader urlCl = new URLClassLoader(
					new URL[] { CustomEntityTemplateService.getClassesDir(null).toURI().toURL() },
					this.getParent()
			)){
				return urlCl.loadClass(qualifiedClassName);
			} catch (Exception e) {
				CharSequenceCompiler.LOGGER.error("Can't load class", e);
			}
		}
		
		// Workaround for "feature" in Java 6
		// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6434149
		try {
			Class<?> c = Class.forName(qualifiedClassName);
			return c;
		} catch (ClassNotFoundException nf) {

		}
		
		// Load classes defined in maven dependencies
		try {
			Class<?> c = MavenClassLoader.getInstance().loadExternalClass(qualifiedClassName);
			return c;
		} catch (ClassNotFoundException ignored) {

		}

		return super.findClass(qualifiedClassName);
	}

   /**
    * Add a class name/JavaFileObject mapping
    * 
    * @param qualifiedClassName
    *           the name
    * @param javaFile
    *           the file associated with the name
    */
   void add(final String qualifiedClassName, final JavaFileObject javaFile) {
      classes.put(qualifiedClassName, javaFile);
   }

   @Override
   protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
      return super.loadClass(name, resolve);
   }

   @Override
   public InputStream getResourceAsStream(final String name) {
      if (name.endsWith(".class")) {
         String qualifiedClassName = name.substring(0, name.length() - ".class".length())
        		 .replace('/', '.');
         JavaFileObjectImpl file = (JavaFileObjectImpl) classes.get(qualifiedClassName);
         if (file != null) {
            return new ByteArrayInputStream(file.getByteCode());
         }
      }
      return super.getResourceAsStream(name);
   }
}
