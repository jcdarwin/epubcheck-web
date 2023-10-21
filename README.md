# EPUBCheck Web Archive build tool

This project allows you to create a web archive and serve it using [Apache Tomcat](http://tomcat.apache.org/), such that
you can provide validation of EPUB files using [EPUBCheck](https://github.com/IDPF/epubcheck) via a web service.

This project is licensed under the same license as the EPUBCheck project, namely, the <a href="http://opensource.org/licenses/BSD-3-Clause">BSD 3 license</a>.

# The rationale

Originally, the `epubcheck.jar` fle was accompanied by a `.war` file, which was very useful as it could be hosted by a Java-based webserver (e.g. <a href="http://tomcat.apache.org/">Apache Tomcat</a>), and used to provide a web-based interface for EPUB validation using epubcheck. This is how the the IDPF offer their <a href="http://validator.idpf.org/">online EPUB validation service</a>.

However, recent builds of EPUBCheck have not been accompanied by this `.war` file meaning that, for your own purposes, you were looking at having to use the command line version, as described above. You could still of course use the IDPF’s <a href="http://validator.idpf.org/">online validation service</a>, but this is only for non-commercial use and for files less than 10MB.

So, after running into confusion where a client mistakenly thought their EPUB3 was invalid because it was being checked against an older version of the epubcheck .war file used by a web service, I thought it was high time I worked out how to create a .war from a recent release.

The steps are below, and the presumption is that you’re running a Mac build environment (and using the <a href="http://brew.sh/">Homebrew package manager</a>), but it should be relatively easy to adapt them to other environments.

# The build process

Note that we need to build using a JDK version which is no older than the Java version that we'll be using on the server, otherwise we'l get errors while serving. Therefore, it may be a good idea to build on the server itself. Also, we've had more luck using the Oracle JDK, instead of the OpenJDK.

* Clone the epubcheck repo

		git clone https://github.com/IDPF/epubcheck.git

* Checkout the tagged release we wish to build

		cd epubcheck/
		git tag -l
		git checkout tags/v5.1.0

* Install <a href="https://github.com/jenv/jenv">jenv</a>:

		brew install jenv
		echo 'export PATH="$HOME/.jenv/bin:$PATH"' >> ~/.bash_profile
		echo 'eval "$(jenv init -)"' >> ~/.bash_profile

		# check installed java versions
		jenv versions

		# will probably report: No JAVA_HOME set
		jenv doctor

		# set JAVA_HOME
		jenv enable-plugin export
		exec $SHELL -l

* More notes about using jenv can be found <a href="https://www.baeldung.com/jenv-multiple-jdk">here</a>

* Install <a href="http://maven.apache.org/">maven</a>, and use it to build the release (this may take some time, because of maven needing to boostrap itself)

		brew install maven
		mvn clean install

* This will create a .jar file in `epubcheck/target/`, e.g. `epubcheck.jar`

* At the root level, checkout the epubcheck `.war` code (i.e. this repo)

		git checkout epubcheck-web

* Make some changes to the build file, `epubcheck-web\build-war.xml`,
specifically include the correct version number, as found in the name of .jar file (e.g.)

		<property name="version" value="5.1.0" />

* If epubcheck has updated their libraries, we'll need to copy them over to `lib`, and ensure that they're referenced in the classpath in the `build-war.xml`.  You'll find the required libraries in the `lib` folder in the compiled zip file, e.g. `target/epubcheck-5.1.0.zip`

* We should also copy over `epubcheck.jar` that we've built or downloaded.  Hence, e.g.:

		<path id="epubcheckServlet.classpath">
		...
			<fileset dir="${epubcheck.web.includelibs}">
				<include name="common-image-*.jar" />
				<include name="common-io-*.jar" />
				<include name="common-lang-*.jar" />
				<include name="commons-compress-*.jar" />
				<include name="guava-*.jar" />
				<include name="imageio-jpeg-*.jar" />
				<include name="imageio-metadata-*.jar" />
				<include name="jackson-core-asl-*.jar" />
				<include name="jackson-mapper-asl-*.jar" />
				<include name="jing-*.jar" />
				<include name="sac-*.jar" />
				<include name="Saxon-*.jar" />
				<include name="epubcheck.jar" />
			</fileset>
		...
		</path>

As we are not building `epubcheck.jar`, we should comment out references to it:

		<!-- comment out the reference to the epubcheck.jar in the distribution directory -->
		<!--
		<fileset dir="${epubcheck.distribution.dir}">
			<include name="epubcheck.jar" />
		</fileset>
		-->
		...

		<!-- comment out the epubcheck.jar target as it's already built -->
		<!--
			<target name="epubcheck" description="creates epubcheck.jar">
				<ant dir="${epubcheck.base.dir}"/>
			</target>
		-->
		...

		<!-- comment out reference to epubcheck.jar as we're not building it -->
		<!--
			<zipfileset dir="${epubcheck.distribution.dir}" prefix="WEB-INF/lib">
				<include name="epubcheck-*.jar" />
			</zipfileset>
		-->


* Also, we need to copy other source libraries from `epubcheck/src/main/java` into the `src` folder.

* We also need to copy `extra/com/adobe/epubcheck/web` to `src/com/adobe/epubcheck/web`, as this provides the HttpServlet functionality.

* Add the code below beneath the `check` method in `src/com/adobe/epubcheck/api/EpubCheck.java`:

		/**
		* Validate the file. Return true if no errors or warnings found.
		*/
		public boolean checkForWeb()
		{
			int result = doValidate();
			if (result == 0)
				return true;
			return false;
		}

* Change the code in the `warning` method in `src/com/adobe/epubcheck/util/WriterReportImpl.java` to be:

		void warning(String resource, int line, int column, String message)
		{
			message = fixMessage(message);
			if (message == null || message == "") return;
			out.println("WARNING: " + (resource == null ? "[top level]" : resource)
				+ (line <= 0 ? "" : "(" + line + (column <= 0 ? "" : "," + column) + ")") + ": " + message);
		}

* Place your customised HTML (as `index.html`) and CSS in `epubcheck-web\http_root` &#8212; at a minimum you'll need:

		<html>
		    <head>
		        <meta charset="utf-8">
		        <title>EPUBCheck for Web</title>

		    </head>
		    <body>
		            <form action="Check" method="POST" enctype="multipart/form-data">
		                <table>
		                    <tr>
		                        <td><span class="label">EPUB file:</span></td>
		                        <td><input type="file" style="width:20em" name="file" /></td>
		                    </tr>
		                    <tr>
		                        <td></td>
		                        <td><input type="submit" value="Check validity"/></td>
		                    </tr>
		                </table>
		            </form>
		    </body>
		</html>

* Install ant and use it to build the `epubcheck.war`:

		brew install ant

		ant -f build-war.xml

* You may receive errors similar to the following:

		[javac]   (use -source 8 or higher to enable lambda expressions)
		[javac] /Users/jasondarwin/workspace/epubcheck-web/src/com/adobe/epubcheck/ops/OPSHandler30.java:607: error: lambda expressions are not supported in -source 1.7
		[javac]         .anyMatch(mimetype -> OPFChecker30.isCoreMediaType(mimetype));
		[javac]                            ^
		[javac]   (use -source 8 or higher to enable lambda expressions)

* In this case, update the `source` attribute in the `build-war.xml`:

		<target name="compile" description="Compiles all src classes" depends="removeClasses">
			<javac 	srcdir="${epubcheck.web.base.dir}/src"
					destdir="${epubcheck.web.build.dir}"
					source="8"
					classpathref="epubcheckServlet.classpath"
					debug="true"
					includeantruntime="false"
					>
				<exclude name="**/SampleServer.java"/>
			</javac>
		</target>

* Install the `.war` (by dropping it in tomcat's `webapps` directory) and restart tomcat

		cd /usr/local/tomcat/webapps
		cp /tmp/epubcheck/epubcheck-web/dist/epubcheck-5.1.0.war epubcheck.war
		/etc/init.d/tomcat start

Once Tomcat’s restarted, you should be able to see the epubcheck service at [http://localhost:8080/epubcheck](http://localhost:8080/epubcheck).

On my system I use Apache to proxy requests from port 80 to 8080, so I can make it available as I do at [http://epubcheck.mebooks.co.nz/](http://epubcheck.mebooks.co.nz/)


