# EpubCheck Web Archive build tool

This project allows you to create a web archive and serve it using [Apache Tomcat](http://tomcat.apache.org/), such that 
you can provide validation of EPUB files using [EpubCheck](https://github.com/IDPF/epubcheck) via a web service.

This project is licensed under the same license as the EpubCheck project, namely, the <a href="http://opensource.org/licenses/BSD-3-Clause">BSD 3 license</a>.

# The rationale

Originally, the EpubCheck .jar fle was accompanied by a .war file, which was very useful as it could be hosted by a Java-based webserver (e.g. <a href="http://tomcat.apache.org/">Apache Tomcat</a>), and used to provide a web-based interface for EPUB validation using epubcheck. This is how the the IDPF offer their <a href="http://validator.idpf.org/">online EPUB validation service</a>.

However, recent builds of EpubCheck have not been accompanied by this .war file meaning that, for your own purposes, you were looking at having to use the command line version, as described above. You could still of course use the IDPF’s <a href="http://validator.idpf.org/">online validation service</a>, but this is only for non-commercial use and for files less than 10MB.

So, after running into confusion where a client mistakenly thought their EPUB3 was invalid because it was being checked against an older version of the epubcheck .war file used by a web service, I thought it was high time I worked out how to create a .war from a recent release.

s epubcheck is written in Java and not being a Java programmer myself (and there’s good reason for this, summed up nicely in this post about <a href="http://steve-yegge.blogspot.co.nz/2006/03/execution-in-kingdom-of-nouns.html">Execution in the Kingdom of Nouns</a>), I was a little tremulous about tackling this, but it actually turned out to be relatively easy. The steps are below, and the presumption is that you’re running a Mac build environment (and using the <a href="http://brew.sh/">Homebrew package manager</a>), but it should be relatively easy to adapt them to other environments.

# The build process

* Clone the EpubCheck repo

		git clone https://github.com/IDPF/epubcheck.git

* Checkout the tagged release we wish to build

		cd epubcheck/
		git tag -l
		git checkout tags/v3.0.1

* Install <a href="http://maven.apache.org/">maven</a>, and use it to build the release (this may take some time, because of maven needing to boostrap itself)

		brew install maven
		mvn install

* This will create a .jar file in `epubcheck/target/`, e.g. `epubcheck-3.0.1.jar`

* At the root level, checkout the EpubCheck .war code (i.e. this repo)

		git checkout epubcheck-web

* Make some changes to the build file, `epubcheck-web\build-war.xml`, 
specifically include the correct version number, as found in the name of .jar file (e.g.)

		<property name="version" value="3.0.1" />

* Place your customised HTML (as `index.html`) and CSS in `epubcheck-web\http_root` &#8212; at a minimum you'll need:

		<html>
		    <head>
		        <meta charset="utf-8">
		        <title>EpubCheck for Web</title>

		    </head>
		    <body>
		            <form action="Check" method="POST" enctype="multipart/form-data">
		                <table>
		                    <tr>
		                        <td><span class="label">Epub file:</span></td>
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

* Install the `.war` (by dropping it in tomcat's `webapps` directory) and restart tomcat

		cd /usr/local/tomcat/webapps
		cp /tmp/epubcheck/epubcheck-web/dist/epubcheck-3.0.1.war epubcheck.war
		/etc/init.d/tomcat start

Once Tomcat’s restarted, you should be able to see the epubcheck service at [http://localhost:8080/epubcheck](http://localhost:8080/epubcheck).

On my system I use Apache to proxy requests from port 80 to 8080, so I can make it available as I do at [http://mebooks.co.nz/epubcheck/](http://mebooks.co.nz/epubcheck/)


